# Matador Backend — Service Specification

> Spring Boot 3 modular monolith. Read `PROJECT.md` first.

---

## 1. Tech stack (pinned)

| Layer | Choice | Version |
|---|---|---|
| Language | Java | 21 (LTS) |
| Framework | Spring Boot | 3.3.x |
| Module isolation | Spring Modulith | 1.2.x |
| Build | Gradle (Kotlin DSL) | 8.x |
| Database | PostgreSQL | 16.x |
| DB extensions | PostGIS, pg_trgm, uuid-ossp (or pgcrypto for UUIDv7) | latest stable |
| Migrations | Flyway | 10.x |
| ORM | Spring Data JPA + Hibernate | bundled with Spring Boot 3.3 |
| Validation | Jakarta Bean Validation (Hibernate Validator) | bundled |
| Security | Spring Security | bundled |
| API docs | springdoc-openapi | 2.x |
| Mapping | MapStruct | 1.5.x |
| HTTP client | Spring's `RestClient` (sync) and `WebClient` (async) | bundled |
| Testing | JUnit 5, AssertJ, Testcontainers, WireMock | latest stable |
| Logging | Logback with `logstash-logback-encoder` for JSON output | latest stable |
| Error tracking | Sentry Spring Boot starter | latest stable |

External SDKs:

- `com.stripe:stripe-java` — Stripe payments + Identity
- `com.smartcar.sdk:java-sdk` — Smartcar telematics
- `com.twilio.sdk:twilio` — SMS
- `com.postmarkapp:postmark` — email

---

## 2. Project structure

This is the entire `matador-backend` repository. The repo root is the project root.

```
matador-backend/                 # repo root
├── build.gradle.kts
├── settings.gradle.kts
├── docker-compose.yml           # local Postgres + Mailpit
├── Dockerfile
├── .github/workflows/
│   ├── ci.yml
│   └── deploy.yml
├── docs/
│   ├── PROJECT.md
│   └── BACKEND.md               # this file
├── src/
│   ├── main/
│   │   ├── java/com/matador/
│   │   │   ├── MatadorApplication.java
│   │   │   ├── shared/                    # Cross-cutting, non-domain
│   │   │   │   ├── config/                # Spring config beans
│   │   │   │   ├── security/              # Auth filters, JWT, etc.
│   │   │   │   ├── error/                 # Global exception handler, ProblemDetail
│   │   │   │   ├── audit/                 # Auditing, request logging
│   │   │   │   ├── money/                 # Money type, currency utils
│   │   │   │   ├── id/                    # UUIDv7 generator
│   │   │   │   └── time/                  # Clock abstraction for testability
│   │   │   ├── customer/                  # Module: Customer
│   │   │   │   ├── package-info.java      # @ApplicationModule
│   │   │   │   ├── Customer.java          # Aggregate root (exposed)
│   │   │   │   ├── CustomerService.java   # (exposed)
│   │   │   │   ├── api/                   # REST controllers
│   │   │   │   ├── events/                # Domain events (exposed)
│   │   │   │   └── internal/              # Hidden: repos, entities, internal logic
│   │   │   ├── vehicle/
│   │   │   ├── zone/
│   │   │   ├── booking/
│   │   │   ├── trip/
│   │   │   ├── pricing/
│   │   │   ├── ledger/
│   │   │   ├── payment/
│   │   │   ├── telematics/
│   │   │   ├── incident/
│   │   │   ├── inspection/
│   │   │   ├── notification/
│   │   │   ├── identity/                  # ID verification (Stripe Identity)
│   │   │   └── webhooks/                  # Inbound webhook controllers
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       ├── db/migration/              # Flyway migrations
│   │       └── messages/                  # i18n strings
│   └── test/
│       ├── java/com/matador/              # mirror of main
│       └── resources/
│           └── application-test.yml
```

**Module isolation rule:** Code in `module-x/internal/` may not be referenced from any other module. Spring Modulith enforces this via an ArchUnit test (`@ApplicationModuleTest`); CI must run this verification.

---

## 3. Module map

| Module | Owns | Depends on (events from) |
|---|---|---|
| `customer` | Customer profiles, verification status references | identity (consumes verification completed) |
| `identity` | Stripe Identity sessions, verification results | — |
| `vehicle` | Vehicle inventory, status, location, telematics device link | telematics (consumes status changes), booking (consumes trip lifecycle) |
| `zone` | Geofence polygons, zone membership queries | — |
| `booking` | Reservations, availability checking | vehicle (queries), pricing (quotes), payment (holds), zone (validation) |
| `trip` | Active and completed trips, state machine | booking (consumes activation), telematics (consumes location/odometer), inspection (consumes photo sets) |
| `pricing` | Rate cards, quote calculation | zone (zone-based surcharges) |
| `ledger` | Immutable financial entries | trip (consumes events), payment (consumes events), incident (consumes events) |
| `payment` | Stripe customer + payment method records, intents | — (publishes to ledger) |
| `telematics` | Smartcar tokens, vehicle-level commands, status polling | vehicle (queries) |
| `incident` | Damage reports, violations, accident records | trip (consumes events) |
| `inspection` | Photo sets at pickup/dropoff | trip (publishes) |
| `notification` | Email and SMS dispatch | all modules (consumes events to send notifications) |

Inter-module communication is event-driven where possible. Direct method calls only for query operations (e.g., "is this vehicle available?"). Mutations flow through events.

---

## 4. Cross-cutting infrastructure

### 4.1 Identity for primary keys

Use UUIDv7 for all primary keys. Provide a `IdGenerator` bean in `shared.id`:

```java
public interface IdGenerator {
    UUID newId();
}
```

Default implementation uses `com.fasterxml.uuid.Generators.timeBasedEpochGenerator()`. Inject this everywhere; do not call generators directly from domain code.

### 4.2 Money type

Provide a value object `shared.money.Money`:

```java
public record Money(long amountCents, Currency currency) {
    public static Money usd(long cents) { ... }
    public Money plus(Money other) { ... }
    public Money minus(Money other) { ... }
    public Money times(int n) { ... }
}
```

Persist as two columns: `amount_cents BIGINT NOT NULL` and `currency CHAR(3) NOT NULL DEFAULT 'USD'`. Provide an attribute converter or composite type. Never use `BigDecimal` or `double`.

### 4.3 Clock abstraction

All time access goes through `java.time.Clock` injected as a Spring bean. Production bean is `Clock.systemUTC()`. Test bean is a mutable `Clock.fixed(...)`. This is non-negotiable for the time-sensitive trip logic.

### 4.4 Error handling

Implement a `@RestControllerAdvice` global handler that converts all exceptions to RFC 7807 `ProblemDetail` responses. Custom exceptions:

- `ResourceNotFoundException` → 404
- `ValidationException` → 422
- `ConflictException` → 409 (e.g., booking overlap)
- `UnauthorizedException` → 401
- `ForbiddenException` → 403
- `ExternalServiceException` → 502

Include a stable `type` URI and a `code` field for client-side handling: `{"type": "https://matador.app/errors/booking-conflict", "code": "BOOKING_CONFLICT", "title": "...", "detail": "...", "status": 409}`.

### 4.5 Auditing

Add `created_at`, `updated_at`, `created_by`, `updated_by` columns to all aggregate root tables. Use Spring Data JPA's `@CreatedDate`, `@LastModifiedDate`, `@CreatedBy`, `@LastModifiedBy` with an `AuditorAware<UUID>` that pulls the current principal's ID (or `'system'` UUID for background jobs).

