# Matador — Project Specification

> **Purpose of this document:** Top-level project orientation. Read this first before any service-specific spec. This file lives in both repositories (`matador-backend` and `matador-web`) and is identical between them.
>
> Companion documents:
> - In `matador-backend/docs/`: `BACKEND.md`
> - In `matador-web/docs/`: `FRONTEND-WORKSPACE.md`, `FRONTEND-ADMIN.md`, `FRONTEND-CUSTOMER.md`

---

## 1. Product summary

Matador is an on-demand car rental service operating a fleet of owned/leased vehicles in a defined geographic zone (the "Matador Zone"). Customers book a vehicle through the app, the vehicle is delivered to them by a Matador driver, they drive it for the booked period, and either return it to the original pickup point or drop it elsewhere within the zone (or outside, with a surcharge). The model is **not peer-to-peer** — all vehicles are owned by the operator. Initial launch market: Raleigh–Durham, North Carolina.

The platform consists of two repositories:

1. **`matador-backend`** — Spring Boot 3 modular monolith (Java 21) handling all business logic, persistence, integrations, and APIs. Serves both internal staff and end customers via separate API namespaces.
2. **`matador-web`** — Angular 19 workspace containing two applications (admin console + customer PWA) and a shared library, providing consistent UX across staff and customer surfaces.

A native iOS customer app is planned for later phases and is **out of scope** for this spec set.

---

## 2. Repository topology

### 2.1 `matador-backend`

Spring Boot service. Single deployable.

```
matador-backend/
├── build.gradle.kts
├── settings.gradle.kts
├── docker-compose.yml          # local Postgres + Mailpit
├── Dockerfile
├── src/
│   ├── main/java/com/matador/
│   └── test/
├── docs/
│   ├── PROJECT.md              # this file
│   └── BACKEND.md
└── README.md
```

See `BACKEND.md` for the full module layout and module specifications.

### 2.2 `matador-web`

Angular workspace. Two deployable applications plus a shared library.

```
matador-web/
├── angular.json                # workspace config, defines all 3 projects
├── package.json                # one set of dependencies for the whole workspace
├── tsconfig.json               # base TS config, projects extend
├── tsconfig.base.json
├── .eslintrc.json              # shared lint config
├── .prettierrc
├── projects/
│   ├── admin-web/              # the admin app (FRONTEND-ADMIN.md)
│   │   ├── src/
│   │   ├── tsconfig.app.json
│   │   └── tsconfig.spec.json
│   ├── customer-web/           # the customer PWA (FRONTEND-CUSTOMER.md)
│   │   ├── src/
│   │   ├── ngsw-config.json
│   │   ├── tsconfig.app.json
│   │   └── tsconfig.spec.json
│   └── shared/                 # shared library, both apps import from here
│       ├── src/
│       │   └── public-api.ts
│       ├── ng-package.json
│       ├── tsconfig.lib.json
│       └── tsconfig.spec.json
├── docs/
│   ├── PROJECT.md              # this file
│   ├── FRONTEND-WORKSPACE.md   # workspace-level setup
│   ├── FRONTEND-ADMIN.md
│   ├── FRONTEND-CUSTOMER.md
│   └── mockups/                # uploaded design references
└── README.md
```

See `FRONTEND-WORKSPACE.md` for workspace tooling, shared library conventions, and build/deploy commands. See the per-app specs for feature details.

---

## 3. System architecture

```
┌─────────────────────┐         ┌─────────────────────┐
│   Customer PWA      │         │    Admin Web        │
│   (Angular 19)      │         │   (Angular 19)      │
│   app.matador.com   │         │  admin.matador.com  │
└──────────┬──────────┘         └──────────┬──────────┘
           │ HTTPS / JWT                   │ HTTPS / Session
           │                               │
           └───────────────┬───────────────┘
                           │
                ┌──────────▼───────────┐
                │   Spring Boot API    │
                │  (Modular Monolith)  │
                │   api.matador.com    │
                └──────────┬───────────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
   ┌────▼────┐       ┌─────▼─────┐       ┌────▼─────┐
   │Postgres │       │ Stripe    │       │ Smartcar │
   │+PostGIS │       │ Identity  │       │ Tesla API│
   └─────────┘       │ Persona   │       │ Twilio   │
                     │ Postmark  │       │ Mapbox   │
                     └───────────┘       └──────────┘
```

External integrations:

