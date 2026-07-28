# 06 — UI/UX Specification

## Design Direction

Elegant, trustworthy, warm, contemporary, spacious, accessible — a cultural heritage platform, not an admin CRUD tool. Subtle cultural motifs (geometric border details, restrained texture) in service of usability, never overriding it. WCAG 2.2 AA target.

## Design System Tokens (Phase 1 deliverable, defined once, reused everywhere)

- **Color**: a small palette — a primary brand hue (heritage-inspired, not a generic Bootstrap blue), a neutral grayscale ramp for text/surfaces, semantic colors for success/warning/error/info, all validated for contrast in both light and dark modes.
- **Typography scale**: one Latin typeface pairing + one Devanagari-compatible typeface (must render Nepali cleanly at all sizes used), a defined scale (e.g., display/h1–h4/body/small/caption).
- **Spacing scale**: 4/8px-based scale, applied consistently (no ad hoc margins as in the current per-page CSS files).
- **Radius, shadow, icon set**: defined once as tokens, not per-component values.
- **Components**: buttons (primary/secondary/ghost/destructive), form fields (with bilingual label + validation message slots), cards, data tables, dialogs, toasts, skeleton loaders, empty states, error states, tree-node styles, focus-visible states for keyboard navigation.

## Sitemap (see also the initial chat response)

```
Public: / · /history · /about-banshawali · /membership · /contact · /privacy · /terms · /login · /signup
Member: /dashboard · /family/{tree|generations|ancestors|descendants|path}
        · /tree (Whole Banshawali) · /search · /people/{id} · /profile · /contributions · /notifications
Admin:  /admin/signups · /admin/people · /admin/relationships · /admin/duplicates
        · /admin/change-requests · /admin/content · /admin/users · /admin/audit-log · /admin/settings
```

## Text Wireframes — Major Pages

### Landing Page (`/`)
```
[Language: EN | NE]                                    [Login] [Request Membership]
─────────────────────────────────────────────────────────────────────────
  BHATTA BANSHAWALI
  A living record of our family's history, preserved and verified.
  [Request Membership]  [Learn About the Banshawali]
─────────────────────────────────────────────────────────────────────────
  About the Banshawali          Family History           How Verification Works
  [admin-managed copy]          [timeline preview]        [3-step explainer]
─────────────────────────────────────────────────────────────────────────
  Public Statistics: N generations documented · N verified profiles · N branches
─────────────────────────────────────────────────────────────────────────
  Footer: About · Privacy · Terms · Contact · Language
```

### Signup (`/signup`) — multi-step
```
Step 1/5  Account            Step 2/5  Identity & Lineage
  Email                        Full name (EN + NE)
  Password / Confirm           DOB (AD) ⇄ DOB (BS)   [synced live, both editable]
  Preferred language           Father's full name
                                Grandfather's full name
                                (optional: mother, birthplace, village, branch, referral, note)
Step 3/5  Review               Step 4/5  Verify Email        Step 5/5  Status
  [summary, edit links]         "Check your inbox"            "Your request is being
                                                                reviewed. We'll notify you."
                                                                (identical wording regardless
                                                                 of match confidence)
```

### Member Dashboard (`/dashboard`)
```
Welcome, {name}          {AD date} · {BS date}         Profile: 70% complete
─────────────────────────────────────────────────────────────────────────
Your Family                          Ancestry Summary
  Father: ...   Mother: ...            Known ancestors: N
  Spouse: ...   Children: ...          Earliest known: {name}, gen {n}
  Siblings: N   Generation: {n}         Paternal line: [path]
  Branch: {name}
─────────────────────────────────────────────────────────────────────────
Quick Actions: View Your Family · Open Whole Banshawali · Search
               Suggest a Correction · Complete Your Profile
─────────────────────────────────────────────────────────────────────────
Featured Story          Timeline Item          Recent Verified Updates
─────────────────────────────────────────────────────────────────────────
Data Quality: 2 fields not visible to other members · Report incorrect data
```

