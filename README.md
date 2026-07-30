# FamilyTree

Spring Boot backend + Next.js frontend for tracking and browsing the family lineage.

## Quick Start

1. Start MySQL:

```bash
docker compose up -d
```

2. Start the backend:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

3. Start the frontend in another terminal:

```bash
cd frontend
npm install
npm run dev
```

4. Open the apps:

- Frontend: `http://localhost:3000`
- Backend: `http://localhost:8080`
- Health: `http://localhost:8080/actuator/health`
- Swagger: `http://localhost:8080/swagger-ui.html`

Log in with `admin` / `admin123` (seeded via `app.admin.*` in `application-local.properties`).

## Profiles

- `local` (`application-local.properties`) — pure local development against the dockerized MySQL
  from `docker-compose.yml` (`localhost:3307`). Use this on your dev machine.
- `dev` (`application-dev.properties`) — this is also the profile production silently falls back
  to by default (`spring.profiles.default=dev`), with its real datasource and credentials supplied
  by the external config at `/srv/config/familytree/` on the prod host. See
  `docs/01-current-system-assessment.md` for the background on this pre-existing quirk. Don't rely
  on this file for local development; use `local` instead.
- `prod` (`application-prod.properties`) — reads `SPRING_DATASOURCE_*` / `APP_ADMIN_*` env vars.
  Not currently activated in production (see note above); mainly relevant if/when the `dev`-in-prod
  issue above gets fixed.

## Stack

- Java 21, Spring Boot, Flyway, MySQL
- Next.js frontend (`frontend/`)

See `docs/` for the full architecture, data model, and migration plans.
