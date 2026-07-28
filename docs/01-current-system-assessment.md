# 01 — Current System Assessment

## Snapshot

| Aspect | Current State |
|---|---|
| Framework | Spring Boot 4.0.5, Java 21 |
| View layer | Thymeleaf (server-rendered HTML), vanilla JS, hand-written CSS |
| Database | MySQL (`spring.jpa.hibernate.ddl-auto=update` — schema drifts implicitly, no migration tool) |
| Auth | Spring Security form login, 2 roles (`ROLE_ADMIN`, `ROLE_USER`), `DelegatingPasswordEncoder` (bcrypt) |
| i18n | English + Nepali, session-based locale (`?lang=`), 221 message keys per locale, custom transliteration service |
| Observability | Actuator `/health` + `/prometheus`, Prometheus scrape restricted to loopback |
| Deployment | Self-hosted GitHub Actions runner → shell script at `/srv/scripts/deploy/familytree-deploy.sh`, Docker image via multi-stage-less `Dockerfile` (builds inside the image) |
| Tests | 1,626 lines across controller, service, repository, and config tests (JUnit + Mockito + H2) |
| Maintainer | Single developer (`@yuvarajbhatta`, sole CODEOWNER) |

## What Currently Works

1. **Person and Relationship CRUD** — `PersonController`/`RelationshipController` provide full create/edit/delete with server-side validation and duplicate-relationship rejection.
2. **Auto-linking of inverse relationships** — `RelationshipService.saveRelationshipWithAutoLinks` automatically creates the inverse `CHILD` edge when a `FATHER`/`MOTHER` edge is added, and infers spousal relationships between two parents of the same child. This is a genuine, non-trivial business rule already encoded and tested.
3. **Recursive lineage tree** — `RelationshipService.buildLineageTree` pre-loads all `CHILD` relationships and all persons into in-memory maps, then recursively assembles a JSON tree keyed by root ancestor (`getRootPersonForLineage`: a person with outgoing `CHILD` edges but no incoming `CHILD` edge naming them as the related person). This avoids N+1 queries at read time by design.
4. **Bilingual name display** — `PersonDisplayHelper` and inline Thymeleaf logic resolve locale-aware full names, falling back to English when a Nepali name is absent.
5. **Custom Nepali transliteration** (`NameTransliterationService`) — a digraph/letter-substitution engine (not phonetic-library-backed) used to backfill missing Nepali names from English input, with a cleanup pass for known artifacts (e.g., `"Bhatta"` → `"भट्ट"`).
6. **Locale-aware validation messages** via `LocalValidatorFactoryBean` bound to the Spring `MessageSource`.
7. **CSRF protection** is present and actively used by the lineage-editing JS (`getCsrfHeaders()` reads the Thymeleaf-rendered CSRF meta tags).

## What Can Be Reused

- The `Person`/`Relationship`/`RelationshipType` shape as the *conceptual* seed for a normalized data model (Section 04) — the FATHER/MOTHER/SPOUSE/CHILD edge model with derived siblings/grandparents is directionally correct; it needs to be extended, not replaced.
- The recursive tree-building algorithm's approach (pre-load, then recurse in memory) — the same pattern generalizes well to a materialized-path or closure-table read model.
- The bilingual infrastructure (`messages*.properties`, `LocaleConfig`, `PersonDisplayHelper`) — this is a working foundation for the i18n requirements in Section 15 of the product brief, not something to rebuild from scratch.
- The transliteration service as a *fallback suggestion* tool (already used this way — it never overwrites an explicit Nepali value; see `suggestNepaliValue`), consistent with the requirement to never overwrite manually verified spelling.
- The Actuator/Prometheus wiring and the self-hosted deploy pipeline shape (once profile handling is fixed).
- The existing test suite's assertions, as a regression baseline while the schema and controllers change underneath them.

## What Should Be Refactored