| Service | Purpose | Notes |
|---|---|---|
| **Stripe** | Payments, security holds, refunds | PaymentIntents with manual capture for deposits |
| **Stripe Identity** | ID & driver's license verification | Alternative: Persona (do not implement both) |
| **Smartcar** | Vehicle telematics (lock/unlock, location, fuel/charge, odometer) | Multi-OEM abstraction |
| **Tesla Fleet API** | Direct Tesla integration where applicable | Optional in Phase 1 |
| **Twilio** | SMS notifications | Customer trip events |
| **Postmark** | Transactional email | Alternative: Resend |
| **Mapbox** | Map rendering on customer & admin | Static maps + GL JS |
| **Google Places API** | Address autocomplete | Customer booking flow |

---

## 4. Implementation phasing

The full system has been planned out to 18+ months. **This spec set covers Phase 1 only.** Out-of-scope items are explicitly listed in each spec under a `Deferred` heading.

**Phase 1 scope:**

- **Backend**: all core domain modules, admin-facing REST API, customer-facing REST API, Stripe payments, Stripe Identity verification, Smartcar telematics integration (lock/unlock, status), email/SMS notifications, structured logging, OpenAPI specification output.
- **Admin web**: vehicle management, customer management, booking management, trip management, photo inspection review, ledger viewing, basic dashboard.
- **Customer web**: account creation with verification, vehicle browse, booking flow, payment, trip view, trip history, photo inspection at pickup/dropoff, support handoff.

**Explicitly deferred (not in this spec set):**

- Driver dispatch app (separate iOS/web app, later)
- Native iOS customer app
- Multi-zone support (build for single zone; design schema to allow zones but operate one)
- Loyalty program, referrals (build minimal promo code support only)
- B2B accounts
- Trip extension mid-trip, dropoff-elsewhere with dynamic pricing, route modification mid-trip (designed in mockups; defer implementation)
- Hourly pricing (daily-only in Phase 1)
- Multiple insurance tiers (single standard tier; build the data structure to allow tiers)
- Advanced fraud detection beyond Stripe Radar defaults

---

## 5. Cross-cutting decisions

These apply across both repositories.

**Time zones.** All timestamps stored as UTC in the database (`TIMESTAMPTZ` in Postgres). All client-facing timestamps presented in the customer's local timezone (default America/New_York for Raleigh launch). Date-only values (e.g., DOB) stored as `DATE`.

**Money.** All monetary values stored as integer cents in a `BIGINT` column with an accompanying `currency` column (always `'USD'` for Phase 1). Never use `FLOAT` or `DOUBLE` for money. All financial state derives from the `ledger_entry` table — never from booking or trip status. Frontends format money via a shared `MoneyPipe` in the shared library.

**IDs.** All primary keys are UUIDv7 (time-ordered). Use the `uuid_generate_v7()` extension or generate in application code. Never expose raw database sequence integers in APIs.

