# 05 — Authentication and Verification Design

## Authentication Model

**Server-side sessions (Spring Session), not JWT-in-browser-storage.**

### Why
- Next.js and Spring Boot will sit behind a shared reverse proxy on the same deployment host, making same-origin (or same-parent-domain) cookies straightforward to configure securely (httpOnly, `Secure`, `SameSite=Lax` or `Strict`).
- Section 6 of the product brief explicitly forbids long-lived tokens in `localStorage`; a correctly-implemented httpOnly session cookie sidesteps this entirely rather than requiring careful short-lived-access/rotated-refresh-token engineering.
- The current codebase already has working Spring Security form-login session infrastructure and CSRF wiring (`SecurityConfig`, and CSRF meta tags consumed by `lineage.js`) — extending this is lower-risk than introducing a token issuance/rotation/revocation subsystem from scratch for a single-host deployment with no stated requirement for cross-domain mobile/third-party API clients.
- Session revocation ("revoke all active sessions" — a required capability) is a native, simple operation against a session store; equivalent JWT revocation requires an additional denylist mechanism regardless, eliminating the "stateless" benefit typically cited for tokens.

### Session Store
Spring Session, JDBC-backed initially (reuses the existing MySQL instance — no new infrastructure dependency for a single-host deployment). Revisit Redis-backed sessions only if session-store latency or multi-instance scaling becomes an actual, measured need.

## Required Capabilities and Implementation Notes

| Capability | Mechanism |
|---|---|
| Secure signup | Email + password (min length/entropy checks), no username enumeration in error messages |
| Email verification | Single-use, expiring, signed token emailed on signup; account remains `PENDING_EMAIL_VERIFICATION` until confirmed |
| Login / logout | Spring Security form login (retained), session invalidation on logout |
| Forgot / reset password | Single-use, expiring (e.g., 30 min) token, invalidated after use or on any subsequent successful login |
| Change password | Requires current password re-entry; invalidates other active sessions optionally |
| Session management | List/revoke active sessions per user, backed by Spring Session's session registry |
| Account lockout / throttling | Adaptive delay or lockout after N failed attempts per account **and** per IP, to resist both credential stuffing and targeted brute force |
| Rate limiting | Applied at the reverse proxy or a filter layer on `/login`, `/signup`, `/forgot-password`, `/verify-email` |
| MFA (optional) | TOTP-based, stored as an opt-in flag on `UserAccount`; not required for MVP but the data model reserves the field |
| Audit trail | Every login success/failure, password reset, role change, and account lock/unlock recorded as an `AuditEvent` |
| Secure cookies | httpOnly, `Secure` (once TLS is confirmed in the deployment), `SameSite` |
| CSRF protection | Retained (already present) since cookie-based auth is used |
| Generic error messages | Login and password-reset return identical messaging regardless of whether the email/account exists. Signup is an accepted exception: it reports "email already exists" explicitly (see "Anti-Enumeration Guarantees" below) |
| Bot protection | CAPTCHA (e.g., Cloudflare Turnstile — see the `turnstile-spin` tooling available in this environment) on signup and login |
| Security headers | CSP, `X-Content-Type-Options`, `Referrer-Policy`, `Strict-Transport-Security` once TLS is in place, set via a servlet filter or the reverse proxy |
| Safe redirect handling | Post-login/post-verification redirect targets validated against an allowlist, never taken raw from a query parameter |

## Signup and Family-Match Workflow

### Steps (per product brief Section 5)
1. Collect account info (email, password, preferred language).
2. Collect identity/lineage info (name, DOB AD+BS, father's name, grandfather's name, optional fields).
3. Show AD/BS conversion clearly and immediately as the applicant types (client-side conversion via the same library/algorithm the backend uses, to avoid a round-trip feeling broken; backend remains the source of truth on submit).
4. Review screen before submission.
5. Email verification (blocks all further steps until complete).
6. Family-match logic runs server-side only after verification.
7. Neutral pending/approved status shown — identical UI regardless of match confidence, to prevent enumeration.
8. Administrators notified only for MEDIUM/LOW confidence cases requiring review.
9. Applicant notified on any status change (approved/rejected/needs-more-info), via email, with generic wording that does not reveal *why* in a way that would leak tree structure.

