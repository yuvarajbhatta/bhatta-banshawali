# 08 — Implementation Roadmap

Each phase ends with: run all relevant tests, update docs, summarize files changed, document risks/remaining work. No phase proceeds past a failed critical test without resolving or explicitly documenting it.

> **Status legend**: ✅ done · 🟡 partial / gap noted · ⬜ not started.
> Last audited against the codebase 2026-07-30 (commit `e135a75`). Re-check before trusting this if much time has passed — status was inferred from code/CI presence, not a phase-closeout report.

## Phase 0 — Discovery and Decisions (this delivery) ✅
- Current-state assessment, product requirements, architecture ADR, data model, auth/verification design, UI/UX spec, migration plan, security threat model, testing strategy, this roadmap.
- **No code changes.** Status: complete as of this document set.
- Immediate independent fix flagged (not part of the rewrite, should happen regardless): correct `spring.profiles.default=dev` so production explicitly runs under `SPRING_PROFILES_ACTIVE=prod`.

## Phase 1 — Foundations 🟡
- ✅ Flyway introduced; baseline schema snapshotted (`V1__baseline_schema.sql`), 12 migrations landed to date.
- ✅ **Staging environment — resolved as intentionally not built (2026-07-30).** This is a single shared home-server host running several other live personal apps (nextcloud, meropasal-pos, trading-copilot, etc.) via systemd + one shared cloudflared tunnel + shared MySQL; a parallel always-on environment would add real risk to that shared host for a benefit this project doesn't need. Migration dry-runs are instead done on demand against a disposable database — see `scripts/migration-rehearsal.sh` and `07-migration-plan.md`. This also resolves the Phase 7 "migration rehearsal" dependency below.
- ✅ DB-level uniqueness constraint on `(person_id, related_person_id, relationship_type)` (`V2__relationship_uniqueness.sql`).
- ✅ Cycle-detection check on relationship writes.
- ✅ `UserAccount`, `Role`, `Permission`, `UserPersonLink` introduced (`V3`, `V4`).
- ✅ Next.js scaffolding, design system, TypeScript strict mode.
- 🟡 OpenAPI/typed client — not verified in this pass, revisit if it matters.
- ✅ CI pipeline builds+tests both Spring Boot and Next.js and lints the frontend (`.github/workflows/ci.yml`). Dependency scanning was already covered by `.github/dependabot.yml` (maven, npm, github-actions — this had been misread as missing in the earlier audit pass). Static analysis was genuinely missing; added `.github/workflows/codeql.yml` (CodeQL for `java-kotlin` + `javascript-typescript`, on push/PR to main/develop plus a weekly scheduled scan). Both are free since the repo is public.
- 🟡 Observability baseline — not verified in this pass.

## Phase 2 — Public Experience ✅
- ✅ Next.js public pages: landing, history, about, membership, contact, privacy, terms.
- ✅ Admin-editable content (`HistoricalArticle`), admin content editor shipped (`601b194`).
- ✅ Public statistics section on the landing page.
- Note: draft/review/publish *workflow* (vs. direct admin edit) not confirmed as a distinct state machine — likely simpler than originally specced, worth confirming if bilingual review gating matters.

## Phase 3 — Signup and Verification 🟡
- ✅ Multi-step signup flow, AD/BS date conversion, email verification, family-match engine with confidence scoring, admin review UI (`admin/signups`), rate limiting on signup/login keyed on `CF-Connecting-IP`.
- 🟡 Security tests for enumeration resistance/bot protection — rate limiting is in; not confirmed whether dedicated enumeration/bot-protection tests exist.
- ✅ **Decision checkpoint resolved (2026-07-30)**: no auto-approve, ever, at any confidence level — manual admin review for every signup, permanently. Matches current `SignupService` behavior (always `PENDING`), so no code change was needed; docs updated in `05-auth-and-verification.md` to remove the "configurable auto-approve" language and record the decision as final.

## Phase 4 — Member Dashboard and Profiles ✅
- ✅ Dashboard, profile pages, person detail pages, directory/search, privacy redaction, correction-request workflow (`admin/corrections`).

## Phase 5 — Genealogy Experiences 🟡
- ✅ Your Family page (ancestors, descendants, relationship-path finder).
- ✅ Whole Banshawali tree (`/tree`) with the forest-green redesign.
- ⬜ **Performance benchmarks not done**: no benchmark scripts/tests found for 500/2,000/10,000-person synthetic datasets. This was a sign-off gate for the phase and should be treated as outstanding.

## Phase 6 — Administration and Data Quality 🟡
- ✅ People/relationship admin CRUD, audit log viewer, role/account management, unlinked-account fixer, signup and correction review queues, content management.
- ⬜ **Duplicate detection + guided merge for people — not built.** Only the relationship-level DB uniqueness constraint exists (Phase 1); there's no person-duplicate-candidate UI or merge tool.
- ⬜ **Data-quality reports — not built.** No missing-parents/cycles/unlinked-accounts/incomplete-dates reporting found.

## Phase 7 — Production Hardening ⬜
- Nothing in this phase appears to have started:
  - ⬜ Load testing against realistic dataset sizes.
  - ⬜ Formal security review pass against `09-security-threat-model.md`'s test-coverage column.
  - ⬜ Accessibility review (WCAG 2.2 AA) — no axe-core/a11y tooling found in the frontend.
  - ⬜ Backup/restore drill — no host-level backup scripts exist yet (`/srv/scripts/backup/` is empty), for this app or any other on the host. Out of scope for the familytree repo alone; flagging since Phase 7 depends on it.
  - ✅ Migration rehearsal — no longer blocked on a staging environment; `scripts/migration-rehearsal.sh` covers this (see Phase 1).
  - ⬜ Deployment runbook update, production readiness checklist, rollback plan.

## Overall
Phases 0, 1, 2, 3, and 4 are now done. Phase 3's open policy question is resolved (manual review only, permanently). Phase 1's staging-environment question is resolved (intentionally not built; disposable-database rehearsal instead) and its CI static-analysis gap is closed (CodeQL added; Dependabot was already there). Phases 5 and 6 are substantially built but still have named gaps (perf benchmarks; duplicate-merge + data-quality reports, respectively). Phase 7 hasn't started, and its backup/restore drill is blocked on host-level backup tooling that doesn't exist yet for *any* app on this box — that's a bigger, host-wide task outside this repo's scope. Best next candidates: Phase 6 (duplicate detection + data-quality reports, pure engineering) or Phase 5 (performance benchmarks).

## Sequencing Notes

- The schema migration in `07-migration-plan.md` is embedded across Phases 1–3, not a separate phase — it must land before Phase 5 (genealogy experiences) depend on the new `Relationship`/materialized-ancestry structures, but can proceed incrementally alongside Phase 1–2 UI work since those phases don't depend on the full genealogy model.
- Phase 3 (signup/verification) is the first phase requiring the full `UserAccount`/`UserPersonLink`/`VerificationRequest` model to exist — treat it as the hard gate for "is the new user/person separation actually done."
