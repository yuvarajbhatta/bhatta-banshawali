# 10 — Testing Strategy

## Baseline

The current test suite (1,626 lines across controller/service/repository/config tests, JUnit + Mockito + H2) is a real asset and the regression baseline for the migration — see `01-current-system-assessment.md`. New tests extend this pattern rather than replacing the testing approach wholesale.

## Backend (Spring Boot)

- **Unit tests**: service-layer business rules in isolation (Mockito), including the existing auto-linking logic (`saveRelationshipWithAutoLinks`) and its extension to new relationship types.
- **Repository tests**: `@DataJpaTest`-style tests against H2 (as today) plus a dedicated MySQL-compatibility test pass before production cutover, since some query behaviors (e.g., `WITH RECURSIVE`) may not be identical between H2 and MySQL.
- **API integration tests**: full request/response cycle per endpoint, asserting correct DTO shape (never entity leakage), correct status codes, and consistent validation-error format.
- **Authorization tests**: a full role × privacy-classification × endpoint matrix — not spot checks — given that broken authorization (IDOR) is the highest-impact realistic threat identified in `09-security-threat-model.md`.
- **Migration tests**: Flyway migration scripts tested against a representative data snapshot, including a dry run against production-shaped data before real cutover — via the disposable-database rehearsal in `scripts/migration-rehearsal.sh` (see `07-migration-plan.md`; no persistent staging environment exists or is planned, per `03-target-architecture.md`).
- **Recursive genealogy query tests**: ancestor/descendant traversal correctness across multiple marriages, adoption, step-relationships, unknown parents, and multi-branch trees — using realistic fixture trees (see Fixtures below), not only trivial three-person cases.
- **Cycle-detection tests**: explicit tests asserting a `PARENT_OF` write that would create a cycle is rejected, and that pre-existing cycles (if discovered during migration) are surfaced by the data-quality report rather than silently accepted.
- **Duplicate-resolution tests**: merge logic correctly re-points `SourceCitation`, `AuditEvent`, and `ChangeRequest` references and never hard-deletes the losing record's history.
- **Date conversion tests**: AD↔BS conversion fixtures covering known reference dates, leap years (both Gregorian and Bikram Sambat leap/variable-month-length years), month boundaries, and timezone edge cases — run against whichever vetted calendar library is selected in Phase 1, never against ad hoc arithmetic.
- **Signup matching tests**: confidence-scoring tests across HIGH/MEDIUM/LOW scenarios, including deliberately adversarial inputs (false lineage claims, transliteration edge cases, common-name collisions).
- **Audit tests**: every sensitive action (role change, merge, privacy-classification change, user-person link) produces exactly the expected `AuditEvent`.

## Frontend (Next.js)

- **Component tests**: design-system components (buttons, forms, cards, tree nodes) in isolation.
- **Form validation tests**: signup multi-step form, including live AD/BS sync behavior and error states.
- **Localization tests**: every screen rendered in both English and Nepali, asserting no missing keys and no layout breakage from longer Nepali strings.
- **Accessibility tests**: automated (axe or equivalent) plus manual keyboard-navigation pass on the tree view and forms.
- **Tree interaction tests**: pan/zoom/expand/collapse/search-and-jump behavior in the Whole Banshawali view, including lazy-load boundary conditions.
- **Responsive-layout tests**: mobile/tablet/desktop breakpoints, with Nepali labels active (not just English) given their greater width.
- **Error-state tests**: network failure, empty dataset, and partial-load scenarios for the tree and search views.

## End-to-End

Covering, at minimum, the scenarios listed in the product brief:
1. Visitor views homepage.
2. Applicant signs up.
3. AD/BS date converts correctly.
4. Email is verified.
5. HIGH/MEDIUM/LOW family-match outcomes each produce correct downstream behavior (auto-approve-or-confirm, admin review queue, neutral applicant messaging).
6. Administrator approves a pending user.
7. User logs in.
8. User opens "Your Family."
9. User opens "Whole Banshawali."
10. User searches and centers the tree on a person.
11. User submits a correction.
12. Administrator approves the correction.
13. Unauthorized user is blocked from private data (explicit negative test, not just a positive-path check).
14. Password reset works securely (token expiry, single-use enforcement).
15. English/Nepali switching persists across navigation and reload.

## Fixtures

Realistic, multi-generation fixture trees are required — not only trivial three-person trees — specifically covering: multiple marriages/remarriage, adoption, step-relationships, at least one branch with an unknown parent, a mix of exact/partial/approximate dates, at least one deceased-member scenario, and (for migration-specific tests) a fixture shaped like the actual legacy schema (flat `firstName`/`firstNameNepali` columns, `FATHER`/`MOTHER`/`SPOUSE`/`CHILD` types) to validate the migration scripts against something structurally identical to production, not just the target schema.

## What "Critical Genealogy Invariant" Means Here (tested explicitly, not incidentally)

- No person is their own ancestor.
- No duplicate identical relationship edges exist.
- Every displayed relationship label (sibling, grandparent, cousin, aunt/uncle) is correctly derived from stored parent/spouse edges, not stored redundantly and inconsistently.
- No API response ever includes a field the requester's role/privacy classification doesn't permit.
- No genealogy fact changes without a `ChangeRequest` record.

## Gating

Per `08-implementation-roadmap.md`, each phase's exit criteria include all relevant tests above passing; a failed critical test (particularly authorization or cycle-detection tests) blocks progression to the next phase until resolved or explicitly documented as an accepted, time-boxed risk.
