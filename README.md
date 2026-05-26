# matador-backend

Spring Boot 3 modular monolith (Java 21) for Matador — on-demand car rental.

See [`docs/PROJECT.md`](docs/PROJECT.md) for product orientation and [`docs/BACKEND.md`](docs/BACKEND.md) for the service specification.

## Prerequisites

- JDK 21
- Docker (for Postgres + Mailpit, and Testcontainers-based tests)

## Local development

Start dependencies (Postgres with PostGIS + Mailpit):

```bash
docker compose up -d postgres mailpit
```

Run the app (dev profile):

```bash
./gradlew bootRun
```

The API serves at `http://localhost:8080`. OpenAPI UI at `http://localhost:8080/swagger-ui.html`, raw spec at `http://localhost:8080/v3/api-docs`.

Run the full stack (app in a container too):

```bash
docker compose --profile full up --build
```

## Testing

```bash
./gradlew check        # unit + integration tests + Spring Modulith verification
```

Integration tests use Testcontainers and require a running Docker daemon.

## Configuration

All secrets are supplied via environment variables (never committed). See [`docs/BACKEND.md`](docs/BACKEND.md) §8 for the full key list. For local dev, defaults in `application-dev.yml` point at the docker-compose Postgres and Mailpit; external integrations (Stripe, Smartcar, etc.) run against mocks/sandboxes.

## Module layout

Each business capability is a Spring Modulith application module under `com.matador.<module>`. Cross-cutting concerns live under `com.matador.shared`. Module internals (`internal/` packages) are not accessible across modules — enforced by the Spring Modulith verification test.
