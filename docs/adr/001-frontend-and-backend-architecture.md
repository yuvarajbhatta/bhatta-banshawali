# ADR 001 — Frontend and Backend Architecture

## Status
Accepted (Phase 0)

## Context

The existing FamilyTree application is a solo-maintained Spring Boot 4 / Java 21 monolith using Thymeleaf server-rendered templates, MySQL, and Spring Security form login with two roles. It is deployed via a self-hosted GitHub Actions runner to `/srv` on a single host. The product vision requires a premium, interactive, bilingual genealogy platform (interactive tree with pan/zoom/lazy-loading, AD/BS date handling, confidence-scored signup verification, six-role authorization, privacy-classified data) that the current Thymeleaf + vanilla-JS UI is not well suited to deliver at the intended quality bar.

Three architecture options were evaluated:

- **Option A**: Next.js + TypeScript frontend, existing Spring Boot backend converted to a clean REST API, MySQL or Postgres, OpenAPI-generated client, session or token auth.
- **Option B**: Next.js full-stack application (Node/TypeScript API layer, Postgres, Prisma/Drizzle).
- **Option C**: Separate React/Vite frontend, Spring Boot REST backend (no SSR).

## Decision

**Option A**: Next.js (App Router, TypeScript) frontend; Spring Boot backend retained and converted into a clean, versioned REST API; MySQL retained as the database; server-side sessions for authentication.

## Reasoning

- **Migration risk**: the current `RelationshipService` already contains correct, non-trivial genealogy logic (auto-linking inverse relationships, spouse inference, recursive tree building with N+1-safe pre-loading). Rewriting this in a new language (Option B) means re-deriving and re-testing business rules that already work, for no benefit tied to the stated goal (a better frontend). Option A reuses this logic directly.
- **Code reuse**: the bilingual infrastructure (`messages*.properties`, `LocaleConfig`, `NameTransliterationService`, `PersonDisplayHelper`) and the test suite (1,600+ lines) are Spring/Java artifacts. Keeping Spring Boot preserves this investment; discarding it (Option B) throws away working, tested code for no stated requirement.
- **Team productivity**: git history shows a single maintainer with deep, demonstrated Spring Boot fluency (custom `ConfigurationProperties`, `CommandLineRunner`-based initializers, JPQL queries) and comparatively less demonstrated Node/TypeScript backend experience. Option A plays to existing strength; Option B requires building backend fluency in a second stack from scratch while also learning Next.js on the frontend.
- **Security**: Spring Security is already correctly wired (CSRF, bcrypt-family password hashing via `DelegatingPasswordEncoder`, role-based authorization) — Option A extends a working security foundation rather than reimplementing auth server-side in Node.
- **Type safety**: OpenAPI-generated TypeScript clients from the Spring controllers give end-to-end type safety without requiring the backend itself to be TypeScript — addressing the "no untyped API payloads" engineering standard without an Option B rewrite.
- **Performance / genealogy-query requirements**: MySQL 8+ (already provisioned) supports `WITH RECURSIVE`, sufficient for ancestor/descendant traversal at genealogy scale; JPA on Spring Data already demonstrates workable query patterns in this codebase. No evidence surfaced during inspection that either Node/Prisma or Postgres would materially outperform the current stack for this domain's realistic data volumes.
- **Deployment complexity**: the self-hosted-runner → `/srv` pipeline already exists and works for a Spring Boot artifact. Option A extends this pipeline to also build/deploy a Next.js process behind the same reverse proxy — additive, not a replacement of working CI/CD. Option B would require standing up an entirely new backend runtime (Node) alongside retiring the JVM one, increasing operational surface for a single maintainer.
- **Long-term maintainability**: one backend language (Java/Spring) and one frontend language (TypeScript) is simpler for a solo maintainer than three stacks in flight during a transition (JVM + Node API + Next.js), which Option B would produce temporarily and Option C avoids only by giving up SSR.
- **Why not Option C**: Vite/React without Next.js forgoes server rendering for the public marketing/history pages (Section 7 of the product brief), which benefits from fast first paint and basic SEO. Since Next.js can still host pure client components for the interactive tree (identical capability to a Vite SPA for that part of the product), choosing Option C would sacrifice SSR benefits for zero gain elsewhere.

## Consequences

- Two runtimes to deploy and operate (JVM API + Node/Next.js frontend) instead of one — accepted as a necessary cost of the frontend modernization goal.
- Requires introducing Flyway (replacing `ddl-auto=update`), OpenAPI tooling (springdoc-openapi), and Spring Session, none of which exist in the current `pom.xml`.
- Requires a reverse proxy (Nginx/Caddy) in front of both processes for same-origin cookie auth to work cleanly — not currently part of the deployment.
- Session-based auth (not JWT) means horizontal scaling of the API later would need a shared session store (Redis or DB-backed Spring Session) — acceptable now, revisit only if multi-instance scaling is ever actually needed.

## Alternatives Considered
See table above; full comparison also in `03-target-architecture.md`.

## Migration Impact
Backend: incremental (controllers converted from Thymeleaf-returning to REST-returning one feature area at a time; entities largely retained and extended). Frontend: full rewrite (expected and intended). Database: schema evolves in place via Flyway migrations; no database engine migration.