### Confidence Scoring

**Inputs**: normalized applicant name, normalized father name, normalized grandfather name, DOB (AD/BS), optional fields.

**Normalization pipeline** (extending the existing `NameTransliterationService` rather than replacing it):
- Case-folding, whitespace/punctuation normalization.
- Diacritic and Unicode normalization (NFC) for Nepali text.
- Transliteration-aware comparison (English↔Nepali via the existing digraph/letter engine, extended with a proper phonetic-matching library evaluated in Phase 1 — the current hand-rolled substitution table is a reasonable heuristic but should be validated against a broader Nepali name corpus before being trusted for security-relevant matching decisions).
- Alias/nickname table lookup (new — no equivalent exists today).
- Common prefix/suffix stripping (honorifics, "Bhatta" clan-name normalization already partially present in `NameTransliterationService.cleanup`).

**Matching logic**:
- Search existing `Person` records (via `PersonName`) for candidates matching the applicant's stated name.
- For each candidate, verify a `PARENT_OF` edge from a person matching the stated father's name, and a `PARENT_OF` edge from that father to a person matching the stated grandfather's name.
- Score:
  - **HIGH**: applicant-name match + father match + grandfather-lineage match all strong, with consistent (or absent, non-conflicting) DOB.
  - **MEDIUM**: plausible name matches but an ambiguous branch (multiple candidates), an incomplete lineage chain, or a DOB conflict that isn't clearly disqualifying.
  - **LOW**: no reliable match, or a direct conflict (e.g., stated father's name matches no recorded parent of any candidate with the applicant's name).
- **HIGH → configurable auto-approve** (admin policy toggle; defaults to still requiring a lightweight admin confirmation until the matcher has a track record).
- **MEDIUM → admin review queue**, with match evidence visible only to administrators.
- **LOW → admin review queue**, applicant sees only a neutral "additional review needed" message — the system never states that no match was found, since that would itself leak information (e.g., confirm that a father's name genuinely isn't in the tree).

### Anti-Enumeration Guarantees
- Login and password-reset responses are identical in timing and wording regardless of whether an email/account exists (constant-response-shape, and rate-limited to reduce timing-based inference).
- **Signup is a deliberate exception**: signup's *email-uniqueness* check breaks from constant-response-shape — `POST /api/v1/signup` returns HTTP 409 with an explicit "email already exists" message when the email is already registered, rather than silently no-op'ing. The site owner made this call for a small, invitation-oriented family site where a vague "check your email" response after a duplicate signup caused real confusion (people assuming the form was broken or emailing to ask why nothing arrived), and judged that cost to outweigh the residual risk of an attacker learning which emails have accounts. Family-match confidence (whether the applicant's stated name/lineage matched anyone in the tree) is unaffected by this and remains fully neutral — see the next bullet.
- Family-match responses remain identical in wording regardless of match confidence (HIGH/MEDIUM/LOW all show the same neutral "pending review" status to the applicant). Match evidence (which specific `Person` records were considered, at what confidence) is visible only inside the admin review UI, never returned to the applicant in any API response, error message, or client-side bundle.

## Optional Verification Fields

Modeled as nullable fields on `VerificationRequest` (see `04-data-model.md`), admin-configurable as to which are shown/required via a settings flag — never forced on the applicant by default.

## Open Question for Admin Policy (flag for user decision in Phase 3)

Whether HIGH-confidence matches should truly auto-approve without any human touch, or always land in a lightweight "confirm and approve" queue for the first operating period. Recommendation: start with the latter (safer), revisit once real match data accumulates.
