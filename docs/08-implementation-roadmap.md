# 08 — Implementation Roadmap

Each phase ends with: run all relevant tests, update docs, summarize files changed, document risks/remaining work. No phase proceeds past a failed critical test without resolving or explicitly documenting it.

## Phase 0 — Discovery and Decisions (this delivery)
- Current-state assessment, product requirements, architecture ADR, data model, auth/verification design, UI/UX spec, migration plan, security threat model, testing strategy, this roadmap.
- **No code changes.** Status: complete as of this document set.
- Immediate independent fix flagged (not part of the rewrite, should happen regardless): correct `spring.profiles.default=dev` so production explicitly runs under `SPRING_PROFILES_ACTIVE=prod`.

## Phase 1 — Foundations
- Introduce Flyway; snapshot current schema as the baseline migration; disable `ddl-auto`.
- Stand up a staging environment (does not exist today).
- Add DB-level uniqueness constraint on `(person_id, related_person_id, relationship_type)` (after duplicate-detection pass from `07-migration-plan.md`).
- Add cycle-detection check to relationship writes.
- Introduce `UserAccount`, `Role`, `Permission`, `UserPersonLink` alongside (not replacing) `AppUser` initially, to allow incremental cutover.
- Design system tokens + component library skeleton (Next.js project scaffolding, TypeScript strict mode).
- OpenAPI wiring on the Spring Boot side (springdoc-openapi); generate first typed client.
- CI pipeline: build+test both Spring Boot and Next.js, dependency scanning, static analysis.
- Observability baseline: structured logging, request/correlation IDs.

## Phase 2 — Public Experience
- Next.js landing page, family history, about, membership info, contact, privacy/terms — server components, admin-editable content backed by `HistoricalArticle`.
- Internal CMS: draft/review/publish workflow for these sections (bilingual).
- Public statistics endpoint (aggregate-only, no PII).

## Phase 3 — Signup and Verification
- Signup flow (multi-step per `06-ui-ux-specification.md`), AD/BS conversion (client + server), email verification, family-match engine (`VerificationRequest`, confidence scoring per `05-auth-and-verification.md`), admin review UI, applicant notifications.
- Security tests for enumeration resistance, rate limiting, bot protection.
- **Decision checkpoint with user**: HIGH-confidence auto-approve policy (see open question in `05-auth-and-verification.md`).

## Phase 4 — Member Dashboard and Profiles
- Dashboard, profile pages, family snapshot, search (indexed normalized-name search replacing `LIKE` scans), person detail pages, privacy controls, correction-request workflow (`ChangeRequest`).

## Phase 5 — Genealogy Experiences
- Your Family (visual tree, generation list, ancestor/descendant views, relationship-path calculator).
- Whole Banshawali (React Flow + Dagre/ELK.js, lazy-loaded by generation/branch, minimap, filters, deep links).
- Performance benchmarks at 500/2,000/(10,000 if plausible) synthetic people before sign-off.

## Phase 6 — Administration and Data Quality
- People/relationship management UI, duplicate detection + guided merge (never automatic), change-request review queue, audit log viewer, content management, role management, data-quality reports (missing parents, cycles, unlinked accounts, incomplete dates).

## Phase 7 — Production Hardening
- Load testing against realistic dataset sizes.
- Security review (see `09-security-threat-model.md` test-coverage column).
- Accessibility review (WCAG 2.2 AA).
- Backup/restore drill, migration rehearsal against a full staging copy.
- Deployment runbook update, production readiness checklist, rollback plan confirmed.

## Sequencing Notes

- The schema migration in `07-migration-plan.md` is embedded across Phases 1–3, not a separate phase — it must land before Phase 5 (genealogy experiences) depend on the new `Relationship`/materialized-ancestry structures, but can proceed incrementally alongside Phase 1–2 UI work since those phases don't depend on the full genealogy model.
- Phase 3 (signup/verification) is the first phase requiring the full `UserAccount`/`UserPersonLink`/`VerificationRequest` model to exist — treat it as the hard gate for "is the new user/person separation actually done."
