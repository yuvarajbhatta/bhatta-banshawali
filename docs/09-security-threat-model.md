# 09 — Security Threat Model

For each threat: attack scenario, risk, preventive control, detective control, recovery approach, test coverage.

## 1. Credential Stuffing / Brute Force
- **Scenario**: automated login attempts against known/guessed email+password pairs.
- **Risk**: account takeover, exposure of private family data.
- **Preventive**: adaptive lockout/throttling per account and per IP, bot protection (CAPTCHA) on `/login`, rate limiting at the reverse proxy.
- **Detective**: `AuditEvent` on repeated failed logins; alerting on threshold breach.
- **Recovery**: forced password reset + session revocation for affected accounts.
- **Test coverage**: authentication integration tests asserting lockout triggers after N failures; rate-limit tests.
- **Ground truth (2026-07-31)**: partially built. Only IP-based throttling exists (`RateLimitFilter`, well-tested for what it is — 10 logins/15min/IP). No per-account lockout field/table exists anywhere, and no CAPTCHA/bot protection exists at all. Not fixed this pass — a small, invitation-oriented family site was judged not to need per-account lockout or CAPTCHA urgently, but this remains a real, open gap, not a false claim corrected to true.

## 2. Account Enumeration
- **Scenario**: probing signup/login/reset endpoints to infer whether an email is registered.
- **Risk**: reveals family membership indirectly.
- **Preventive**: identical response shape/timing regardless of account existence, across login and password-reset. Signup is a deliberate, accepted exception — it returns an explicit "email already exists" (HTTP 409) error, because on this small, invitation-oriented family site the UX cost of a vague signup error (people re-submitting, assuming the form is broken, or emailing the admin) was judged to outweigh the enumeration risk. See `EmailAlreadyRegisteredException`'s javadoc and docs/05's "Anti-Enumeration Guarantees" section.
- **Detective**: monitoring for high-volume distinct-email probing from a single source.
- **Recovery**: N/A (informational leak, not account compromise) — rate-limit and block source.
- **Test coverage**: explicit tests asserting identical response bodies/status codes for existing vs. non-existing emails on login/reset; explicit tests asserting a 409 with a clear message on signup with a duplicate email.
- **Ground truth (2026-07-31)**: password reset is confirmed **not built at all** (no controller, service, token entity, or email flow anywhere) — that half of this item is N/A, not a gap. The login half's mechanism is real (`BridgingUserDetailsService` throws one generic exception regardless of why an account can't log in) but genuinely untested for the specific enumeration-resistance property (no test compares a wrong-password-on-existing-account response against a nonexistent-account response). Not fixed this pass; the signup 409 exception is real and already tested (`SignupControllerTest.returnsConflictWhenEmailAlreadyRegistered`).