### 4.6 Request logging

Filter that logs every API request/response with: `trace_id`, `method`, `path`, `status`, `duration_ms`, `user_id`, `ip`. Strip sensitive fields from request bodies before logging (passwords, payment tokens, SSN). Use MDC to propagate `trace_id` and `user_id` to all log statements.

### 4.7 Background jobs

Two mechanisms:

1. **Scheduled jobs** via Spring's `@Scheduled`. Use for periodic tasks (e.g., release expired holds every 15 minutes, poll vehicle status every 5 minutes). Run only one instance via `ShedLock` to prevent duplicate execution.
2. **Deferred jobs** via a `pending_job` table in Postgres. Records hold `job_type`, `payload JSONB`, `run_after TIMESTAMPTZ`, `attempts INT`, `status`. A poller picks up due jobs every 30 seconds. Use this for trip-related delayed actions (e.g., "charge cleaning fee 2 hours after trip end if no incident raised").

Do not use Redis or external queues in Phase 1.

---

## 5. Authentication & authorization

### 5.1 Admin authentication

Session-cookie based. Endpoints under `/api/admin/**`. Login at `POST /api/admin/auth/login` returns `Set-Cookie: SESSION=...`. Logout at `POST /api/admin/auth/logout`. Passwords hashed with BCrypt (cost factor 12). CSRF protection enabled (Spring Security default).

### 5.2 Customer authentication

JWT bearer tokens. Endpoints under `/api/customer/**`. Login at `POST /api/customer/auth/login` returns `{access_token, refresh_token, expires_in}`. Access token TTL 15 minutes, refresh token TTL 30 days. Refresh at `POST /api/customer/auth/refresh`. Tokens signed with RS256 (RSA keypair); store private key in env var, expose `/jwks.json`.

### 5.3 Webhooks

Endpoints under `/api/webhooks/**`. Each provider has its own signature verification:

- Stripe: verify `Stripe-Signature` header via Stripe SDK using webhook secret.
- Smartcar: verify HMAC-SHA256 signature header against webhook secret.

Webhooks are idempotent — store every event by provider event ID, return 200 if already processed.

### 5.4 Roles

| Role | Description |
|---|---|
| `ADMIN` | Full access including user management, refunds without limit |
| `DISPATCHER` | Vehicle and booking operations, can issue refunds up to $500 |
| `SUPPORT` | Read most data, can edit customer profiles, escalate incidents |
| `READONLY` | Read-only access to all admin data |
| `CUSTOMER` | End user, can only access own resources |

Enforce via Spring Security `@PreAuthorize("hasRole('ADMIN')")` etc. on controllers.

---

## 6. Module specifications

The following subsections specify each module. For every module, the agent should produce:

- Aggregates as JPA entities (mutable, package-private setters)
- Read models as Java records / DTOs
- A `*Service` class in the module root package as the public API
- A `*Repository` Spring Data interface in `internal/`
- REST controllers in `api/`
- Domain events in `events/` as Java records
- Unit tests for domain logic, integration tests for controllers

### 6.1 `customer` module

**Purpose.** Manages customer profiles and their lifecycle.

**Database tables:**

```sql
CREATE TABLE customer (
    id UUID PRIMARY KEY,
    email TEXT NOT NULL UNIQUE,
    phone TEXT NOT NULL UNIQUE,
    first_name TEXT NOT NULL,
    last_name TEXT NOT NULL,
    date_of_birth DATE NOT NULL,
    license_number TEXT,                       -- nullable until verified
    license_state CHAR(2),
    license_expires_on DATE,
    verification_status TEXT NOT NULL,         -- enum
    verification_completed_at TIMESTAMPTZ,
    stripe_customer_id TEXT UNIQUE,            -- set after first payment method add
    status TEXT NOT NULL,                      -- enum
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_by UUID
);
CREATE INDEX idx_customer_email ON customer(LOWER(email));
CREATE INDEX idx_customer_phone ON customer(phone);
CREATE INDEX idx_customer_verification ON customer(verification_status);

CREATE TABLE customer_address (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customer(id),
    label TEXT,                                -- 'Home', 'Work', etc.
    line1 TEXT NOT NULL,
    line2 TEXT,
    city TEXT NOT NULL,
    state CHAR(2) NOT NULL,
    postal_code TEXT NOT NULL,
    country CHAR(2) NOT NULL DEFAULT 'US',
    location GEOGRAPHY(POINT, 4326) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL
);
```

**Enums:**

- `verification_status`: `UNVERIFIED`, `IN_PROGRESS`, `VERIFIED`, `REJECTED`, `EXPIRED`
- `status`: `ACTIVE`, `SUSPENDED`, `DEACTIVATED`

**State transitions for `verification_status`:**

```
UNVERIFIED → IN_PROGRESS (when verification session started)
IN_PROGRESS → VERIFIED (when identity webhook confirms success)
IN_PROGRESS → REJECTED (when identity webhook reports failure)
VERIFIED → EXPIRED (when license_expires_on < today, via daily job)
REJECTED → IN_PROGRESS (allow retry after 24h cooldown)
```

**REST API:**

| Method | Path | Auth | Purpose |
|---|---|---|---|
| `POST` | `/api/customer/auth/register` | None | Create customer (email, phone, name, DOB, password). Returns access token. |
| `POST` | `/api/customer/auth/login` | None | Login with email + password. |
| `POST` | `/api/customer/auth/refresh` | None | Refresh access token. |
| `GET` | `/api/customer/me` | Customer | Current customer profile. |
| `PATCH` | `/api/customer/me` | Customer | Update name, phone, addresses. Email and DOB immutable post-registration. |
| `GET` | `/api/customer/me/addresses` | Customer | List saved addresses. |
| `POST` | `/api/customer/me/addresses` | Customer | Add address. |
| `DELETE` | `/api/customer/me/addresses/{id}` | Customer | Remove address. |
| `GET` | `/api/admin/customers` | Staff | Paginated list with filters: email, phone, verification status, status. |
| `GET` | `/api/admin/customers/{id}` | Staff | Full profile with trip count, lifetime value. |
| `PATCH` | `/api/admin/customers/{id}` | Admin/Support | Update status (suspend, deactivate). |

**Domain events published:**

- `CustomerRegistered(customerId, email, registeredAt)`
- `CustomerVerified(customerId, verifiedAt)`
- `CustomerSuspended(customerId, reason, suspendedAt)`

**Business rules:**

- Minimum age: 21 years at time of registration. Enforce on `date_of_birth`.
- Email must be unique (case-insensitive). Phone must be unique.
- Password: minimum 12 characters, must contain at least one letter and one digit. Hash with BCrypt cost 12.
- A customer can only book if `verification_status = VERIFIED` and `status = ACTIVE`.

### 6.2 `identity` module

**Purpose.** Manages ID and driver's license verification via Stripe Identity. Decouples customer module from the verification provider.

**Database tables:**

```sql
CREATE TABLE verification_session (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customer(id),
    provider TEXT NOT NULL DEFAULT 'STRIPE_IDENTITY',
    provider_session_id TEXT NOT NULL UNIQUE,
    client_secret TEXT NOT NULL,               -- returned to client for completing flow
    status TEXT NOT NULL,                      -- CREATED, PROCESSING, VERIFIED, REQUIRES_INPUT, CANCELED
    result_payload JSONB,                      -- raw provider response
    created_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ
);
CREATE INDEX idx_verification_customer ON verification_session(customer_id);
```