### Your Family (`/family`)
```
Tabs: [Visual Tree] [Generation List] [Ancestors] [Descendants] [Relationship Path]

Visual Tree: centered on you, expand/collapse siblings/parents/children,
             generation labels on the left rail

Relationship Path:  Select person A [You, default]  Select person B [search]
                     → "You → Father → Grandfather → Great-grandfather"
```

### Whole Banshawali (`/tree`)
```
[Search & jump]  [Fit] [Center] [Fullscreen]  [Generation filter] [Branch filter]
[Ancestor-only] [Descendant-only]                              [Minimap: bottom-right]
─────────────────────────────────────────────────────────────────────────
  <interactive canvas, lazy-loaded by generation/branch>
─────────────────────────────────────────────────────────────────────────
Node: [photo/initials] Name (EN) · Name (NE) · b.YYYY–d.YYYY · Gen N
Loading / Empty / Error states shown inline, never an indefinite spinner
```

### Person Profile (`/people/{id}`)
```
[Photo]  Name (EN) / Name (NE)                         [Verification badge]
Born: {AD} / {BS}  {place}     Died: {AD} / {BS}
Branch: {name}   Generation: {n}
─────────────────────────────────────────────────────────────────────────
Parents · Spouse(s) · Children · Siblings         Biography
Source Citations                                   Revision History
[Suggest a Correction]  (visible per viewer's privacy permissions)
```

### Admin — Signup Review (`/admin/signups`)
```
Queue: [Pending] [Needs Info] [Approved] [Rejected]
Row: Applicant name · Stated father/grandfather · Confidence: MEDIUM
     [View Match Evidence] [Approve] [Reject] [Request More Info]

Match Evidence panel: candidate Person records, name-similarity scores,
                       lineage chain comparison, DOB consistency — admin-only
```

## Navigation

- **Public**: Home · Family History · About Banshawali · Membership · Contact · Language · Login.
- **Member**: Dashboard · Your Family · Whole Banshawali · Search · Family History · Contributions · Notifications · Profile & Privacy · Help.
- **Admin/Editor**: separated visually (distinct section, e.g. a top-level "Administration" area) from member navigation — Review Signups · People · Relationships · Duplicates · Change Requests · Content · Users & Roles · Audit Log · Settings.
- **Mobile**: drawer navigation for member/admin nav; bottom-nav pattern considered for the 4–5 most-used member actions (Dashboard, Your Family, Tree, Search, Profile). Nepali labels are measurably wider than their English equivalents (verified against the existing `messages_ne.properties` strings) — all nav components must be tested with Nepali labels active, not just English, before sign-off.

## Progressive Disclosure

Common member flows (view your family, search, view profile) stay simple by default; advanced tools (relationship-path calculator, duplicate resolution, data-quality reports) live behind clear secondary navigation, not on the primary dashboard.

## Role/Permission Matrix (summary)

| Capability | Visitor | Pending | Verified | Editor | Admin | Super Admin |
|---|:---:|:---:|:---:|:---:|:---:|:---:|
| Public pages | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| View own verification status | – | ✓ | ✓ | ✓ | ✓ | ✓ |
| Dashboard / Your Family | – | – | ✓ | ✓ | ✓ | ✓ |
| Whole Banshawali (privacy-filtered) | – | – | ✓ | ✓ | ✓ | ✓ |
| Suggest corrections | – | – | ✓ | ✓ | ✓ | ✓ |
| Add people/relationships (proposed) | – | – | – | ✓ | ✓ | ✓ |
| Approve signups / change requests | – | – | – | – | ✓ | ✓ |
| Merge duplicates | – | – | – | – | ✓ | ✓ |
| Manage users/roles | – | – | – | – | ✓ | ✓ |
| System settings, backups, destructive ops | – | – | – | – | – | ✓ |

All rows enforced server-side (centralized authorization layer), independent of what any client renders.
