# 04 — Data Model

## Design Principles

1. **A login is not a person.** `UserAccount` and `Person` are separate entities linked by an optional, verified `UserPersonLink` — never merged, never inferred from name match alone.
2. **Facts are separate from presentation.** Rendered labels (e.g., "Grandfather") are computed from stored relationships and a viewer's perspective; they are never stored as source-of-truth strings.
3. **Prefer derived over stored** for relationships reliably computable from parent/child/spouse edges (siblings, grandparents, aunts/uncles, cousins) to avoid contradictory duplicate facts. Store only what cannot be safely derived (adoption, step-relationships, guardianship) as explicit edges.
4. **No silent overwrites.** Any change to a person's core facts (parents, spouse, DOB, privacy classification) goes through `ChangeRequest`, never a direct update outside of admin-reviewed flows.

## Core Entities

```
UserAccount
  id, email (unique), passwordHash, status (PENDING_EMAIL_VERIFICATION | ACTIVE | LOCKED | DISABLED),
  preferredLanguage, mfaEnabled, createdAt, lastLoginAt

Role / Permission
  Role: VISITOR* | PENDING_MEMBER | VERIFIED_MEMBER | FAMILY_EDITOR | ADMINISTRATOR | SUPER_ADMINISTRATOR
  (*VISITOR is implicit/unauthenticated, not a stored assignment)
  Permission: fine-grained action grants, mapped many-to-many to Role, checked centrally in the authorization layer

UserPersonLink
  id, userAccountId (0..1), personId (0..1), linkStatus (PENDING | VERIFIED | REJECTED | UNLINKED),
  verifiedBy, verifiedAt
  -- unique constraint: at most one VERIFIED link per person, at most one VERIFIED link per user

Person
  id, generationNumber, familyBranchId, sex, isLiving, isDeceased,
  birthDate{ type: EXACT|MONTH_YEAR|YEAR_ONLY|APPROX|BEFORE|AFTER|UNKNOWN, valueAd, valueBs, originalEntry, conversionMeta },
  deathDate (same structure), birthPlaceId, currentResidencePlaceId,
  biography, privacyClassification, verificationState (VERIFIED|FAMILY_REPORTED|INFERRED|DISPUTED|UNKNOWN),
  createdAt, updatedAt

PersonName
  id, personId, nameType (ENGLISH|NEPALI|ALIAS|NICKNAME|TRANSLITERATION),
  firstName, middleName, lastName, isPrimaryForType, source

Relationship
  id, personId, relatedPersonId, relationshipType, startDate?, endDate?, note,
  confidence (VERIFIED|FAMILY_REPORTED|INFERRED|DISPUTED),
  createdAt, createdBy
  -- unique constraint: (personId, relatedPersonId, relationshipType)
  -- CHECK: personId <> relatedPersonId

RelationshipType
  PARENT_OF, SPOUSE_OF, ADOPTIVE_PARENT_OF, STEP_PARENT_OF, GUARDIAN_OF
  (CHILD_OF and derived kinship — sibling, grandparent, cousin, aunt/uncle — are computed, not stored,
   except where the parent edge itself is ambiguous/disputed)

FamilyBranch
  id, name (bilingual), description, foundingAncestorPersonId, createdAt

Place
  id, name (bilingual), region, country, normalizedName

MediaAsset
  id, ownerPersonId?, uploaderUserAccountId, storageKey, mimeType, caption (bilingual),
  privacyClassification, virusScanStatus, createdAt

HistoricalArticle
  id, slug, titleEn, titleNe, bodyEn, bodyNe, status (DRAFT|IN_REVIEW|PUBLISHED|UNPUBLISHED),
  authorUserAccountId, reviewedBy, publishedAt, revisionOf (self-referential, for history)

SourceCitation
  id, personId or relationshipId (polymorphic reference), citationType (DOCUMENT|INTERVIEW|RECORD|PHOTO|NOTE),
  description, mediaAssetId?, createdBy, createdAt

VerificationRequest
  id, userAccountId, submittedFatherName, submittedGrandfatherName, submittedDobAd, submittedDobBs,
  optionalFields (mother name, birthplace, ancestral village, branch, known relative, invitation code, note),
  matchConfidence (HIGH|MEDIUM|LOW), matchedPersonCandidateIds (admin-visible only),
  status (PENDING|APPROVED|REJECTED|NEEDS_MORE_INFO), reviewedBy, reviewedAt, decisionNote

ChangeRequest
  id, entityType, entityId, changeType (FIELD_UPDATE|PARENT_CHANGE|SPOUSE_CHANGE|MERGE|DELETE|PRIVACY_CHANGE|
  USER_PERSON_LINK|ROLE_CHANGE), beforeSnapshot (JSON), afterSnapshot (JSON), reason, sourceCitationIds,
  submittedBy, status (PENDING|APPROVED|REJECTED), reviewedBy, reviewedAt, rollbackOfChangeRequestId?

AuditEvent
  id, actorUserAccountId, eventType, entityType, entityId, ipAddress, userAgent, metadata (JSON), createdAt

Notification
  id, userAccountId, type, payload (JSON), readAt, createdAt

PrivacyPreference
  id, personId, fieldName, classification (PUBLIC|VERIFIED_FAMILY|BRANCH_ONLY|ADMIN_ONLY|PRIVATE_TO_LINKED_USER)

Session
  (Spring Session-managed; not a hand-rolled entity)
```