**REST API:**

| Method | Path | Auth | Purpose |
|---|---|---|---|
| `POST` | `/api/customer/me/verification` | Customer | Start a verification session. Returns client_secret. |
| `GET` | `/api/customer/me/verification` | Customer | Get current session status. |
| `POST` | `/api/webhooks/stripe-identity` | Webhook | Handle Stripe Identity events. |

**Webhook handling.** On `identity.verification_session.verified`, update session and emit event. On `identity.verification_session.requires_input` or `.canceled`, update session.

**Domain events published:**

- `VerificationCompleted(customerId, sessionId, success: boolean, completedAt)`

The `customer` module subscribes and updates `verification_status` accordingly. On success, extract `license_number`, `license_state`, `license_expires_on` from the verified document and persist.

### 6.3 `vehicle` module

**Purpose.** Manages the fleet — physical cars, their classification, their current status and location.

**Database tables:**

```sql
CREATE TABLE vehicle_class (
    id UUID PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,                 -- e.g., 'Compact SUV Hybrid'
    description TEXT,
    seats INT NOT NULL,
    luggage_capacity INT NOT NULL,
    drivetrain TEXT NOT NULL,                  -- ICE, HYBRID, EV
    base_daily_rate_cents BIGINT NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE vehicle (
    id UUID PRIMARY KEY,
    vin TEXT NOT NULL UNIQUE,
    license_plate TEXT NOT NULL,
    license_state CHAR(2) NOT NULL,
    make TEXT NOT NULL,
    model TEXT NOT NULL,
    year INT NOT NULL,
    color TEXT NOT NULL,
    class_id UUID NOT NULL REFERENCES vehicle_class(id),
    status TEXT NOT NULL,                      -- enum, see below
    current_location GEOGRAPHY(POINT, 4326),   -- last known
    current_address TEXT,                      -- reverse-geocoded, last known
    odometer_miles INT NOT NULL DEFAULT 0,
    fuel_charge_percent INT,                   -- 0-100, null if unknown
    range_miles INT,                           -- estimated
    home_zone_id UUID NOT NULL REFERENCES zone(id),
    telematics_provider TEXT,                  -- SMARTCAR, TESLA, NONE
    telematics_vehicle_id TEXT,                -- provider-specific identifier
    notes TEXT,
    acquired_on DATE NOT NULL,
    retired_on DATE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_vehicle_status ON vehicle(status);
CREATE INDEX idx_vehicle_class ON vehicle(class_id);
CREATE INDEX idx_vehicle_location ON vehicle USING GIST(current_location);

CREATE TABLE vehicle_photo (
    id UUID PRIMARY KEY,
    vehicle_id UUID NOT NULL REFERENCES vehicle(id),
    url TEXT NOT NULL,
    label TEXT,                                -- 'Front', 'Hero', etc.
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL
);
```

**Vehicle status enum and state machine:**

```
States: AVAILABLE, RESERVED, IN_PREPARATION, EN_ROUTE_TO_CUSTOMER, WITH_CUSTOMER,
        EN_ROUTE_TO_WAREHOUSE, AWAITING_INSPECTION, IN_CLEANING, IN_MAINTENANCE,
        OUT_OF_SERVICE, RETIRED

Valid transitions:
  AVAILABLE → RESERVED (booking confirmed)
  RESERVED → IN_PREPARATION (pre-trip cleaning/charging starts)
  RESERVED → AVAILABLE (booking cancelled before prep)
  IN_PREPARATION → EN_ROUTE_TO_CUSTOMER (driver picks up vehicle)
  EN_ROUTE_TO_CUSTOMER → WITH_CUSTOMER (handoff completed)
  WITH_CUSTOMER → EN_ROUTE_TO_WAREHOUSE (trip ended, driver retrieving)
  EN_ROUTE_TO_WAREHOUSE → AWAITING_INSPECTION (returned to warehouse)
  AWAITING_INSPECTION → IN_CLEANING (passed inspection, queued for clean)
  AWAITING_INSPECTION → IN_MAINTENANCE (issue found)
  IN_CLEANING → AVAILABLE (cleaning complete)
  IN_MAINTENANCE → AVAILABLE (maintenance complete)
  AVAILABLE → OUT_OF_SERVICE (manual hold)
  OUT_OF_SERVICE → AVAILABLE (manual release)
  any → RETIRED (terminal, when vehicle removed from fleet)
```

Enforce in `VehicleService.transitionStatus(...)`. Reject invalid transitions with `ConflictException`.

**REST API:**

| Method | Path | Auth | Purpose |
|---|---|---|---|
| `GET` | `/api/admin/vehicles` | Staff | Paginated list with filters: status, class, zone. |
| `POST` | `/api/admin/vehicles` | Admin | Create vehicle. |
| `GET` | `/api/admin/vehicles/{id}` | Staff | Full detail including telematics state. |
| `PATCH` | `/api/admin/vehicles/{id}` | Admin | Update mutable fields. |
| `POST` | `/api/admin/vehicles/{id}/status` | Dispatcher | Transition status (validated). |
| `POST` | `/api/admin/vehicles/{id}/lock` | Dispatcher | Send lock command via telematics. |
| `POST` | `/api/admin/vehicles/{id}/unlock` | Dispatcher | Send unlock command via telematics. |
| `GET` | `/api/admin/vehicle-classes` | Staff | List classes. |
| `POST` | `/api/admin/vehicle-classes` | Admin | Create class. |
| `PATCH` | `/api/admin/vehicle-classes/{id}` | Admin | Update class. |
| `GET` | `/api/customer/vehicle-classes` | Customer | Public-facing class catalog. |
| `GET` | `/api/customer/vehicles/available` | Customer | Available vehicles for date range and pickup location. Query params: `pickupAt`, `dropoffAt`, `pickupLat`, `pickupLng`. Returns vehicle classes with representative photo, count available, starting price. |

**Domain events published:**

- `VehicleStatusChanged(vehicleId, fromStatus, toStatus, changedAt, changedBy)`
- `VehicleLocationUpdated(vehicleId, location, recordedAt)` (high-volume, fire-and-forget)
- `VehicleAcquired(vehicleId, vin, acquiredOn)`
- `VehicleRetired(vehicleId, retiredOn)`

**Business rules:**

- A vehicle is bookable only in statuses: `AVAILABLE`.
- Status transitions originate from: booking lifecycle (most), dispatcher manual action, telematics events.

### 6.4 `zone` module

**Purpose.** Manages Matador's operational zones — geofenced polygons defining where vehicles operate.

**Database tables:**

```sql
CREATE TABLE zone (
    id UUID PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,                 -- e.g., 'Triangle'
    slug TEXT NOT NULL UNIQUE,                 -- 'triangle'
    boundary GEOGRAPHY(POLYGON, 4326) NOT NULL,
    center GEOGRAPHY(POINT, 4326) NOT NULL,
    out_of_zone_dropoff_fee_cents BIGINT NOT NULL DEFAULT 0,
    out_of_zone_dropoff_allowed BOOLEAN NOT NULL DEFAULT TRUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_zone_boundary ON zone USING GIST(boundary);
```

**REST API:**