## 3. Family-Member Enumeration via Signup Matching
- **Scenario**: an attacker submits guessed father/grandfather names during signup to learn whether specific people exist in the private tree.
- **Risk**: leaks private genealogy structure through the public signup form.
- **Preventive**: neutral pending-status response regardless of match confidence (HIGH/MEDIUM/LOW all look identical to the applicant); match evidence never returned to the applicant, only to admins.
- **Detective**: rate-limiting + logging of repeated signup attempts with varying father/grandfather combinations from the same source.
- **Recovery**: block source, review any admin-visible match evidence for signs of targeted probing.
- **Test coverage**: signup-matching tests asserting API/UI responses are indistinguishable across confidence levels; abuse-pattern tests.
- **Ground truth, fixed (2026-07-31)**: the guarantee already held structurally (`submitSignup` returns `void`, so there's no code path for match data to reach the response) but was untested. Added `SignupConfidenceNeutralResponseTest`, which uses the real (unmocked) `SignupService`/`FamilyMatchService` with real `Person`/`Relationship` fixtures engineered to produce genuine HIGH, MEDIUM, and LOW confidence, and asserts the HTTP response is byte-identical across all three.

## 4. Unauthorized Access to Private Family Data (IDOR / Broken Authorization)
- **Scenario**: a Verified Member requests `/api/v1/people/{id}` for a person outside their permitted privacy scope by guessing/incrementing IDs.
- **Risk**: exposure of privacy-classified data (living members, disputed facts) to unauthorized viewers.
- **Preventive**: centralized, per-request server-side authorization check against `PrivacyPreference` classification and the requester's role/relationship to the target — never client-side filtering alone; DTOs never expose fields the viewer isn't authorized to see (not returned-then-hidden).
- **Detective**: audit log of denied authorization attempts, especially repeated sequential ID probing.
- **Recovery**: N/A if preventive control holds; incident review of audit logs if a gap is found.
- **Test coverage**: authorization tests for every role × every privacy classification × every endpoint (matrix testing, not spot checks).
- **Ground truth (2026-07-31)**: the real model is much narrower than described — there is no `PrivacyPreference` entity or per-field/per-person classification; `PersonProfileAssembler` redacts exactly two fields (`birthDate`, `currentAddress`) for non-admin/non-self viewers, and that narrow rule is genuinely well-tested (`PersonProfileAssemblerTest`). The broader claim ("full role × classification × endpoint matrix") is false — only two endpoints (`/api/v1/admin/summary`, `/admin/signups`) have real `@SpringBootTest`+`MockMvc` role-based security tests; this pass added two more (`POST /persons`, `POST /api/v1/admin/admin-access-requests/{id}/approve`) for the specific gaps found, not a full matrix. The two-field redaction model itself is an accepted, known simplification from earlier phases, not something to redesign here.

## 5. False Identity Linkage
- **Scenario**: an applicant claims to be a specific existing `Person` (living or historical) they are not, to gain access to that person's connections/data.
- **Risk**: impersonation, exposure of a real family member's private data to an unrelated party.
- **Preventive**: `UserPersonLink` only created via the confidence-scored verification workflow with admin review for anything below HIGH; never created from client-asserted claims alone.
- **Detective**: audit trail on every `UserPersonLink` creation, admin-reviewable.
- **Recovery**: `UNLINKED` status (not deletion) on discovery, full audit trail preserved, affected person's data access reviewed.
- **Test coverage**: verification-matching tests including deliberately adversarial/incorrect lineage claims.
- **Ground truth (2026-07-31)**: real, unaddressed gap found and left open. `VerificationReviewService.approve()` has no confidence-score gating in code at all — `matchConfidence` is stored/displayed to the admin but never checked/enforced. More importantly, `UserAccountAdminService.link()` (the admin "Manage User Accounts" tool) lets any admin create a `VERIFIED` `UserPersonLink` between any account and any person directly, with zero confidence check — bypassing the verification workflow entirely. This wasn't fixed this pass (it wasn't in the fix list scoped from the audit); it's arguably consistent with this app's existing broad-admin-trust model (admins can already directly edit any relationship, merge duplicates, etc. without a formal check), but that's a judgment call worth revisiting deliberately, not an accepted-by-default gap. No adversarial test exists either.

