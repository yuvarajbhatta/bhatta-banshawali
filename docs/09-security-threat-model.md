# 09 — Security Threat Model

For each threat: attack scenario, risk, preventive control, detective control, recovery approach, test coverage.

## 1. Credential Stuffing / Brute Force
- **Scenario**: automated login attempts against known/guessed email+password pairs.
- **Risk**: account takeover, exposure of private family data.
- **Preventive**: adaptive lockout/throttling per account and per IP, bot protection (CAPTCHA) on `/login`, rate limiting at the reverse proxy.
- **Detective**: `AuditEvent` on repeated failed logins; alerting on threshold breach.
- **Recovery**: forced password reset + session revocation for affected accounts.
- **Test coverage**: authentication integration tests asserting lockout triggers after N failures; rate-limit tests.

## 2. Account Enumeration
- **Scenario**: probing signup/login/reset endpoints to infer whether an email is registered.
- **Risk**: reveals family membership indirectly.
- **Preventive**: identical response shape/timing regardless of account existence, across login, signup, and password-reset.
- **Detective**: monitoring for high-volume distinct-email probing from a single source.
- **Recovery**: N/A (informational leak, not account compromise) — rate-limit and block source.
- **Test coverage**: explicit tests asserting identical response bodies/status codes for existing vs. non-existing emails.

## 3. Family-Member Enumeration via Signup Matching
- **Scenario**: an attacker submits guessed father/grandfather names during signup to learn whether specific people exist in the private tree.
- **Risk**: leaks private genealogy structure through the public signup form.
- **Preventive**: neutral pending-status response regardless of match confidence (HIGH/MEDIUM/LOW all look identical to the applicant); match evidence never returned to the applicant, only to admins.
- **Detective**: rate-limiting + logging of repeated signup attempts with varying father/grandfather combinations from the same source.
- **Recovery**: block source, review any admin-visible match evidence for signs of targeted probing.
- **Test coverage**: signup-matching tests asserting API/UI responses are indistinguishable across confidence levels; abuse-pattern tests.

## 4. Unauthorized Access to Private Family Data (IDOR / Broken Authorization)
- **Scenario**: a Verified Member requests `/api/v1/people/{id}` for a person outside their permitted privacy scope by guessing/incrementing IDs.
- **Risk**: exposure of privacy-classified data (living members, disputed facts) to unauthorized viewers.
- **Preventive**: centralized, per-request server-side authorization check against `PrivacyPreference` classification and the requester's role/relationship to the target — never client-side filtering alone; DTOs never expose fields the viewer isn't authorized to see (not returned-then-hidden).
- **Detective**: audit log of denied authorization attempts, especially repeated sequential ID probing.
- **Recovery**: N/A if preventive control holds; incident review of audit logs if a gap is found.
- **Test coverage**: authorization tests for every role × every privacy classification × every endpoint (matrix testing, not spot checks).

## 5. False Identity Linkage
- **Scenario**: an applicant claims to be a specific existing `Person` (living or historical) they are not, to gain access to that person's connections/data.
- **Risk**: impersonation, exposure of a real family member's private data to an unrelated party.
- **Preventive**: `UserPersonLink` only created via the confidence-scored verification workflow with admin review for anything below HIGH; never created from client-asserted claims alone.
- **Detective**: audit trail on every `UserPersonLink` creation, admin-reviewable.
- **Recovery**: `UNLINKED` status (not deletion) on discovery, full audit trail preserved, affected person's data access reviewed.
- **Test coverage**: verification-matching tests including deliberately adversarial/incorrect lineage claims.

## 6. XSS
- **Scenario**: stored data (biography, notes, history article content) rendered without escaping.
- **Risk**: session/cookie theft, action-on-behalf-of-victim.
- **Preventive**: React's default escaping (Next.js) for all rendered user content; strict CSP; rich-text content (history articles) sanitized server-side with an allowlist-based sanitizer, never raw HTML passthrough.
- **Detective**: CSP violation reporting.
- **Recovery**: session revocation, content review/rollback via `HistoricalArticle` revision history.
- **Test coverage**: XSS payload tests against every free-text field (notes, biography, article body).

## 7. CSRF
- **Scenario**: cross-site request forgery against session-cookie-authenticated state-changing endpoints.
- **Risk**: unauthorized changes performed as a logged-in user.
- **Preventive**: CSRF token required on all state-changing requests (already present in the current app; must be preserved/extended for the new API), `SameSite` cookies.
- **Detective**: rejected-CSRF-token audit logging.
- **Recovery**: N/A if preventive control holds.
- **Test coverage**: tests asserting state-changing endpoints reject requests missing/mismatching the CSRF token.

## 8. SQL Injection
- **Scenario**: crafted input into search or admin filters.
- **Risk**: data exfiltration/corruption.
- **Preventive**: JPA/parameterized queries exclusively (already the pattern in `PersonRepository`); no string-concatenated JPQL/SQL introduced during the rewrite.
- **Detective**: WAF/query-anomaly monitoring if available.
- **Recovery**: restore from backup if corruption occurs.
- **Test coverage**: repository tests with adversarial search-keyword inputs.

## 9. Mass Assignment
- **Scenario**: a client sends extra fields (e.g., `role`, `verificationState`, `privacyClassification`) on a profile-update request, hoping the backend blindly binds them.
- **Risk**: privilege escalation, unauthorized data exposure change.
- **Preventive**: explicit per-use-case request DTOs (never binding directly to `Person`/`UserAccount` entities as `PersonController.savePerson(@Valid Person person, ...)` does today), allowlisted fields only.
- **Detective**: schema validation rejection logging for unexpected fields.
- **Recovery**: N/A if preventive control holds.
- **Test coverage**: API tests submitting extra/forbidden fields and asserting they have no effect.