| Method | Path | Auth | Purpose |
|---|---|---|---|
| `GET` | `/api/admin/zones` | Staff | List zones with boundaries as GeoJSON. |
| `POST` | `/api/admin/zones` | Admin | Create zone with GeoJSON polygon. |
| `PATCH` | `/api/admin/zones/{id}` | Admin | Update zone. |
| `GET` | `/api/customer/zones` | None | Public list of zones (name, slug, boundary, center) for map display. |
| `GET` | `/api/zones/contains?lat=...&lng=...` | None | Returns which zone (if any) contains the point. |

**Service contract:**

```java
public interface ZoneService {
    Optional<Zone> findContaining(double lat, double lng);
    boolean isInZone(UUID zoneId, double lat, double lng);
    List<Zone> activeZones();
}
```

Implementation uses PostGIS `ST_Contains` on the `boundary` column.

### 6.5 `booking` module

**Purpose.** Manages reservations from creation through activation. After activation, the trip module takes over.

**Database tables:**

```sql
CREATE TABLE booking (
    id UUID PRIMARY KEY,
    booking_number TEXT NOT NULL UNIQUE,       -- human-readable, e.g., 'MTD-2024-00001234'
    customer_id UUID NOT NULL REFERENCES customer(id),
    vehicle_class_id UUID NOT NULL REFERENCES vehicle_class(id),
    assigned_vehicle_id UUID REFERENCES vehicle(id),  -- null until assigned
    pickup_at TIMESTAMPTZ NOT NULL,
    dropoff_at TIMESTAMPTZ NOT NULL,
    pickup_location GEOGRAPHY(POINT, 4326) NOT NULL,
    pickup_address TEXT NOT NULL,
    pickup_zone_id UUID NOT NULL REFERENCES zone(id),
    dropoff_location GEOGRAPHY(POINT, 4326) NOT NULL,
    dropoff_address TEXT NOT NULL,
    dropoff_zone_id UUID REFERENCES zone(id),  -- null if outside any zone
    dropoff_in_zone BOOLEAN NOT NULL,
    quote_id UUID NOT NULL REFERENCES pricing_quote(id),
    quoted_total_cents BIGINT NOT NULL,
    deposit_hold_intent_id TEXT,               -- Stripe PaymentIntent ID for the hold
    deposit_amount_cents BIGINT NOT NULL,
    insurance_tier TEXT NOT NULL DEFAULT 'STANDARD',
    status TEXT NOT NULL,                      -- enum
    cancellation_reason TEXT,
    cancelled_at TIMESTAMPTZ,
    activated_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by UUID NOT NULL,
    updated_by UUID
);
CREATE INDEX idx_booking_customer ON booking(customer_id);
CREATE INDEX idx_booking_vehicle ON booking(assigned_vehicle_id);
CREATE INDEX idx_booking_status_pickup ON booking(status, pickup_at);
CREATE INDEX idx_booking_class_window ON booking(vehicle_class_id, pickup_at, dropoff_at)
    WHERE status IN ('PENDING_PAYMENT', 'CONFIRMED', 'ACTIVATED');
```

**Booking status enum and state machine:**

```
States: PENDING_PAYMENT, CONFIRMED, ACTIVATED, COMPLETED, CANCELLED, NO_SHOW

Transitions:
  PENDING_PAYMENT → CONFIRMED (payment hold succeeds)
  PENDING_PAYMENT → CANCELLED (payment fails or customer abandons; hold released)
  CONFIRMED → ACTIVATED (driver hands off vehicle, trip starts)
  CONFIRMED → CANCELLED (customer or admin cancels before activation)
  ACTIVATED → COMPLETED (trip ends, terminal state for booking)
  CONFIRMED → NO_SHOW (pickup window passed without activation; system or admin)
```

**REST API:**

| Method | Path | Auth | Purpose |
|---|---|---|---|
| `POST` | `/api/customer/bookings/quote` | Customer | Get a price quote without creating booking. Returns `quoteId` and breakdown. |
| `POST` | `/api/customer/bookings` | Customer | Create booking from a quote. Returns booking with `clientSecret` for confirming the payment hold. |
| `GET` | `/api/customer/bookings` | Customer | List customer's bookings (paginated, filtered by status). |
| `GET` | `/api/customer/bookings/{id}` | Customer | Booking detail. |
| `POST` | `/api/customer/bookings/{id}/cancel` | Customer | Cancel booking. Refund per policy (see business rules). |
| `GET` | `/api/admin/bookings` | Staff | List bookings with filters: status, customer, vehicle, date range. |
| `GET` | `/api/admin/bookings/{id}` | Staff | Booking detail. |
| `POST` | `/api/admin/bookings/{id}/assign-vehicle` | Dispatcher | Assign a specific vehicle. |
| `POST` | `/api/admin/bookings/{id}/cancel` | Dispatcher | Admin-initiated cancel. |
| `POST` | `/api/admin/bookings/{id}/activate` | Dispatcher | Activate booking → creates Trip, transitions vehicle to WITH_CUSTOMER. |

**Domain events published:**

- `BookingCreated(bookingId, customerId, vehicleClassId, pickupAt, dropoffAt, createdAt)`
- `BookingConfirmed(bookingId, confirmedAt)`
- `BookingCancelled(bookingId, reason, cancelledAt, refundAmountCents)`
- `BookingActivated(bookingId, tripId, vehicleId, activatedAt)`
- `BookingCompleted(bookingId, tripId, completedAt)`

**Business rules:**

- Minimum booking duration: 4 hours. Maximum: 30 days. (Hourly pricing deferred — Phase 1 enforces daily rounding: `dropoff_at - pickup_at` rounded up to whole days for pricing.)
- Pickup `at` must be ≥ 2 hours in the future at booking creation.
- Pickup must be inside an active zone. Dropoff may be outside; if so, `dropoff_in_zone = false` and out-of-zone fee applies.
- A vehicle class is bookable for a window only if at least one `AVAILABLE` vehicle in that class has no overlapping `CONFIRMED` or `ACTIVATED` booking. Concurrency control: use `SELECT ... FOR UPDATE` on candidate vehicle rows when creating a booking, OR use a unique constraint + retry pattern. Document the chosen approach in code comments.
- Cancellation policy:
  - More than 24 hours before pickup: full refund of any captured charges, hold released.
  - Within 24 hours and more than 2 hours before pickup: 50% of subtotal charged, rest refunded.
  - Within 2 hours of pickup or after `EN_ROUTE_TO_CUSTOMER`: 100% of subtotal charged.
  - These thresholds defined as configuration, not hardcoded.
- A customer can only have one `ACTIVATED` booking at a time.
- `NO_SHOW` is triggered by a scheduled job that runs every 15 minutes and marks any `CONFIRMED` booking whose `pickup_at + 2 hours` is in the past.

### 6.6 `pricing` module

**Purpose.** Calculates prices for bookings. Configurable via admin without code changes.

**Database tables:**