## 6. XSS
- **Scenario**: stored data (biography, notes, history article content) rendered without escaping.
- **Risk**: session/cookie theft, action-on-behalf-of-victim.
- **Preventive**: React's default escaping (Next.js) for all rendered user content; strict CSP; rich-text content (history articles) sanitized server-side with an allowlist-based sanitizer, never raw HTML passthrough.
- **Detective**: CSP violation reporting.
- **Recovery**: session revocation, content review/rollback via `HistoricalArticle` revision history.
- **Test coverage**: XSS payload tests against every free-text field (notes, biography, article body).
- **Ground truth, partially fixed (2026-07-31)**: no rich-text/HTML content model exists at all (article body is plain text with blank-line paragraph breaks, rendered via a plain JSX `<p>` in `Paragraphs.tsx` — no `dangerouslySetInnerHTML` anywhere in the frontend) so the "allowlist-sanitizer" this item describes was never built; safety here is incidental (plain text + React's default escaping), not the designed control, but no rich-text feature means no rich-text XSS surface either. **CSP was missing entirely** (not in `SecurityConfig`, `next.config.ts`, or the nginx vhost) — added this pass on both layers, but not as originally written: a first attempt used a static `script-src 'self'` on both sides, and a live click-through of the **production build** against a disposable dev stack caught that this broke the app outright on both layers before it ever reached prod. On the Next.js side, the App Router's own inline hydration/flight scripts are blocked by a bare `'self'`; fixed by moving the CSP into `proxy.ts` (middleware) with a fresh nonce per request, which Next.js applies to its own injected scripts automatically once the root layout reads it via `headers()`. On the Thymeleaf side, `lineage.html` and the `language-switch` fragment (included on nearly every legacy page) use inline `<script>` blocks and `onclick="..."` handlers throughout — refactoring all of those away was out of scope for a CSP-verification pass on code being phased out anyway, so `SecurityConfig`'s policy keeps `'unsafe-inline'` in `script-src` there, honestly reflecting that script-src isn't materially strengthened for the legacy pages while the other directives (`frame-ancestors`, `base-uri`, `form-action`, `connect-src`, `img-src`) still hold. No payload-injection test was added this pass (lower priority given there's no rich-text surface to attack) — a real gap still worth a cheap regression test later.

## 7. CSRF
- **Scenario**: cross-site request forgery against session-cookie-authenticated state-changing endpoints.
- **Risk**: unauthorized changes performed as a logged-in user.
- **Preventive**: CSRF token required on all state-changing requests (already present in the current app; must be preserved/extended for the new API), `SameSite` cookies.
- **Detective**: rejected-CSRF-token audit logging.
- **Recovery**: N/A if preventive control holds.
- **Test coverage**: tests asserting state-changing endpoints reject requests missing/mismatching the CSRF token.
- **Ground truth, fixed (2026-07-31)**: this control was already real and well-tested — only one exemption exists (`/api/v1/signup`, the documented, intentional exception), and `CsrfCookieFilterTest` already had genuine end-to-end tests for the missing-token case on a real mutating endpoint. The "mismatching" case specifically (token present but wrong) was untested — added `authenticatedPostWithAMismatchedXsrfTokenIsRejected` this pass. `SameSite` is still not explicitly configured for the CSRF cookie itself (only the session cookie was hardened, deliberately — see item 11 — to avoid risking the carefully-tuned, already-documented CSRF cookie behavior).

## 8. SQL Injection
- **Scenario**: crafted input into search or admin filters.
- **Risk**: data exfiltration/corruption.
- **Preventive**: JPA/parameterized queries exclusively (already the pattern in `PersonRepository`); no string-concatenated JPQL/SQL introduced during the rewrite.
- **Detective**: WAF/query-anomaly monitoring if available.
- **Recovery**: restore from backup if corruption occurs.
- **Test coverage**: repository tests with adversarial search-keyword inputs.
- **Ground truth, fixed (2026-07-31)**: the control was already architecturally sound (only two repositories use `@Query`, both exclusively `@Param`-bound, no native/concatenated SQL anywhere in the codebase) but genuinely untested against adversarial input. Added `searchPersonsIsSafeAgainstSqlInjectionInTheKeyword` (`PersonRepositoryTest`), asserting a `' OR '1'='1` keyword returns no unexpected rows.

## 9. Mass Assignment
- **Scenario**: a client sends extra fields (e.g., `role`, `verificationState`, `privacyClassification`) on a profile-update request, hoping the backend blindly binds them.
- **Risk**: privilege escalation, unauthorized data exposure change.
- **Preventive**: explicit per-use-case request DTOs (never binding directly to `Person`/`UserAccount` entities as `PersonController.savePerson(@Valid Person person, ...)` does today), allowlisted fields only.
- **Detective**: schema validation rejection logging for unexpected fields.
- **Recovery**: N/A if preventive control holds.
- **Test coverage**: API tests submitting extra/forbidden fields and asserting they have no effect.
- **Ground truth, fixed (2026-07-31)**: the exact anti-pattern this item warns about is still live in the legacy `PersonController.savePerson`/`updatePerson` (direct `@Valid Person person` entity binding) — deliberately not rewritten this pass (that's a bigger cleanup, not a security fix), but a **real, separate authorization gap** was found and fixed: `SecurityConfig`'s general `/persons` rule matched every HTTP method including `POST /persons` (create), so any authenticated non-admin member could create `Person` rows directly. Fixed by adding a more specific `POST /persons` → `ADMIN`-only matcher ahead of the general rule, with a new `PersonCreateSecurityTest` proving `ROLE_USER` gets 403. The newer `AdminPersonApiController`/`AdminPersonRequestDto` path (already admin-only, already DTO-based) matches the doc's intended target state as-is.

## 10. Malicious Image Upload
- **Scenario**: an uploaded "photo" is actually a malicious file (polyglot, embedded script, oversized/zip-bomb image).
- **Risk**: stored XSS, resource exhaustion, malware distribution.
- **Preventive**: strict MIME/content-type validation independent of file extension, image re-encoding (not passthrough), size limits, virus scan (`MediaAsset.virusScanStatus`), storage outside the web root/served via a locked-down path with no execute permissions.
- **Detective**: virus-scan failure logging and alerting.
- **Recovery**: quarantine/delete flagged assets, notify uploader.
- **Test coverage**: upload tests with adversarial file types/sizes.
- **Ground truth (2026-07-31)**: confirmed **not built at all**. No `MultipartFile`/upload endpoint exists anywhere; `Person.photoPath` is a plain, manually-typed string field, not an upload target. This entire item is N/A, same category as password reset — not a weakened control, a feature that doesn't exist.

## 11. Session Theft
- **Scenario**: session cookie stolen via XSS, network interception, or a shared/public device.
- **Risk**: account takeover.
- **Preventive**: httpOnly + `Secure` + `SameSite` cookies, TLS everywhere, "revoke all sessions" self-service capability, idle/absolute session timeouts.
- **Detective**: `AuditEvent` on login from a new device/IP pattern (informational, not blocking).
- **Recovery**: user-initiated or admin-initiated session revocation.
- **Test coverage**: cookie-attribute assertions in integration tests; session-timeout tests.
- **Ground truth, partially fixed (2026-07-31)**: none of this was configured — no explicit cookie attributes, no timeout, no "revoke all sessions" feature. Fixed the cookie/timeout half in `application-prod.properties` (`server.servlet.session.cookie.same-site=lax`, `.secure=true`, `server.servlet.session.timeout=30m`; prod-only since `secure=true` would break local HTTP dev). **Not built**: a "revoke all sessions" self-service capability — that's a real feature, not a config change, and stays flagged rather than built this pass. No new test added (config-only change; a real test would need a running server with actual HTTP cookies, not MockMvc, to observe the `Secure`/`SameSite` attributes).
  **Follow-up gap found during Fix 4's live verification, not yet resolved**: production actually runs with the Spring `dev` profile (`spring.profiles.default=dev` in `application.properties`, and the systemd env file at `/srv/config/familytree/familytree.env` sets no `SPRING_PROFILES_ACTIVE` override — confirmed by reading both), with real secrets supplied through an external `application.properties` override file instead of `application-prod.properties`. That means **the three settings above almost certainly never take effect in the live deployment** — `application-prod.properties` is dead code today. The honest fix requires either editing that external, git-ignored config and restarting the live service, or restructuring how profile-specific config is loaded; both are live-production changes outside the scope of a docs/test pass, so this stays flagged rather than silently patched.

## 12. Password-Reset Abuse
- **Scenario**: repeated reset requests for a target email (harassment/enumeration), or reset-token guessing.
- **Risk**: account lockout via spam, or token brute force.
- **Preventive**: rate-limited reset requests, cryptographically random single-use expiring tokens, token invalidated after use or after a new request supersedes it.
- **Detective**: audit logging of reset-request volume per account.
- **Recovery**: N/A if preventive control holds.
- **Test coverage**: token-expiry and single-use tests, rate-limit tests.
- **Ground truth (2026-07-31)**: confirmed **not built at all** — no password-reset controller, service, token entity, or email flow exists anywhere in the codebase. N/A, not a gap.

## 13. Admin Privilege Escalation
- **Scenario**: a Family Editor or compromised admin account attempts to grant itself Administrator/Super Administrator rights.
- **Risk**: full system compromise.
- **Preventive**: role changes require Super Administrator approval and are themselves a `ChangeRequest` subject to audit; no self-service role elevation endpoint exists at any layer.
- **Detective**: `AuditEvent` on every role change, alerted.
- **Recovery**: immediate role reversion + forced re-authentication + audit review.
- **Test coverage**: authorization tests confirming no non-Super-Admin path can alter roles.
- **Ground truth, fixed (2026-07-31) — this was the most serious finding of the review**: this claim was false in the code. `BridgingUserDetailsService` mapped both `ADMINISTRATOR` and `SUPER_ADMINISTRATOR` to the single Spring Security authority `ROLE_ADMIN` — there is no `@PreAuthorize` anywhere in the app, so `SecurityConfig`'s request matchers are the *only* enforcement point, and none of them distinguished the two roles either. Any plain `ADMINISTRATOR` could call `POST /api/v1/admin/admin-access-requests/{id}/approve` and grant `ADMINISTRATOR` to any account, with no further checkpoint — exactly the escalation this item exists to prevent.
  Fixed: `BridgingUserDetailsService` now grants a distinct `ROLE_SUPER_ADMIN` authority to accounts holding `SUPER_ADMINISTRATOR`, and to the legacy `AppUser` admin account (confirmed via a read-only production query: there's exactly one, it's the site owner's own login, and it predates the whole verification workflow — granting it `SUPER_ADMIN` too means the owner can never be the one account locked out of this). `SecurityConfig` now gates the approval endpoint specifically behind `hasRole("SUPER_ADMIN")`, ordered before the general `/api/v1/admin/**` → `ADMIN` rule. New `AdminAccessRequestApprovalSecurityTest` proves all four cases (unauthenticated → 401, `ROLE_USER` → 403, plain `ROLE_ADMIN` → 403, `ROLE_ADMIN`+`ROLE_SUPER_ADMIN` → succeeds); `BridgingUserDetailsServiceTest` extended for the new authority-granting logic.
  Confirmed via the same production query that as of this fix, the existing `ADMINISTRATOR` `UserAccount` in production keeps every capability it had before *except* approving new admin-access-requests — nobody's access was reduced beyond the one thing this was meant to restrict.

## 14. Audit-Log Tampering
- **Scenario**: an admin (or attacker with admin access) attempts to delete/alter `AuditEvent` rows to hide activity.
- **Risk**: loss of accountability, undetectable misconduct.
- **Preventive**: `AuditEvent` table is append-only at the application layer (no update/delete endpoint exists); consider a separate, more restricted DB credential for audit writes.
- **Detective**: periodic integrity check (e.g., row-count/hash-chain monitoring) if tampering risk is judged high enough to warrant it.
- **Recovery**: restore from backup, cross-reference with external logs (reverse proxy/access logs) if available.
- **Test coverage**: tests confirming no API surface permits audit-event mutation or deletion.
- **Ground truth, fixed (2026-07-31)**: this control was already correctly built (`AdminAuditLogApiController` has exactly one `@GetMapping` and nothing else; `AuditLogService` never exposes a delete/update path) but had no regression test — a future PR could add a delete endpoint with nothing to catch it. Added `AdminAuditLogMutationSecurityTest`, asserting `DELETE`/`PUT` to `/api/v1/admin/audit-log/1` both 404 (proving no such route exists, not just "not authorized").

## 15. Scraping the Family Tree
- **Scenario**: automated bulk extraction of the Whole Banshawali data via repeated API calls.
- **Risk**: bulk exposure of family data beyond intended per-user browsing.
- **Preventive**: rate limiting on tree/search/profile endpoints, viewport/branch-scoped responses (never a full-graph dump endpoint), authentication required for any non-aggregate data.
- **Detective**: anomalous request-volume monitoring per account.
- **Recovery**: account suspension, rate-limit tightening.
- **Test coverage**: rate-limit tests, payload-size tests confirming tree endpoints never return unbounded data.
- **Ground truth, partially fixed (2026-07-31)**: `RateLimitFilter` previously covered only signup/login (its own javadoc said so) — `GET /api/v1/family-tree` and `GET /api/v1/persons` (search) were completely unthrottled. Fixed: both now rate-limited (20/min/IP and 60/min/IP respectively, generous enough for real usage — `/tree` fetches the whole graph once per page load post-Phase-5-fix, window expansion is client-side slicing with no re-fetch; search is live-typing typeahead). New tests in `RateLimitFilterTest` mirror the existing per-path/per-IP isolation pattern. **Still true, not fully closed**: calling `/api/v1/family-tree` with no query params still returns everyone in one response (`FamilyTreeAssembler`'s own javadoc: "the original behavior, still used by /family's unwindowed fetch") — rate limiting bounds *how often* a full dump can happen, it doesn't make the endpoint incapable of one. "Never a full-graph dump endpoint" remains false as a blanket claim; it's now a rate-limited one instead of an unlimited one.

## 16. Sensitive Logging
- **Scenario**: passwords, tokens, or full profile/verification details end up in application logs, error trackers, or analytics.
- **Risk**: secondary data breach via log access.
- **Preventive**: structured logging with explicit field allowlists; never log request bodies for auth/signup/verification endpoints; error messages sanitized before returning to clients or logging.
- **Detective**: periodic log-content review/linting for known-sensitive field names.
- **Recovery**: log purge/rotation, credential rotation if a leak is confirmed.
- **Test coverage**: tests asserting logs from auth/signup flows never contain raw passwords/tokens.
- **Ground truth (2026-07-31)**: confirmed safe by inspection — `CorrelationIdFilter` logs only method/URI/status/duration, no request-logging filter or debug-level Spring web logging exists anywhere, and no DTO has a `toString()` override that could leak field values. But there is **no regression test** proving this (`LoggingConfigTest`'s one test asserts filter registration/ordering, not log content) — not added this pass; a real test would need a Logback `ListAppender` attached during a MockMvc signup/login call, which wasn't in this pass's fix list. Remains a real, cheap-to-close gap for later.

## 17. Dependency Vulnerabilities
- **Scenario**: a known CVE in a Spring/Next.js/npm dependency.
- **Risk**: varies by CVE; could be RCE.
- **Preventive**: dependency scanning in CI (already partially covered by Actuator/Prometheus presence suggesting some operational maturity; add explicit `dependabot`/OWASP dependency-check).
- **Detective**: CI failure on new critical CVEs.
- **Recovery**: patch/upgrade pipeline, documented in the deployment runbook.
- **Test coverage**: CI gate on dependency-scan severity threshold.
- **Ground truth (2026-07-31)**: `dependabot.yml` (maven, npm, github-actions) and CodeQL (`codeql.yml`, static analysis, not dependency/CVE scanning) both exist, added in earlier Phase 1 work this session — but neither gates anything. Dependabot only opens PRs; CodeQL doesn't check dependency CVEs at all. There is **no branch protection on `main`** (confirmed via the GitHub API), so nothing could block a merge even if a check existed. Not fixed this pass — deliberately: enabling branch protection and an actual severity-gated SCA step changes real merge/deploy behavior (this repo currently auto-deploys straight from a push to `main`) and deserves its own explicit decision, not a change bundled into a test-writing pass.

## 18. Backup Exposure
- **Scenario**: a `mysqldump` backup (containing full private family data) stored insecurely (public bucket, unencrypted, world-readable path).
- **Risk**: full data breach.
- **Preventive**: encrypted backups, access-restricted storage location, no backups committed to git or placed under a web-served directory.
- **Detective**: periodic access-control audit of backup storage location.
- **Recovery**: rotate any credentials/tokens that might have been embedded in application config alongside data, notify affected users if a breach is confirmed.
- **Test coverage**: N/A (operational control, verified via runbook/checklist rather than automated test) — included in the Phase 7 production-readiness checklist.
- **Ground truth (2026-07-31)**: confirmed unchanged — `/srv/scripts/backup/` is still empty, and backup automation has never been built for any app on this host, familytree included. Accurately described by the doc as out of repo scope; not something this pass touches.

## Formal Security Review (2026-07-31)

A full pass went through every item above against actual code and actual tests (not just this doc's
claims), via three parallel audits. Real, fixable gaps were closed: the Super Admin privilege-escalation
gap (item 13, the most severe finding — any plain admin could mint more admins), the legacy `POST
/persons` authorization gap (item 9), missing rate limiting on the tree/search endpoints (item 15), a
missing CSP header everywhere (item 6), missing session-cookie hardening (item 11), and five missing
regression tests for controls that already worked but had nothing proving it (items 3, 7, 8, 14, plus the
audit-log one). Confirmed **N/A, never built** rather than weakened: password reset (items 2, 12),
image/photo upload (item 10). Confirmed real, deliberately **not** fixed this pass, and flagged rather than
silently accepted: per-account lockout/CAPTCHA (item 1), the admin `link()` bypass of the verification
workflow (item 5), a full `PrivacyPreference`-classification authorization matrix (item 4), "revoke all
sessions" (item 11), the family-tree endpoint's unwindowed full-dump capability (item 15, now
rate-limited but not eliminated), a sensitive-logging regression test (item 16), and a CI severity gate +
branch protection for dependency scanning (item 17, since it changes real merge/deploy behavior and
deserves its own decision). See `docs/08-implementation-roadmap.md` Phase 7 for how this fits the
broader production-hardening picture.

## Baseline Note

Some of these controls (CSRF, bcrypt-family hashing, Actuator-endpoint IP restriction) already exist in the current codebase and must not regress during the rewrite — see `01-current-system-assessment.md` for what is already working.
