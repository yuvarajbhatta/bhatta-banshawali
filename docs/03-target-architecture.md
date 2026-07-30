# 03 — Target Architecture

## Decision Summary

See `adr/001-frontend-and-backend-architecture.md` for the formal ADR. Summary: **Next.js (TypeScript, App Router) frontend + modernized Spring Boot REST API backend + MySQL**, replacing Thymeleaf entirely but keeping the JVM backend and current database engine.

## Why Not the Alternatives

| Option | Verdict | Reason |
|---|---|---|
| A: Next.js + Spring Boot REST + MySQL/Postgres | **Chosen** | Reuses working recursive-tree and relationship logic; the team's proven depth is in Spring, not Node; lowest migration risk for the backend half of the system. |
| B: Next.js full-stack (Node API + Prisma/Drizzle + Postgres) | Rejected | Would require re-deriving already-working genealogy query logic (auto-linking, tree building) in a new language and ORM, for a stated goal (frontend modernization) that does not require it. Doubles surface area of the rewrite for no functional gain. |
| C: Vite/React SPA + Spring Boot | Rejected | Forgoes SSR for public/history pages (SEO, first-load performance) with no compensating benefit over Next.js; Next.js can still serve pure client components for the tree, so there is no interactivity loss from choosing it over Vite. |

## Backend (Spring Boot)

- **Stays Spring Boot**, upgraded incrementally, restructured into a clean layered REST API: `controller` (thin, DTO-only) → `service` (business rules, authorization checks) → `repository` (Spring Data JPA) → `entity` (never returned directly from controllers).
- **DTOs per use case**, not shared with entities — e.g. a `PersonSummaryDto` for search results vs. a `PersonProfileDto` for the profile page vs. an admin-only `PersonAdminDto` with full audit fields.
- **OpenAPI** generated from the controller layer (springdoc-openapi), used to generate a typed TypeScript client for the Next.js app — eliminates hand-maintained request/response types drifting from the backend.
- **Session-based auth** via Spring Security + Spring Session (see `05-auth-and-verification.md` for the full rationale), not JWT-in-browser-storage.
- **Versioned migrations** via Flyway, replacing `ddl-auto=update`. `ddl-auto` is disabled entirely once Flyway is introduced.
- **Centralized authorization**: role/permission checks expressed once (method-security annotations backed by a permission-evaluator, not scattered `hasRole()` calls duplicated between `SecurityConfig` request matchers and controller logic as today).

## Frontend (Next.js)

- **App Router**, TypeScript strict mode.
- **Server components** for public pages (landing, history, about, statistics) — fast first load, good for pre-authentication content that doesn't change per-request.
- **Client components** for the interactive tree (React Flow), dashboard widgets, and forms with live AD/BS conversion.
- **i18n**: `next-intl` (or equivalent) driving English/Nepali strings, mirroring the message-key structure already established in `messages.properties`/`messages_ne.properties` so existing translations can be ported directly rather than re-translated from scratch.
- **Design system**: a small internal component library (buttons, cards, forms, dialogs, toasts, skeletons, tree-node styles) built once and reused, per `06-ui-ux-specification.md` — not ad hoc per-page CSS as today's `static/css/*.css` files are.
- **Auth**: same-origin (or same-parent-domain) cookie session shared with the Spring Boot API behind a reverse proxy; Next.js route handlers proxy authenticated API calls server-side where helpful for security (keeping the session cookie httpOnly and never exposed to client JS).

## Database

- **Stay on MySQL** (already provisioned in production; MySQL 8+ supports `WITH RECURSIVE`, covering ancestor/descendant traversal needs). A Postgres migration is not ruled out permanently, but it is not justified by any concrete limitation identified during this assessment — introducing a database migration *simultaneously* with a full-stack rewrite would multiply risk without a corresponding, demonstrated need.
- **Query strategy**: materialized-path or closure-table read model for ancestor/descendant lookups, built and incrementally maintained alongside the normalized `Relationship` edges (the edges remain the source of truth; the path/closure table is a derived, cache-invalidated projection). This directly extends the existing `buildLineageTree` pre-load pattern rather than replacing it with something unrelated.
- **Full-text/normalized name search**: a generated/indexed normalized-name column (folding case, diacritics, and transliteration variants) backing `PersonRepository`-style search, replacing the current `LIKE '%...%'` scan which cannot use an index and will not scale past a few thousand rows.

## Genealogy Visualization

- **React Flow** for node/edge rendering and interaction (pan/zoom/selection), **Dagre or ELK.js** for automatic hierarchical layout of subtrees.
- **Server-driven, viewport/branch-scoped payloads**: the API returns only the generations/branches currently in view or requested (lazy expand/collapse), never the entire graph in one response. This is a hard requirement once the dataset exceeds a few hundred people, and the API should be designed this way from the start rather than retrofitted.
- **Benchmark before committing**: load-test the chosen approach against synthetic datasets of 500, 2,000, and (if plausible for this family's actual size) 10,000 people before finalizing the lazy-loading page-size defaults.

## Authentication & Sessions

- Spring Security server-side sessions (Spring Session JDBC or Redis-backed, decided in Phase 1 based on operational simplicity for a single-host deployment — JDBC-backed sessions avoid introducing a new infrastructure dependency and are likely sufficient at this scale).
- CSRF protection retained (as it is today) since cookie-based auth is used.
- Rationale documented fully in `05-auth-and-verification.md`.

## Deployment

- Retains the self-hosted-runner → `/srv` deployment model already in place, extended to build and deploy both the Spring Boot API and the Next.js app as separate processes/containers behind a shared reverse proxy (e.g., Nginx or Caddy), with the `SPRING_PROFILES_ACTIVE=prod` misconfiguration (see `01-current-system-assessment.md`) fixed as an immediate, independent action.
- Environments: local, test, production. **Decided (2026-07-30)**: no persistent staging tier. This is a single shared home-server host running several unrelated personal apps side by side (nextcloud, meropasal-pos, trading-copilot, etc.) via systemd + one shared cloudflared tunnel + shared MySQL — provisioning a parallel always-on app environment here adds real risk (port/tunnel/systemd config shared with other live apps) for a benefit (a persistently reachable pre-prod copy) this project doesn't need. Pre-cutover migration verification is instead done via a disposable, on-demand database — see `07-migration-plan.md` and `scripts/migration-rehearsal.sh`.

## What This Explicitly Avoids

- No graph database (not justified by scale).
- No microservices split (single Spring Boot API is sufficient for this domain and team size).
- No JWT access/refresh token scheme (unnecessary complexity for a same-origin deployment; sessions are simpler to secure correctly here).
- No premature Kubernetes/multi-region infrastructure.