```sql
CREATE TABLE pricing_quote (
    id UUID PRIMARY KEY,
    customer_id UUID REFERENCES customer(id), -- nullable for anonymous quote previews
    vehicle_class_id UUID NOT NULL REFERENCES vehicle_class(id),
    pickup_at TIMESTAMPTZ NOT NULL,
    dropoff_at TIMESTAMPTZ NOT NULL,
    dropoff_in_zone BOOLEAN NOT NULL,
    insurance_tier TEXT NOT NULL DEFAULT 'STANDARD',
    line_items JSONB NOT NULL,                 -- structured breakdown
    subtotal_cents BIGINT NOT NULL,
    tax_cents BIGINT NOT NULL,
    total_cents BIGINT NOT NULL,
    deposit_cents BIGINT NOT NULL,
    promo_code TEXT,
    discount_cents BIGINT NOT NULL DEFAULT 0,
    expires_at TIMESTAMPTZ NOT NULL,           -- quotes valid for 15 minutes
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE pricing_rate (
    id UUID PRIMARY KEY,
    vehicle_class_id UUID NOT NULL REFERENCES vehicle_class(id),
    daily_rate_cents BIGINT NOT NULL,
    delivery_fee_cents BIGINT NOT NULL DEFAULT 0,
    out_of_zone_dropoff_fee_cents BIGINT NOT NULL DEFAULT 0,
    insurance_tier TEXT NOT NULL,
    insurance_daily_cents BIGINT NOT NULL,
    effective_from TIMESTAMPTZ NOT NULL,
    effective_to TIMESTAMPTZ,                  -- null = current
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID NOT NULL
);
CREATE INDEX idx_rate_class_active ON pricing_rate(vehicle_class_id, insurance_tier, effective_from);

CREATE TABLE promo_code (
    id UUID PRIMARY KEY,
    code TEXT NOT NULL UNIQUE,
    discount_type TEXT NOT NULL,               -- PERCENT, FIXED
    discount_value INT NOT NULL,               -- percent (0-100) or cents
    max_uses INT,                              -- null = unlimited
    used_count INT NOT NULL DEFAULT 0,
    starts_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL
);
```

**Quote line item structure (JSONB):**

```json
[
  {"type": "RENTAL", "description": "3 days × Compact SUV Hybrid", "amount_cents": 22500},
  {"type": "DELIVERY", "description": "Delivery fee", "amount_cents": 1500},
  {"type": "INSURANCE", "description": "Standard protection × 3 days", "amount_cents": 9000},
  {"type": "OUT_OF_ZONE_DROPOFF", "description": "Out-of-zone dropoff", "amount_cents": 2500},
  {"type": "PROMO", "description": "Promo MATADOR10", "amount_cents": -3550},
  {"type": "TAX", "description": "NC sales tax (6.75%)", "amount_cents": 2200}
]
```

**Service contract:**

```java
public interface PricingService {
    PricingQuote createQuote(QuoteRequest request);
    PricingQuote getQuote(UUID quoteId);
    boolean isQuoteValid(UUID quoteId);
}
```

**Business rules:**

- Quote rounding: days computed as `ceil((dropoff_at - pickup_at) / 24h)`.
- Tax: NC state sales tax 4.75% + local. Configurable per zone. Single rate for Phase 1: 6.75%.
- Deposit: greater of $300 or 30% of subtotal, rounded up to nearest $50.
- Quote TTL: 15 minutes. Expired quotes cannot be used to create bookings.
- Promo code validation: not expired, under max uses, active. Increment `used_count` only on booking confirmation, not quote creation.

**Deferred:** dynamic/surge pricing, hourly rates, customer-tier discounts.

### 6.7 `payment` module

**Purpose.** Wraps Stripe interactions. Manages payment methods and intents. Emits ledger events.

**Database tables:**

```sql
CREATE TABLE payment_method (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customer(id),
    stripe_payment_method_id TEXT NOT NULL UNIQUE,
    type TEXT NOT NULL,                        -- CARD
    brand TEXT,                                -- 'visa', 'mastercard', etc.
    last4 CHAR(4),
    exp_month INT,
    exp_year INT,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    detached_at TIMESTAMPTZ
);
CREATE INDEX idx_payment_method_customer ON payment_method(customer_id) WHERE detached_at IS NULL;

CREATE TABLE payment_intent (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customer(id),
    booking_id UUID REFERENCES booking(id),
    trip_id UUID REFERENCES trip(id),
    stripe_intent_id TEXT NOT NULL UNIQUE,
    purpose TEXT NOT NULL,                     -- DEPOSIT_HOLD, RENTAL_CHARGE, INCIDENT_CHARGE
    amount_cents BIGINT NOT NULL,
    captured_amount_cents BIGINT NOT NULL DEFAULT 0,
    status TEXT NOT NULL,                      -- mirrors Stripe status
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_payment_intent_booking ON payment_intent(booking_id);
CREATE INDEX idx_payment_intent_trip ON payment_intent(trip_id);
```

**REST API:**

| Method | Path | Auth | Purpose |
|---|---|---|---|
| `GET` | `/api/customer/me/payment-methods` | Customer | List saved cards. |
| `POST` | `/api/customer/me/payment-methods/setup` | Customer | Create a SetupIntent. Returns client_secret. |
| `POST` | `/api/customer/me/payment-methods` | Customer | After client-side confirmation, attach payment method to customer. |
| `DELETE` | `/api/customer/me/payment-methods/{id}` | Customer | Detach payment method. |
| `POST` | `/api/customer/me/payment-methods/{id}/default` | Customer | Set default. |
| `POST` | `/api/webhooks/stripe` | Webhook | Stripe events (payment_intent.*, charge.dispute.*). |

**Service operations (called by other modules):**

```java
public interface PaymentService {
    PaymentIntentResult createDepositHold(UUID customerId, UUID bookingId, Money amount, UUID paymentMethodId);
    PaymentIntentResult captureHold(UUID paymentIntentId, Money amount); // partial capture allowed
    void releaseHold(UUID paymentIntentId);
    PaymentIntentResult chargeOffSession(UUID customerId, UUID tripId, Money amount, String description);
    void refund(UUID paymentIntentId, Money amount, String reason);
}
```

**Domain events published:**

- `PaymentHeld(intentId, bookingId, amountCents)`
- `PaymentCaptured(intentId, bookingId, tripId, amountCents)`
- `PaymentReleased(intentId, bookingId, amountCents)`
- `PaymentFailed(intentId, reason)`
- `PaymentRefunded(intentId, amountCents, reason)`
- `PaymentDisputeCreated(intentId, amountCents)`

**Idempotency.** Every Stripe call uses an idempotency key derived from `(operation_type, entity_id, attempt)`. Persist these keys to allow safe retries.

### 6.8 `ledger` module

**Purpose.** Immutable financial ledger. Source of truth for all money state.

**Database tables:**

```sql
CREATE TABLE ledger_entry (
    id UUID PRIMARY KEY,
    occurred_at TIMESTAMPTZ NOT NULL,
    customer_id UUID NOT NULL REFERENCES customer(id),
    booking_id UUID REFERENCES booking(id),
    trip_id UUID REFERENCES trip(id),
    incident_id UUID REFERENCES incident(id),
    entry_type TEXT NOT NULL,                  -- enum
    amount_cents BIGINT NOT NULL,              -- positive = debit customer; negative = credit customer
    currency CHAR(3) NOT NULL DEFAULT 'USD',
    description TEXT NOT NULL,
    payment_intent_id UUID REFERENCES payment_intent(id),
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_ledger_customer ON ledger_entry(customer_id, occurred_at);
CREATE INDEX idx_ledger_booking ON ledger_entry(booking_id);
CREATE INDEX idx_ledger_trip ON ledger_entry(trip_id);
```

**Entry types:**