**API style.** REST over HTTPS, JSON request/response, `application/json` content type. Error responses follow [RFC 7807 Problem Details](https://datatracker.ietf.org/doc/html/rfc7807). Standard HTTP status codes (200, 201, 400, 401, 403, 404, 409, 422, 500). Versioning via URL prefix: `/api/v1/...`.

**Authentication.** Admin app uses session cookies (Spring Security default). Customer app uses JWT bearer tokens (access + refresh). Webhook endpoints verify provider signatures.

**Authorization model.** Role-based: `ADMIN`, `DISPATCHER`, `SUPPORT`, `READONLY` for staff; `CUSTOMER` for end users. Customers can only access their own resources. Staff role permissions enforced at controller level via `@PreAuthorize`.

**Localization.** English (en-US) only for Phase 1. Structure copy in i18n-ready files even though we ship one locale.

**Observability.** Structured JSON logging on backend. Every log line includes `trace_id`, `user_id` (where known), `trip_id` (where applicable). Sentry for error tracking on backend and both frontends. PostHog for product analytics on customer-facing surfaces only.

**Testing minimums.** Backend: unit tests for all domain logic, integration tests for all REST endpoints (using Testcontainers + Postgres), contract tests for external integrations using WireMock. Frontends: unit tests for services and complex components, e2e smoke tests for critical flows (booking, payment) via Playwright.

**CI/CD.** GitHub Actions in both repos. On PR: build, test, lint. On main merge: build, test, deploy to staging. Manual promotion to production. No production deploys without passing tests.

---

## 6. Cross-repo coordination

The two repositories coordinate via four mechanisms. Treat these as the contract.

### 6.1 OpenAPI as the API contract

The backend generates `openapi.json` at build time using **springdoc-openapi**. Both customer and admin APIs are documented in the same file, grouped by tag.

**Publication.** Every backend release produces a versioned `openapi.json` artifact, published as a GitHub Release asset on the `matador-backend` repo (e.g., `openapi-v1.4.2.json`). The latest stable version is also available at a stable URL (`https://api.matador.com/v3/api-docs` for the live spec; a CI step copies it to `https://api.matador.com/openapi/latest.json` after deployment).

**Consumption.** The frontend workspace generates TypeScript clients from `openapi.json` using **openapi-typescript** (types only) plus a thin Angular service wrapper, OR **ng-openapi-gen** (services + types together). Generated code lives in `projects/shared/src/lib/api-client/` and is committed to the frontend repo. Regeneration is a manual step triggered by a new backend release.

**Why not auto-regenerate on every backend change?** Because the frontend should pin to a known-good contract version. Auto-regeneration would cause silent breakage. The frontend `package.json` records the consumed openapi version explicitly.

### 6.2 Versioning & breaking changes

When the backend makes a breaking API change:

1. The backend PR is merged first, tagged with a minor or major semver bump.
2. The frontend PR is opened against the new `openapi.json`, regenerates clients, updates any affected code, and merges.
3. Backend deploys to staging first. Frontend deploys after backend staging is verified.
4. Production deploy order: backend → frontend, with a brief grace window.

**Backward compatibility expectation.** Within a major version, the backend SHOULD NOT remove or break existing endpoints. Additive changes (new endpoints, new optional fields) require only a minor bump and don't block the frontend. Required-field additions to request bodies, removed fields from responses, changed status codes — these are breaking and require coordinated PRs.

### 6.3 Environment configuration

Both repos use the same environment names: `dev`, `staging`, `prod`.

| Environment | Backend URL | Customer URL | Admin URL |
|---|---|---|---|
| dev (local) | `http://localhost:8080` | `http://localhost:4200` | `http://localhost:4201` |
| staging | `https://api.staging.matador.com` | `https://app.staging.matador.com` | `https://admin.staging.matador.com` |
| prod | `https://api.matador.com` | `https://app.matador.com` | `https://admin.matador.com` |

Frontend `environment.ts` files (in `projects/admin-web/src/environments/` and `projects/customer-web/src/environments/`) record the backend URL per environment. Build target selects the environment file via Angular's `fileReplacements`.

### 6.4 Shared issue tracking

Use a single GitHub Project (board) that spans both repos. Issues are filed in whichever repo the work occurs; the board links them by milestone. Cross-cutting issues (e.g., "Add `vehicle_color` field for customer display") are tracked as parent issues with linked PRs in each repo.

---

## 7. Glossary

Terminology used consistently across all specs:

- **Booking** — A future or current reservation by a customer for a vehicle over a time range. Becomes a Trip when activated.
- **Trip** — An active or completed rental. One-to-one with a Booking after activation.
- **Vehicle** — A physical car in the fleet, identified by VIN.
- **Vehicle Class** — A grouping of similar vehicles for booking purposes (e.g., "Compact SUV Hybrid"). Customers book a class; the system assigns a specific Vehicle at delivery.
- **Zone** — A geofenced area within which Matador operates. Phase 1 has one zone (Triangle). Stored as a PostGIS polygon.
- **Inspection** — A photo set captured at pickup or dropoff documenting vehicle condition.
- **Incident** — Any non-happy-path event: damage, late return, out-of-zone violation, accident, ticket.
- **Ledger Entry** — An immutable record of a financial event. Source of truth for all money state.
- **Hold** — A Stripe PaymentIntent in `requires_capture` state used as a security deposit. Captured (charged) or cancelled (released).

---

## 8. How an agent should approach this codebase

When working on either repository:

1. **Read `PROJECT.md` first** (you're reading it now), then read the spec relevant to your repo:
   - In `matador-backend`: read `BACKEND.md`.
   - In `matador-web`: read `FRONTEND-WORKSPACE.md` first to understand the workspace and shared library, then the per-app specs in the order you'll implement them.
2. **Scaffold before implementing features.** Don't get deep into one module/feature before the scaffold is in place — flows are easier to validate end-to-end.
3. **Follow the implementation order** listed at the end of each spec. Each phase produces a runnable system.
4. **Do not implement deferred features.** When you encounter a feature in the visual mockups (e.g., "Extend Trip" button) that is marked deferred, scaffold the UI element disabled or hidden behind a feature flag, but do not build the flow.
5. **Respect the API contract.** Frontend agents must consume the OpenAPI-generated client and not hand-craft DTOs. Backend agents must update the OpenAPI annotations whenever they change a controller.
6. **Ask for clarification rather than guessing on business rules.** This spec is intentionally explicit on rules; if you find an underspecified area, raise it.
7. **Write tests as you go, not at the end.**