## Relationship to the Current Schema

| Current | Target | Migration Note |
|---|---|---|
| `AppUser (username, password, role: String)` | `UserAccount` + `Role`/`Permission` + `UserPersonLink` | `username` becomes email (requires backfill/collection); `role` string becomes an enum-backed FK; no existing link data to migrate — must be created via admin reconciliation. |
| `Person (firstName, firstNameNepali, ...)` | `Person` + `PersonName` rows | Existing flat name columns migrate into `PersonName` rows of type `ENGLISH`/`NEPALI`; `notes`, `photoPath`, `birthPlace`, `currentAddress` map onto `Person`/`Place`/`MediaAsset`. |
| `Relationship (person, relatedPerson, type: FATHER|MOTHER|SPOUSE|CHILD)` | `Relationship (PARENT_OF, SPOUSE_OF, ...)` | `FATHER`/`MOTHER` collapse into `PARENT_OF` with a `parentRole` attribute (or retain distinct types if sex-of-parent must remain explicit — decide in Phase 1 based on how `MOTHER`/`FATHER` are actually used in existing UI logic); `CHILD` edges become derived rather than stored, since they were only ever the inverse of `PARENT_OF`. |
| No cycle constraint | Application-level cycle check on every `Relationship` write + periodic admin data-quality report | New; must run once against existing data before enforcement is turned on, to surface any pre-existing cycles for manual correction rather than rejecting writes that "fix" a state the constraint doesn't yet know about. |

## Query Strategy

- **Ancestor/descendant traversal**: MySQL 8+ `WITH RECURSIVE` CTEs for on-demand queries (profile pages, relationship-path calculation between two people), backed by indexes on `(person_id, relationship_type)` and `(related_person_id, relationship_type)`.
- **Whole-tree rendering**: a materialized/cached ancestor-descendant projection (a `PersonAncestry(personId, ancestorId, depth)` table), rebuilt incrementally on `Relationship` writes (not full-table recompute on every write — recompute only the affected subtree), to back the Whole Banshawali view's generation/branch filters without recursive CTEs on every pan/zoom interaction.
- **Cache invalidation**: any accepted `ChangeRequest` touching a `PARENT_OF`/`ADOPTIVE_PARENT_OF`/`STEP_PARENT_OF` edge triggers recomputation of the ancestry projection rows for the affected person and all of their existing descendants.
- **No graph database.** Recursive SQL plus a materialized projection is sufficient at genealogy scale (hundreds to low thousands of people) and keeps a single query engine and single backup/restore story.

## Integrity Rules

- A person cannot be their own ancestor: enforced by an application-level cycle check before any `PARENT_OF`/`ADOPTIVE_PARENT_OF`/`STEP_PARENT_OF` write is committed (walk the candidate parent's own ancestor chain in the materialized projection; reject if the child appears).
- Duplicate identical relationship edges are prevented by a DB unique constraint on `(personId, relatedPersonId, relationshipType)`, not only an application check as today.
- Date inconsistencies (child born before parent, parent age extremes) are **flagged for review**, not blocked — historical records are frequently imprecise, and Section 10 of the product brief explicitly requires this to be a review signal, not a hard rejection.
- Merges preserve all `SourceCitation`, `AuditEvent`, and `ChangeRequest` references by re-pointing them to the surviving `Person` id and recording the merge itself as an `AuditEvent` with both original ids retained — never a hard delete of the losing record's history.

## Date Representation

Every date field (birth, death, relationship start/end) uses the `{type, valueAd, valueBs, originalEntry, conversionMeta}` structure from `Person` above, applied consistently everywhere a date appears, so that "year only" or "before 1950" records are representable without inventing false precision — this generalizes the current schema's plain `LocalDate birthDate`/`deathDate`, which cannot represent partial or approximate dates at all today.