- `DEPOSIT_HELD` (informational, not a charge; amount = 0)
- `DEPOSIT_CAPTURED` (positive)
- `DEPOSIT_RELEASED` (informational; amount = 0)
- `RENTAL_CHARGED` (positive)
- `OVERAGE_CHARGED` (positive)
- `INCIDENT_CHARGED` (positive)
- `REFUND_ISSUED` (negative)
- `DISPUTE_CHARGEBACK` (positive, against Matador)
- `MANUAL_ADJUSTMENT` (positive or negative; requires admin reason)

**Critical rule.** Ledger entries are insert-only. There is no UPDATE or DELETE on this table. Corrections happen via compensating entries.

**Service operations:**

```java
public interface LedgerService {
    LedgerEntry record(LedgerEntryRequest req);
    Money customerOutstandingBalance(UUID customerId);
    Money tripNetCharges(UUID tripId);
    List<LedgerEntry> findByTrip(UUID tripId);
    List<LedgerEntry> findByCustomer(UUID customerId, PageRequest page);
}
```

The ledger module subscribes to events from `payment`, `trip`, and `incident` modules and writes entries.

**REST API:**

| Method | Path | Auth | Purpose |
|---|---|---|---|
| `GET` | `/api/admin/ledger/trips/{tripId}` | Staff | All entries for a trip. |
| `GET` | `/api/admin/ledger/customers/{customerId}` | Staff | Customer history. |
| `POST` | `/api/admin/ledger/adjustments` | Admin | Manual adjustment with reason. |

### 6.9 `trip` module

**Purpose.** Manages active and completed trips.

**Database tables:**

```sql
CREATE TABLE trip (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL UNIQUE REFERENCES booking(id),
    customer_id UUID NOT NULL REFERENCES customer(id),
    vehicle_id UUID NOT NULL REFERENCES vehicle(id),
    status TEXT NOT NULL,                      -- enum
    actual_pickup_at TIMESTAMPTZ NOT NULL,
    actual_dropoff_at TIMESTAMPTZ,
    actual_pickup_location GEOGRAPHY(POINT, 4326) NOT NULL,
    actual_dropoff_location GEOGRAPHY(POINT, 4326),
    actual_dropoff_address TEXT,
    actual_dropoff_in_zone BOOLEAN,
    odometer_start INT NOT NULL,
    odometer_end INT,
    miles_driven INT,
    pickup_inspection_id UUID REFERENCES inspection(id),
    dropoff_inspection_id UUID REFERENCES inspection(id),
    final_charges_cents BIGINT,                -- computed at close
    closed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_trip_customer ON trip(customer_id);
CREATE INDEX idx_trip_vehicle ON trip(vehicle_id);
CREATE INDEX idx_trip_status ON trip(status);

CREATE TABLE trip_location_sample (
    id UUID PRIMARY KEY,
    trip_id UUID NOT NULL REFERENCES trip(id),
    sampled_at TIMESTAMPTZ NOT NULL,
    location GEOGRAPHY(POINT, 4326) NOT NULL,
    speed_mph NUMERIC(5,2),
    odometer_miles INT,
    fuel_charge_percent INT
);
CREATE INDEX idx_trip_sample_trip_time ON trip_location_sample(trip_id, sampled_at);
```

**Status enum and state machine:**

```
States: IN_PROGRESS, ENDED_PENDING_INSPECTION, CLOSED

Transitions:
  IN_PROGRESS → ENDED_PENDING_INSPECTION (customer or driver ends trip; awaiting dropoff inspection)
  ENDED_PENDING_INSPECTION → CLOSED (inspection complete, final charges recorded)
```

**REST API:**

| Method | Path | Auth | Purpose |
|---|---|---|---|
| `GET` | `/api/customer/me/trips/current` | Customer | Currently active trip if any. |
| `GET` | `/api/customer/me/trips` | Customer | Paginated trip history. |
| `GET` | `/api/customer/me/trips/{id}` | Customer | Trip detail. |
| `POST` | `/api/customer/me/trips/{id}/end` | Customer | End the active trip (customer-initiated). |
| `GET` | `/api/admin/trips` | Staff | List with filters. |
| `GET` | `/api/admin/trips/{id}` | Staff | Detail. |
| `POST` | `/api/admin/trips/{id}/close` | Dispatcher | Close trip after inspection. Computes final charges. |

**Domain events published:**

- `TripStarted(tripId, bookingId, vehicleId, startedAt)`
- `TripEnded(tripId, endedAt, milesDriven)`
- `TripClosed(tripId, finalChargesCents, closedAt)`

**Business rules on close:**

- Compute `miles_driven` from `odometer_end - odometer_start`.
- Compute final charges: subtotal from booking + any out-of-zone dropoff differential + any incidents charged + tax delta.
- Emit `TripClosed` event. Ledger module records `RENTAL_CHARGED`. Payment module captures hold or charges off-session as needed. Booking transitions to `COMPLETED`. Vehicle transitions toward `AWAITING_INSPECTION`.

### 6.10 `inspection` module

**Purpose.** Photo sets at pickup and dropoff.

**Database tables:**

```sql
CREATE TABLE inspection (
    id UUID PRIMARY KEY,
    trip_id UUID NOT NULL REFERENCES trip(id),
    phase TEXT NOT NULL,                       -- PICKUP, DROPOFF
    odometer_miles INT,
    fuel_charge_percent INT,
    notes TEXT,
    submitted_at TIMESTAMPTZ NOT NULL,
    submitted_by_role TEXT NOT NULL,           -- CUSTOMER, DRIVER, STAFF
    submitted_by_id UUID NOT NULL,
    reviewed_at TIMESTAMPTZ,
    reviewed_by UUID,
    review_status TEXT,                        -- PASSED, FLAGGED
    review_notes TEXT
);

CREATE TABLE inspection_photo (
    id UUID PRIMARY KEY,
    inspection_id UUID NOT NULL REFERENCES inspection(id),
    angle TEXT NOT NULL,                       -- FRONT, BACK, LEFT, RIGHT, INTERIOR_FRONT, INTERIOR_REAR, ODOMETER
    url TEXT NOT NULL,                         -- object storage URL
    captured_at TIMESTAMPTZ NOT NULL,
    location GEOGRAPHY(POINT, 4326),
    file_size_bytes INT,
    width_px INT,
    height_px INT
);
```

**Required angles per inspection (Phase 1):** `FRONT`, `BACK`, `LEFT`, `RIGHT`, `INTERIOR_FRONT`, `INTERIOR_REAR`, `ODOMETER`.

**Object storage.** Photos uploaded directly to Cloudflare R2 (S3-compatible) via presigned URLs. Backend generates the URLs, client uploads, then submits the inspection with the URLs.

**REST API:**

| Method | Path | Auth | Purpose |
|---|---|---|---|
| `POST` | `/api/customer/me/trips/{id}/inspections/{phase}/upload-url` | Customer | Get presigned upload URL for an angle. |
| `POST` | `/api/customer/me/trips/{id}/inspections/{phase}` | Customer | Submit inspection with all photo URLs. |
| `GET` | `/api/admin/inspections/{id}` | Staff | View inspection with all photos. |
| `POST` | `/api/admin/inspections/{id}/review` | Staff | Mark passed or flagged. |

**Business rules:**

- Trip cannot transition from `RESERVED` → `WITH_CUSTOMER` (booking activation) until pickup inspection is submitted and all required angles present.
- Trip cannot transition from `ENDED_PENDING_INSPECTION` → `CLOSED` until dropoff inspection submitted.

