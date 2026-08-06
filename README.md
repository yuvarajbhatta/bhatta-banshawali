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
- `dev` (`application-dev.properties`) — the profile Spring falls back to if `SPRING_PROFILES_ACTIVE`
  is ever unset (`spring.profiles.default=dev`); no longer prod's actual day-to-day profile (see
  below), just its safety net. Don't rely on this file for local development; use `local` instead.
- `prod` (`application-prod.properties`) — the profile production actually runs under. Activated via
  `SPRING_PROFILES_ACTIVE=prod` in the systemd `EnvironmentFile`
  (`/srv/config/familytree/familytree.env`). Real datasource/admin credentials come from the
  external, git-ignored `/srv/config/familytree/application.properties` (loaded via
  `--spring.config.additional-location`), not from env vars.

## Stack

- Java 21, Spring Boot, Flyway, MySQL
- Next.js frontend (`frontend/`)

See `docs/` for the full architecture, data model, and migration plans.
