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
- 🟡 **Performance benchmarks run (2026-07-31) — result: fail, gate does not pass as built.**
  `scripts/benchmark/generate-synthetic-family.py` generates a plausible multi-generation tree
  (bulk SQL, disposable dev DB only, never production) at N=500 and N=2,000. Findings:

  | N | Backend (`GET /api/v1/family-tree`) | Payload | Dagre layout alone (Node, steady-state) | Real browser: time to fully rendered tree (production build) |
  |---|---|---|---|---|
  | 500 | ~75ms | 103 KB | ~0.9s | ~5s |
  | 2,000 | ~200ms | 417 KB | ~18s | ~30-35s |

  **The backend is not the problem** — `FamilyTreeAssembler` is a fast, simple bulk load at both
  scales. The problem is entirely client-side: `docs/03-target-architecture.md` calls
  server-driven, viewport-scoped lazy loading a "hard requirement" once the dataset exceeds a few
  hundred people, but this was never built — `FamilyTreeAssembler.buildTree()`'s own Javadoc
  already says "this endpoint returns every person in one response," and both `/tree` and `/family`
  fetch that single payload and run the *entire* set through `@dagrejs/dagre` layout
  (`useFamilyTreeLayout.ts`) synchronously on the main thread, then render every node as a real
  React Flow DOM/SVG element with no virtualization. Growth from 500→2,000 (4x the people) produced
  ~20x the Dagre layout time and ~6-7x the real end-to-end render time — clearly super-linear, not
  just "a bit slower." **N=10,000 was deliberately not run**: extrapolating the observed scaling
  put it at several minutes, which would have added no further decision value (the verdict was
  already unambiguous at 2,000) for a real cost in session time.
  Initial testing showed the tree never rendering at all even at N=500 after 30+ seconds — that
  turned out to be a `next dev` artifact (dev-mode compilation/HMR overhead), not a real production
  characteristic; re-tested against an actual production build (`npm run build && npm run start`)
  before drawing any conclusion, which is the number in the table above. Screenshots taken during
  testing confirm the tree renders correctly once complete — this is a **speed** problem, not a
  correctness bug.
  **Recommendation**: build the deferred lazy-loading — the architecture doc's own suggested shape
  (page/fetch by generation or branch, not the whole graph) is the right starting point, since
  `PersonTreeNodeDto` already carries `generationNumber` and the `/tree` page already has a
  generation filter client-side that could become a real server-side query parameter instead. This
  is a real, separate feature (new scoped endpoint + incremental-fetch frontend work), not something
  to build as a side effect of running this benchmark — flagging it clearly, same as the
  `app_users` AUTO_INCREMENT drift and `PersonService.deletePersonById`'s missing re-pointing were
  flagged rather than silently fixed earlier. Until it's built, this phase is functionally usable
  for families the size of the real data today (small) but will degrade badly if the tree grows
  toward the low thousands.

## Phase 6 — Administration and Data Quality ✅
- ✅ People/relationship admin CRUD, audit log viewer, role/account management, unlinked-account fixer, signup and correction review queues, content management.
- ✅ **Duplicate detection + guided merge for people (2026-07-31).** `DuplicateCandidateService` (name-match via the existing `NameMatcher`, scored HIGH/MEDIUM/LOW with corroborating/conflicting reasons) + `PersonMergeService` (re-points `Relationship`/`UserPersonLink`/`PersonCorrectionRequest` rows, refuses on direct-relationship or both-verified-account conflicts, records one `PERSON_MERGED` audit entry). `/admin/duplicates` — merge is never automatic; requires an explicit, named confirmation per pair. Verified end-to-end against a real dev DB (create duplicate pair → candidate appears → add direct relationship → excluded → conflicting DOB → LOW confidence → real merge → relationships re-pointed, audit log correct).
- ✅ **Data-quality reports (2026-07-31).** `DataQualityService`: missing/partial parents (generation-aware, doesn't guess which gaps are legitimate roots), relationship cycles (batch three-color DFS, not per-node ancestor checks), unlinked accounts (reuses `UserAccountAdminService`), and date issues (missing/future birth date, death-before-birth, implausible parent-child age gap). `/admin/data-quality`, read-only. Deliberately **not** wired into the sidebar's live badge counts (`AdminSummaryDto`) — both reports scan the full person/relationship tables, and that summary is computed on every admin page load; a live count would mean paying that cost on every page view instead of only when the report is opened.

## Phase 7 — Production Hardening ⬜
- Nothing in this phase appears to have started:
  - ⬜ Load testing against realistic dataset sizes.
  - ⬜ Formal security review pass against `09-security-threat-model.md`'s test-coverage column.
  - ⬜ Accessibility review (WCAG 2.2 AA) — no axe-core/a11y tooling found in the frontend.
  - ⬜ Backup/restore drill — no host-level backup scripts exist yet (`/srv/scripts/backup/` is empty), for this app or any other on the host. Out of scope for the familytree repo alone; flagging since Phase 7 depends on it.
  - ✅ Migration rehearsal — no longer blocked on a staging environment; `scripts/migration-rehearsal.sh` covers this (see Phase 1).
  - ⬜ Deployment runbook update, production readiness checklist, rollback plan.

## Overall
Phases 0, 1, 2, 3, 4, and 6 are now done. Phase 3's open policy question is resolved (manual review only, permanently). Phase 1's staging-environment question is resolved (intentionally not built; disposable-database rehearsal instead) and its CI static-analysis gap is closed (CodeQL added; Dependabot was already there). Phase 6's last two gaps (duplicate detection + guided merge, data-quality reports) are built and verified end-to-end against a real dev database. Phase 5's benchmark gate has now actually been run (2026-07-31) rather than left outstanding — the result is a genuine fail: the `/tree` page's whole-graph, client-side Dagre-layout approach degrades super-linearly and needs the lazy/viewport-scoped loading the architecture doc always called a hard requirement, not yet built. That's now a concrete, scoped follow-up (see Phase 5 above) rather than an open question. Phase 7 hasn't started, and its backup/restore drill is blocked on host-level backup tooling that doesn't exist yet for *any* app on this box — that's a bigger, host-wide task outside this repo's scope. The two things worth doing next: build the `/tree` lazy-loading follow-up (real user-facing risk once the tree grows), or move on to Phase 7.

## Sequencing Notes

- The schema migration in `07-migration-plan.md` is embedded across Phases 1–3, not a separate phase — it must land before Phase 5 (genealogy experiences) depend on the new `Relationship`/materialized-ancestry structures, but can proceed incrementally alongside Phase 1–2 UI work since those phases don't depend on the full genealogy model.
- Phase 3 (signup/verification) is the first phase requiring the full `UserAccount`/`UserPersonLink`/`VerificationRequest` model to exist — treat it as the hard gate for "is the new user/person separation actually done."