### 6.11 `telematics` module

**Purpose.** Abstraction over vehicle telematics providers.

**Service contract (the abstraction the rest of the system uses):**

```java
public interface TelematicsProvider {
    String providerName();
    boolean supports(Vehicle vehicle);
    LockResult lock(Vehicle vehicle);
    UnlockResult unlock(Vehicle vehicle);
    VehicleSnapshot snapshot(Vehicle vehicle);  // location, odometer, fuel/charge
}

public record VehicleSnapshot(
    GeoPoint location,
    Integer odometerMiles,
    Integer fuelChargePercent,
    Integer rangeMiles,
    Instant recordedAt
) {}
```

**Implementations:**

- `SmartcarProvider` — primary
- `TeslaFleetProvider` — for Tesla vehicles where direct API preferred
- `MockProvider` — for dev/test

**Provider selection.** `TelematicsService` picks provider by `vehicle.telematics_provider` field.

**Database tables:**

```sql
CREATE TABLE telematics_token (
    id UUID PRIMARY KEY,
    vehicle_id UUID NOT NULL REFERENCES vehicle(id),
    provider TEXT NOT NULL,
    access_token TEXT NOT NULL,                -- encrypted at rest
    refresh_token TEXT NOT NULL,               -- encrypted at rest
    access_token_expires_at TIMESTAMPTZ NOT NULL,
    scopes TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
CREATE UNIQUE INDEX idx_telematics_token_vehicle ON telematics_token(vehicle_id, provider);

CREATE TABLE telematics_command_log (
    id UUID PRIMARY KEY,
    vehicle_id UUID NOT NULL REFERENCES vehicle(id),
    trip_id UUID REFERENCES trip(id),
    command TEXT NOT NULL,                     -- LOCK, UNLOCK, SNAPSHOT
    requested_at TIMESTAMPTZ NOT NULL,
    requested_by UUID NOT NULL,
    succeeded BOOLEAN,
    error TEXT,
    response_payload JSONB
);
```

**Encryption.** Access and refresh tokens encrypted at the application layer using AES-256-GCM with a key from env var. Provide a `Cipher` bean for this.

**Background polling.** A scheduled job runs every 5 minutes, polls snapshots for all vehicles in `WITH_CUSTOMER` status, persists samples to `trip_location_sample`. For all other statuses, poll every 30 minutes. Tune per provider rate limits.

**Webhook ingestion.** Smartcar supports webhooks for some events; configure if available, fall back to polling otherwise.

### 6.12 `incident` module

**Purpose.** Records non-happy-path events tied to trips.

**Database tables:**

```sql
CREATE TABLE incident (
    id UUID PRIMARY KEY,
    trip_id UUID REFERENCES trip(id),
    vehicle_id UUID NOT NULL REFERENCES vehicle(id),
    customer_id UUID REFERENCES customer(id),
    type TEXT NOT NULL,                        -- DAMAGE, OUT_OF_ZONE, LATE_RETURN, ACCIDENT, TICKET, OTHER
    severity TEXT NOT NULL,                    -- LOW, MEDIUM, HIGH
    description TEXT NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    reported_at TIMESTAMPTZ NOT NULL,
    reported_by_role TEXT NOT NULL,
    reported_by_id UUID NOT NULL,
    location GEOGRAPHY(POINT, 4326),
    status TEXT NOT NULL,                      -- OPEN, IN_REVIEW, RESOLVED, DISMISSED
    resolution_notes TEXT,
    resolved_at TIMESTAMPTZ,
    resolved_by UUID,
    charged_amount_cents BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE incident_photo (
    id UUID PRIMARY KEY,
    incident_id UUID NOT NULL REFERENCES incident(id),
    url TEXT NOT NULL,
    caption TEXT,
    uploaded_at TIMESTAMPTZ NOT NULL
);
```

**REST API:**

| Method | Path | Auth | Purpose |
|---|---|---|---|
| `POST` | `/api/customer/me/trips/{id}/incidents` | Customer | Customer reports an incident. |
| `POST` | `/api/admin/incidents` | Staff | Staff creates an incident. |
| `GET` | `/api/admin/incidents` | Staff | List, filterable. |
| `GET` | `/api/admin/incidents/{id}` | Staff | Detail. |
| `PATCH` | `/api/admin/incidents/{id}` | Staff | Update status, resolution, charges. |

**Domain events published:**

- `IncidentReported(incidentId, type, tripId, vehicleId)`
- `IncidentResolved(incidentId, chargedAmountCents)`

### 6.13 `notification` module

**Purpose.** Sends emails and SMS based on domain events.

**Database tables:**

```sql
CREATE TABLE notification (
    id UUID PRIMARY KEY,
    customer_id UUID REFERENCES customer(id),
    channel TEXT NOT NULL,                     -- EMAIL, SMS
    template TEXT NOT NULL,                    -- e.g., 'booking-confirmed'
    payload JSONB NOT NULL,
    recipient TEXT NOT NULL,
    status TEXT NOT NULL,                      -- PENDING, SENT, FAILED
    sent_at TIMESTAMPTZ,
    error TEXT,
    created_at TIMESTAMPTZ NOT NULL
);
```

**Templates (Phase 1):**

- `customer-welcome` (registration)
- `verification-required`, `verification-completed`
- `booking-confirmed`, `booking-cancelled`
- `vehicle-on-the-way` (delivery driver dispatched, ~30 min ETA)
- `trip-started`, `trip-ending-soon`, `trip-ended`
- `payment-failed`
- `incident-reported`

**Event subscriptions.** Each template binds to a domain event. The notification module subscribes to events and creates `notification` records. A background sender flushes pending notifications via Postmark/Twilio.

---

## 7. Webhook contracts

### 7.1 Stripe webhook (`POST /api/webhooks/stripe`)

Events to handle:

| Stripe event | Action |
|---|---|
| `payment_intent.succeeded` | Update payment_intent.status, emit PaymentCaptured (if RENTAL_CHARGE) or PaymentHeld (if DEPOSIT_HOLD with `requires_capture`). |
| `payment_intent.payment_failed` | Update status, emit PaymentFailed. |
| `payment_intent.canceled` | Update status, emit PaymentReleased. |
| `charge.refunded` | Emit PaymentRefunded. |
| `charge.dispute.created` | Emit PaymentDisputeCreated. |
| `setup_intent.succeeded` | Attach payment method to customer. |
| `identity.verification_session.verified` | Forward to identity module. |
| `identity.verification_session.requires_input` | Forward to identity module. |
| `identity.verification_session.canceled` | Forward to identity module. |

All events stored in `webhook_event` table for audit and idempotency.

### 7.2 Smartcar webhook (`POST /api/webhooks/smartcar`)

Handle vehicle event notifications (location updates, etc.) where Smartcar provides them.

---

## 8. Configuration

`application.yml` keys (with env var overrides):