- **`AppUser`** needs to gain an email, verification status, and a link to `Person`, without losing the working password-hashing and role-checking behavior.
- **`RelationshipType`** needs additional values (adoptive, step, guardian) and a uniqueness constraint at the database level, not only at the application level.
- **Tree transport** (`/lineage/tree` JSON endpoint) is a reasonable shape to keep as an API contract concept, but the admin-editing UX in `lineage.js` (prompt()/confirm()-based CRUD, inline `contenteditable`) should move to a proper form-based editor.
- **Startup-time data mutation** (`NepaliNameBackfillInitializer`, `AdminUserInitializer` running as `CommandLineRunner`s) should become explicit, one-time, auditable migration scripts, not code that runs — and re-checks — on every deployment.

## What Should Be Replaced

- Username+password-only signup with no verification (`SignupForm`, `AuthController.registerUser`) — replaced by the full signup/verification workflow (Section 05).
- Two-role, all-or-nothing authorization (`SecurityConfig`) — replaced by the six-role model with field-level privacy classification.
- Thymeleaf templates and hand-rolled tree JS — replaced by Next.js/React + React Flow.
- `spring.profiles.default=dev` as the effective production profile (see Data and Migration Risks below) — replaced by an explicit `prod` profile activated via `SPRING_PROFILES_ACTIVE`.

## Data and Migration Risks

- **No `UserAccount`↔`Person` link exists today.** Every existing `AppUser` row is an island; associating existing users with their correct `Person` record cannot be fully automated and will require an admin reconciliation pass during migration (see `07-migration-plan.md`).
- **No DB-level uniqueness constraint** on `(person_id, related_person_id, relationship_type)` — only an application-level `existsBy` check gates writes. A direct DB edit, a race condition, or a bug could already have produced duplicate edges; must be audited before adding a real constraint.
- **No cycle detection** anywhere in `RelationshipService` — a data-entry error could make a person their own ancestor. The recursive tree builder does not currently guard against this and would either loop or stack-overflow if it occurred. This must be checked before any schema constraint is added and must be enforced going forward.
- **Schema managed by `ddl-auto=update`**, not versioned migrations (no Flyway/Liquibase) — the actual current production schema may already differ subtly from what the entities declare, depending on manual interventions. The first migration step must be to snapshot and diff the live schema against the entity classes.
- **The production profile ambiguity** (`application.properties` line 10's comment: prod runs under the `dev` default profile, with true prod values supplied by an external, out-of-repo config directory) is an active operational risk independent of the redesign — it means the true production configuration is not fully visible in this repository and must be inventoried directly from the production host before any cutover.
- **No seed/reference SQL is checked into the repository.** All genealogy data lives only in the live MySQL instance; there is no committed snapshot to develop or test migrations against. A read-only export must be taken before schema work begins.

## Hidden Business Rules Discovered

1. Adding a `FATHER` or `MOTHER` relationship auto-creates the inverse `CHILD` edge **and** auto-links a `SPOUSE` relationship between that parent and any other parent already recorded for the same child (`autoCreateSpouseBetweenParents`).
2. The root of the lineage tree is inferred, not stored: it is whichever person has outgoing `CHILD` edges but no relationship naming them as someone else's child (`getRootPersonForLineage`). This is a heuristic, not a flag, and breaks if more than one such person exists (multiple disconnected trees) — `.findFirst()` silently picks one arbitrarily.
3. Nepali name backfill only ever fills a *blank* Nepali field from a transliteration of the English name, and only *clears* a Nepali field if it exactly matches what the transliterator would currently generate (`clearAutogeneratedNepaliNames`) — this is a deliberate mechanism to avoid ever overwriting a human-entered Nepali spelling, and must be preserved in spirit in the new system.
4. Search (`PersonRepository.searchPersons`) matches free text against six name fields plus notes, and separately treats the query as a possible generation-number filter if it parses as an integer.

## Risks a Rewrite Could Introduce

- Losing the “never overwrite a verified Nepali spelling” guarantee if the transliteration/backfill logic is reimplemented carelessly.
- Losing the inverse-relationship auto-linking behavior, which the current UI silently depends on for the tree to render correctly (a raw one-directional edge model would double the manual entry work for admins).
- Breaking the single arbitrary-root heuristic without providing a replacement (an explicit, admin-designated root ancestor per branch) before cutover.
- Underestimating the manual/admin effort needed to link legacy `AppUser` rows to `Person` rows, since no automated signal exists in the current schema to do this reliably.