## 10. Malicious Image Upload
- **Scenario**: an uploaded "photo" is actually a malicious file (polyglot, embedded script, oversized/zip-bomb image).
- **Risk**: stored XSS, resource exhaustion, malware distribution.
- **Preventive**: strict MIME/content-type validation independent of file extension, image re-encoding (not passthrough), size limits, virus scan (`MediaAsset.virusScanStatus`), storage outside the web root/served via a locked-down path with no execute permissions.
- **Detective**: virus-scan failure logging and alerting.
- **Recovery**: quarantine/delete flagged assets, notify uploader.
- **Test coverage**: upload tests with adversarial file types/sizes.

## 11. Session Theft
- **Scenario**: session cookie stolen via XSS, network interception, or a shared/public device.
- **Risk**: account takeover.
- **Preventive**: httpOnly + `Secure` + `SameSite` cookies, TLS everywhere, "revoke all sessions" self-service capability, idle/absolute session timeouts.
- **Detective**: `AuditEvent` on login from a new device/IP pattern (informational, not blocking).
- **Recovery**: user-initiated or admin-initiated session revocation.
- **Test coverage**: cookie-attribute assertions in integration tests; session-timeout tests.

## 12. Password-Reset Abuse
- **Scenario**: repeated reset requests for a target email (harassment/enumeration), or reset-token guessing.
- **Risk**: account lockout via spam, or token brute force.
- **Preventive**: rate-limited reset requests, cryptographically random single-use expiring tokens, token invalidated after use or after a new request supersedes it.
- **Detective**: audit logging of reset-request volume per account.
- **Recovery**: N/A if preventive control holds.
- **Test coverage**: token-expiry and single-use tests, rate-limit tests.

## 13. Admin Privilege Escalation
- **Scenario**: a Family Editor or compromised admin account attempts to grant itself Administrator/Super Administrator rights.
- **Risk**: full system compromise.
- **Preventive**: role changes require Super Administrator approval and are themselves a `ChangeRequest` subject to audit; no self-service role elevation endpoint exists at any layer.
- **Detective**: `AuditEvent` on every role change, alerted.
- **Recovery**: immediate role reversion + forced re-authentication + audit review.
- **Test coverage**: authorization tests confirming no non-Super-Admin path can alter roles.

## 14. Audit-Log Tampering
- **Scenario**: an admin (or attacker with admin access) attempts to delete/alter `AuditEvent` rows to hide activity.
- **Risk**: loss of accountability, undetectable misconduct.
- **Preventive**: `AuditEvent` table is append-only at the application layer (no update/delete endpoint exists); consider a separate, more restricted DB credential for audit writes.
- **Detective**: periodic integrity check (e.g., row-count/hash-chain monitoring) if tampering risk is judged high enough to warrant it.
- **Recovery**: restore from backup, cross-reference with external logs (reverse proxy/access logs) if available.
- **Test coverage**: tests confirming no API surface permits audit-event mutation or deletion.

## 15. Scraping the Family Tree
- **Scenario**: automated bulk extraction of the Whole Banshawali data via repeated API calls.
- **Risk**: bulk exposure of family data beyond intended per-user browsing.
- **Preventive**: rate limiting on tree/search/profile endpoints, viewport/branch-scoped responses (never a full-graph dump endpoint), authentication required for any non-aggregate data.
- **Detective**: anomalous request-volume monitoring per account.
- **Recovery**: account suspension, rate-limit tightening.
- **Test coverage**: rate-limit tests, payload-size tests confirming tree endpoints never return unbounded data.

## 16. Sensitive Logging
- **Scenario**: passwords, tokens, or full profile/verification details end up in application logs, error trackers, or analytics.
- **Risk**: secondary data breach via log access.
- **Preventive**: structured logging with explicit field allowlists; never log request bodies for auth/signup/verification endpoints; error messages sanitized before returning to clients or logging.
- **Detective**: periodic log-content review/linting for known-sensitive field names.
- **Recovery**: log purge/rotation, credential rotation if a leak is confirmed.
- **Test coverage**: tests asserting logs from auth/signup flows never contain raw passwords/tokens.

## 17. Dependency Vulnerabilities
- **Scenario**: a known CVE in a Spring/Next.js/npm dependency.
- **Risk**: varies by CVE; could be RCE.
- **Preventive**: dependency scanning in CI (already partially covered by Actuator/Prometheus presence suggesting some operational maturity; add explicit `dependabot`/OWASP dependency-check).
- **Detective**: CI failure on new critical CVEs.
- **Recovery**: patch/upgrade pipeline, documented in the deployment runbook.
- **Test coverage**: CI gate on dependency-scan severity threshold.

## 18. Backup Exposure
- **Scenario**: a `mysqldump` backup (containing full private family data) stored insecurely (public bucket, unencrypted, world-readable path).
- **Risk**: full data breach.
- **Preventive**: encrypted backups, access-restricted storage location, no backups committed to git or placed under a web-served directory.
- **Detective**: periodic access-control audit of backup storage location.
- **Recovery**: rotate any credentials/tokens that might have been embedded in application config alongside data, notify affected users if a breach is confirmed.
- **Test coverage**: N/A (operational control, verified via runbook/checklist rather than automated test) — included in the Phase 7 production-readiness checklist.

## Baseline Note

Some of these controls (CSRF, bcrypt-family hashing, Actuator-endpoint IP restriction) already exist in the current codebase and must not regress during the rewrite — see `01-current-system-assessment.md` for what is already working.