```yaml
matador:
  jwt:
    private-key-pem: ${JWT_PRIVATE_KEY}
    public-key-pem: ${JWT_PUBLIC_KEY}
    access-ttl-minutes: 15
    refresh-ttl-days: 30
  stripe:
    secret-key: ${STRIPE_SECRET_KEY}
    webhook-secret: ${STRIPE_WEBHOOK_SECRET}
    identity-webhook-secret: ${STRIPE_IDENTITY_WEBHOOK_SECRET}
  smartcar:
    client-id: ${SMARTCAR_CLIENT_ID}
    client-secret: ${SMARTCAR_CLIENT_SECRET}
    webhook-secret: ${SMARTCAR_WEBHOOK_SECRET}
  twilio:
    account-sid: ${TWILIO_ACCOUNT_SID}
    auth-token: ${TWILIO_AUTH_TOKEN}
    from-number: ${TWILIO_FROM_NUMBER}
  postmark:
    server-token: ${POSTMARK_SERVER_TOKEN}
    from-address: ${POSTMARK_FROM_ADDRESS}
  storage:
    r2-account-id: ${R2_ACCOUNT_ID}
    r2-access-key: ${R2_ACCESS_KEY}
    r2-secret-key: ${R2_SECRET_KEY}
    r2-bucket: ${R2_BUCKET}
    r2-public-base-url: ${R2_PUBLIC_BASE_URL}
  cipher:
    key-base64: ${ENCRYPTION_KEY_BASE64}
  pricing:
    quote-ttl-minutes: 15
    tax-rate-bps: 675                          # 6.75%
    deposit-min-cents: 30000
    deposit-percent-bps: 3000                  # 30%
  policy:
    min-customer-age: 21
    min-booking-hours: 4
    max-booking-days: 30
    pickup-lead-time-minutes: 120
    no-show-after-minutes: 120
```

---

## 9. Testing requirements

**Coverage targets:**

- Domain logic (services, state machines, pricing): 90%+ unit test coverage
- REST controllers: integration tests for all endpoints (happy path + key error paths) via `@SpringBootTest` + Testcontainers Postgres
- External integrations: contract tests with WireMock
- Spring Modulith verification: `@ApplicationModuleTest` ensures no cross-module internal access

**Categories of tests required for each module:**

1. **Domain unit tests.** Pure logic, no Spring context. Test state machine transitions, pricing math, business rule validation.
2. **Repository tests.** `@DataJpaTest` against Testcontainers Postgres. Verify queries, indexes work.
3. **Service tests.** With Spring context, mocking external collaborators.
4. **Controller tests.** Full `@SpringBootTest` with MockMvc or WebTestClient. Authentication setup via `@WithMockUser` for staff or test JWTs for customers.
5. **Event flow tests.** `@ApplicationModuleTest` with `Scenario` API from Spring Modulith to verify event-driven workflows.

Provide a `make test` or `./gradlew check` that runs everything.

---

## 10. Build & deploy

**Build.** `./gradlew build` produces a Spring Boot executable jar.

**Run locally.** Docker Compose file in repo root provides Postgres-with-PostGIS, Mailpit (email capture), and the backend. `docker compose up`.

**OpenAPI generation.** springdoc-openapi serves the live spec at `GET /v3/api-docs` (configured to be accessible in non-production, restricted in production). A Gradle task `./gradlew generateOpenApiSpec` writes a static `openapi.json` to `build/openapi/openapi.json`. CI uploads this file as a release asset and copies it to a stable URL (`https://api.matador.com/openapi/latest.json`) after each deploy. See `PROJECT.md` § 6.1 for how the frontend consumes this contract.

**OpenAPI annotation requirements.** Every controller method MUST have:
- `@Operation(summary = "...", description = "...")`
- `@Tag(name = "...")` at controller level (one of: `Customer-Auth`, `Customer-Bookings`, `Customer-Trips`, `Customer-Profile`, `Customer-Vehicles`, `Admin-Vehicles`, `Admin-Bookings`, `Admin-Trips`, `Admin-Customers`, `Admin-Incidents`, `Admin-Ledger`, `Admin-Settings`, `Webhooks`)
- Documented response codes via `@ApiResponses` for all non-200 outcomes
- Request/response DTOs as concrete records, not generic `Map<String, Object>` or wildcards

The OpenAPI output is a published contract. Breaking changes require coordination with the frontend repo per `PROJECT.md` § 6.2.

**CI.** GitHub Actions workflow (`.github/workflows/ci.yml`):

1. `build`: checkout, set up JDK 21, cache Gradle, `./gradlew build`
2. `test`: `./gradlew check` with Testcontainers
3. `module-verify`: Spring Modulith ArchUnit checks
4. `openapi-generate`: `./gradlew generateOpenApiSpec`, upload `openapi.json` as workflow artifact
5. `deploy-staging` (on push to main): build container image, push to registry, deploy to staging, copy `openapi.json` to stable URL

On version tags (`v*`), publish `openapi.json` to the corresponding GitHub Release.

**Dockerfile.** Multi-stage build, distroless or eclipse-temurin JRE base, non-root user.

---

## 11. Implementation order

Build the backend in this order. Each phase produces a runnable system with increasing capability.

**Phase 1.1 — Foundation (Week 1–2).**

1. Scaffold project. Configure Gradle, Spring Boot, Spring Modulith, Postgres, Flyway, Sentry, logging, error handler, IdGenerator, Money, Clock.
2. Implement shared `security` package with Spring Security configuration for both session (admin) and JWT (customer).
3. Implement `customer` and `identity` modules end-to-end including REST endpoints, tests.
4. Manually test registration → verification flow with Stripe Identity sandbox.

**Phase 1.2 — Inventory (Week 3).**

5. Implement `zone` module with one seeded zone (Triangle).
6. Implement `vehicle` and `vehicle_class` modules including admin REST endpoints.
7. Seed sample vehicles for testing.

**Phase 1.3 — Booking & Pricing (Week 4–5).**

8. Implement `pricing` module with quote creation and admin rate management.
9. Implement `payment` module (Stripe SDK wrapper, payment methods, intents).
10. Implement `ledger` module (insert-only, event-driven).
11. Implement `booking` module including quote → booking → confirm with deposit hold.
12. Manually run end-to-end: register, verify, browse, get quote, create booking, confirm payment hold.

**Phase 1.4 — Trip lifecycle (Week 6–7).**

13. Implement `inspection` module with R2 presigned uploads.
14. Implement `trip` module with state machine, location samples.
15. Implement booking activation → trip start. Trip end → close → final charges.

**Phase 1.5 — Telematics & Incidents (Week 8–9).**

16. Implement `telematics` module with Smartcar provider and Mock provider. Polling job.
17. Implement `incident` module with reporting and resolution.

**Phase 1.6 — Notifications & polish (Week 10).**

18. Implement `notification` module with Postmark and Twilio. Subscribe to all event types.
19. End-to-end happy-path test. Run a full simulated trip via the admin UI.

---

## 12. Anti-patterns to avoid

- Do **not** extract microservices. Modules communicate via in-process events.
- Do **not** use entity objects in API responses. Map to DTOs/records via MapStruct.
- Do **not** access another module's internal classes. Use the module's `*Service` only.
- Do **not** compute money in `double` or `BigDecimal`. Use `Money` (cents).
- Do **not** call Stripe or other external APIs from within a transaction. Make external calls after `@TransactionalEventListener(AFTER_COMMIT)` or in dedicated service methods without `@Transactional`.
- Do **not** store secrets in `application.yml`. Use env vars or a secret manager.
- Do **not** hand-write SQL where JPA queries suffice. Use JPQL or Specifications. Resort to native SQL only for PostGIS operations.
- Do **not** log payment data, full email addresses, or personal IDs at INFO level.
