# 02 — Product Requirements

## Vision

A bilingual (English/Nepali), privacy-aware, production-grade genealogy platform for the Bhatta Banshawali: approved family members sign up, are matched against the existing family tree with transparent confidence levels, and can then explore their ancestry, the whole Banshawali, and family history — all while private data about living relatives stays protected.

## Roles (see `05-auth-and-verification.md` and the permission matrix below)

Visitor → Pending Member → Verified Member → Family Editor → Administrator → Super Administrator. Each is strictly additive in capability; every sensitive action is authorized server-side regardless of what the UI shows.

## Functional Requirements by Area

### Public Site
- Landing page with hero, mission statement, language switcher, login/request-membership CTAs.
- Admin-managed (not hardcoded) content for: About the Banshawali, Family History (origin, migration, branches, timeline), Public Statistics (aggregate-only), membership-verification explainer, privacy statement, contact.
- No private member data, no full tree, no PII surfaced pre-authentication.

### Signup & Verification
- Fields: email, full name, DOB in AD, DOB in BS (bidirectionally synced), father's full name, grandfather's full name, password (+confirmation), preferred language, ToS/privacy agreement. Optional: mother's name, place of birth, ancestral village, branch/clan, known relative, invitation code, note to admin.
- AD↔BS conversion via a vetted calendar library (never approximate arithmetic), storing canonical value + original entry + conversion metadata.
- Confidence-scored family-match engine (HIGH/MEDIUM/LOW) using normalized name comparison (English, Nepali, transliteration, aliases) plus father/grandfather lineage checks.
- Neutral status UI at every stage — never confirms or denies the existence of a specific family member during signup.
- Email verification required before any match/review proceeds.

### Authentication & Account Security
- Login/logout, forgot/reset password (expiring single-use tokens), change password, session revocation, optional MFA, adaptive throttling/lockout, rate limiting, audit trail for security events, secure/httpOnly cookies, CSRF protection, generic error messages (no enumeration), bot protection on public auth endpoints.

### Member Dashboard
- Welcome area (name, AD+BS date, verification/profile-completion status), family snapshot (parents, spouse, children, siblings, generation, branch), ancestry summary, quick actions, featured historical content, and a data-quality/privacy panel distinguishing verified/submitted/disputed/unknown facts.

### Your Family
- Parents, grandparents, great-grandparents, ancestors-by-generation, spouse(s), children, grandchildren, siblings, aunts/uncles, derivable cousins, branch, relative generation number.
- Five views: Visual Tree, Generation List, Ancestor View, Descendant View, Relationship Path (two-person kinship calculator).
- Must correctly represent multiple marriages, adoption, step-relationships, unknown parents, partial dates, deceased members, and records under review — without crashing or misrepresenting the family structure.

### Whole Banshawali
- Full-dataset interactive tree: pan/zoom/fit/center, search-and-jump, expand/collapse, lazy-loaded branches/generations, generation and branch filters, ancestor-only/descendant-only modes, minimap, fullscreen, mobile fallback, keyboard access, shareable deep links (subject to permission), branch export/print, and explicit loading/empty/error states.
- Must remain usable at realistic scale (benchmark at 500 / 2,000 / 10,000 people) without freezing the browser or shipping the entire graph payload at once.

### Search & Profiles
- Search across English/Nepali name, transliteration, alternate names, father/grandfather name, AD/BS birth year, location, branch, generation — always privacy-filtered by the requester's role.
- Person profile: bilingual names, photo, AD/BS birth/death dates, birthplace, residence, biography, branch, generation, parents/spouses/children/siblings, source citations, verification state, revision history, privacy classification.

### Content Management
- Draft/review/publish/unpublish/revision-history workflow for homepage sections, history articles, timeline entries, FAQs, announcements — bilingual, admin-authored, not hardcoded into UI components.

### Administration
- Signup review queue with match evidence, people/relationship management, duplicate detection and safe merge (never automatic), change-request review with before/after diffs, role/user management, audit log, data-quality reports (missing parents, cycles, unlinked accounts).

## Non-Functional Requirements

- **Privacy**: field-level classification (public / verified-family-only / branch-only / admin-only / private-to-linked-user); minimized display of living-member data by default.
- **Bilingual-first**: no hardcoded English copy anywhere a Nepali equivalent is expected; UI must not break under longer Nepali text.
- **Auditability**: no silent overwrite of genealogy facts — every sensitive change goes through a change-request with before/after snapshot, submitter, reviewer, and rollback path.
- **Performance**: indexed name search, bounded/paginated API responses, no N+1 queries, tree endpoints scoped to viewport/branch/generation rather than full-graph.
- **Security**: threat-modeled (see `09-security-threat-model.md`); no entity leakage from persistence models directly to API responses.
- **Testability**: layered test strategy (see `10-testing-strategy.md`) covering genealogy invariants (no cycles, no duplicate edges, correct derived relationships) as first-class test subjects, not afterthoughts.

## Explicit Non-Goals (for this phase)

- No graph database — relational recursive/materialized-path queries are sufficient at the demonstrated and plausible scale (family genealogy, not a social network).
- No mobile native app — responsive web only.
- No public API for third-party consumption — the API is for this product's own frontend only, versioned for internal evolution, not external partners.
- No automatic merging of duplicate person records under any circumstance.

## Assumptions Requiring Confirmation

1. The production MySQL instance's actual current row counts and data quality are unknown from the repository alone and must be inventoried directly (see `07-migration-plan.md`) before schema work proceeds.
2. "Approved family members" implies a bounded, known population (not open public signup) — the verification workflow assumes there is a real existing family tree to match against, not a cold-start empty database.
3. Deployment remains single-host / self-hosted-runner based for the near term (no requirement stated for Kubernetes or multi-region); the architecture should not over-engineer for scale not requested.
4. Email sending infrastructure (for verification/reset emails) does not yet exist in this repo and must be selected/provisioned as part of Phase 1.
