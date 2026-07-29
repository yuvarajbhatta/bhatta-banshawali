# 11 — Frontend Redesign Plan

Status: supersedes nothing in `01`–`10`; this is an execution plan for one slice of
`08-implementation-roadmap.md` (the visual reskin + Phase 5), written because a very
large, generic "rebuild everything" brief was received and needed to be reconciled
against the architecture already decided and partially built.

## Why this document exists

The incoming brief asked for a from-scratch genealogy SaaS rebuild: a different stack
(Vite/React Router, Zustand, shadcn/ui, Tailwind), a much larger feature surface
(Photos, Events, Messages, Documents, Reports, a 3D "heritage view", admin queues for
seven request types), and a generic dashboard visual language.

That conflicts with decisions already made and already shipped:

- **ADR-001** chose Next.js App Router + Spring Boot REST + MySQL, explicitly
  rejecting a Vite/React SPA split (see `docs/adr/001-frontend-and-backend-architecture.md`).
- **`docs/02-product-requirements.md`** and **`docs/06-ui-ux-specification.md`** define
  this product's actual scope: a verified-membership genealogy record for one family,
  centered on a dashboard, a family tree, person profiles, correction requests, and
  admin verification review. Photos/Events/Messages/Documents/Reports are not in it.
- The backend data model (`Person`, `Relationship` with four types, `UserAccount`,
  `VerificationRequest`, `PersonCorrectionRequest`, `HistoricalArticle`) has no
  entities for those extra modules. Building UI for them would mean designing and
  shipping new backend domains never requested by the product spec.
- Phases 0–4 of `docs/08-implementation-roadmap.md` are already substantially built:
  design tokens, login, signup, landing, public content, dashboard (member + admin),
  member search/directory, person detail pages, correction-request workflow, CSRF.

Decision (confirmed with the user): **continue the real roadmap.** Apply the brief's
visual direction (color system, typography, layout conventions, sidebar/header shell)
to the existing Next.js app, and use this delivery to implement the next roadmap item
that was still outstanding — **Phase 5, the interactive family tree** — for real,
against real data and real privacy rules. Skip Photos/Events/Messages/Documents/
Reports/3D-heritage-view: out of product scope, no backing data model, would be
decorative-only UI otherwise ("fake functionality that appears complete but does
nothing" is explicitly something to avoid).

## Existing architecture (confirmed by reading the repo, not assumed)

- **Backend**: Spring Boot 4 (`pom.xml`), MySQL + Flyway (`V1`..`V9` migrations),
  Spring Security session-cookie auth, `springdoc-openapi`. Layered
  `controller → service → repository → entity`. REST endpoints live under `/api/v1/*`
  (`PersonApiController`, `MemberProfileController`, `SignupController`,
  `DateConversionController`, `PersonCorrectionController`, `AdminSummaryController`,
  admin verification/correction review). Legacy Thymeleaf admin pages
  (`/persons`, `/relationships`, `/lineage`, `/generations`, `/admin/signups`,
  `/admin/corrections`) remain the only UI for Phase 6 (administration) work, which
  hasn't started in Next.js yet — untouched by this delivery.
- **Frontend**: Next.js 16 App Router, TypeScript, `next-intl` (en/ne) with a
  `[locale]` segment, CSS Modules driven by CSS custom-property design tokens
  (`app/globals.css`), Framer Motion + GSAP + `@react-three/fiber`/`drei` already
  installed and used for a small 3D "family constellation" preview on the dashboard.
  No client-state library, no data-fetching library — server components fetch via
  `lib/api.ts` (forwarding the session cookie server-to-server), a few client
  components (`PersonSearch`, `CorrectionForm`) call same-origin relative endpoints
  directly with the browser's own cookie.
- **Auth**: session cookie (`JSESSIONID`) + CSRF via `XSRF-TOKEN` cookie, echoed back
  as `X-XSRF-TOKEN` on mutations. No JWT, no localStorage tokens.
- **Data model**: `Person` (name in EN+NE, gender, generation number, birth/death
  date, birthplace, current address, notes, photo path) and `Relationship` (FATHER,
  MOTHER, SPOUSE, CHILD edges, auto-linked reciprocally, cycle-checked on write). No
  "branch" entity — a `PersonSummaryDto`/`PersonDetailDto` viewer-redaction rule
  already exists (`ViewerContext`/`PersonProfileAssembler`): birth date and current
  address are hidden from everyone except the record's own linked account and admins.

## Proposed architecture (this delivery)

No framework change, no new client-state/data libraries beyond what's needed for the
tree itself. Specifically:

- **Design tokens**: retheme `app/globals.css` to the brief's forest-green heritage
  palette, typography scale, radius/shadow scale — same CSS-custom-property token
  names, new values, so every existing component re-themes without a rewrite.
- **AppShell (new)**: a persistent left sidebar + sticky top header for the
  authenticated section of the app (Dashboard, Family Tree, Members), added as a
  `(app)` Next.js route group so public marketing pages keep the current minimal
  `PageShell`. Mobile: collapses to a slide-out drawer.
- **Family tree data**: one new read-only, privacy-aware REST endpoint,
  `GET /api/v1/family-tree`, added because nothing existing returns the whole graph
  with per-viewer redaction applied (`/lineage/tree` is the closest existing thing,
  but it's the unauthenticated legacy admin-lineage-builder endpoint with no privacy
  filtering — reused as a pattern reference, not reused directly). Built by composing
  existing `PersonService`/`RelationshipService`/`PersonDisplayHelper`/`ViewerContext`
  — no schema change, no new tables.
- **Family tree rendering**: `@xyflow/react` (React Flow) for canvas/pan/zoom/node
  rendering + `@dagrejs/dagre` for automatic hierarchical layout, exactly as already
  decided in `docs/03-target-architecture.md`. This is new frontend surface, not a
  new architectural direction.
- **Three.js**: no new usage. The existing dashboard "family constellation" mini-view
  stays as-is (it already covers the "miniature interactive preview" idea) and gets a
  "View Full Tree" link into the new 2D tree page. No 3D heritage mode — not in
  product scope, and R3F on the dashboard already demonstrates the team can build one
  later if the product spec ever calls for it.

## New page structure

```
app/[locale]/
  (public)/            -- implicit today (no group needed): /, /about, /history, ...
  login, signup/...     -- unchanged
  (app)/                -- NEW route group, shares AppShell layout
    layout.tsx           -- AppShell: Sidebar + TopHeader
    dashboard/            -- moved, unchanged behavior
    directory/            -- moved, unchanged behavior ("Members")
    directory/[id]/        -- moved, unchanged behavior
    tree/                  -- NEW: Phase 5 interactive family tree
      page.tsx
```

Sidebar items: Dashboard, Family Tree, Members — the three real authenticated
sections that exist. Not Photos/Events/Reports/etc. (not built, would be dead links).

## Component structure (new)

```
components/
  shell/
    AppShell.tsx / .module.css      -- layout: sidebar + header + content
    Sidebar.tsx / .module.css       -- desktop nav, active-state styling
    MobileNav.tsx / .module.css     -- slide-out drawer
    TopHeader.tsx / .module.css     -- page title slot, user menu, (search: deferred, see Risks)
    UserMenu.tsx
  family-tree/
    TreeCanvas.tsx                  -- client component, React Flow instance
    MemberNode.tsx / .module.css    -- custom RF node: avatar-initials, name, years, gender marker
    TreeControls.tsx                -- zoom in/out/fit, legend toggle
    TreeLegend.tsx
    MemberQuickView.tsx             -- side drawer on node click/select
    TreeFilters.tsx                 -- generation range, living/deceased
    useFamilyTreeLayout.ts          -- pure fn: FamilyTreeDto -> dagre-laid-out RF nodes/edges (unit-testable)
    familyTree.types.ts
```

## API integration plan

- `lib/api.ts`: add `FamilyTreeDto`/`PersonTreeNodeDto` types and
  `getFamilyTree(cookieHeader)`, following the exact pattern of `getMemberProfile`/
  `getPersonDetail` (server-to-server fetch, cookie forwarded, `cache: "no-store"`,
  401/403 → `{ kind: "unauthenticated" }`).
- Tree page is a server component that authenticates + fetches once; the data is
  handed to a client component (`TreeCanvas`) for interactive layout/pan/zoom — no
  client-side refetching library needed for a dataset this size (whole-family-in-one-
  response, same scale assumption the existing `/lineage/tree` endpoint already makes).
- No new mutation endpoints. The tree is read-only in this delivery (adding/editing
  people and relationships from the tree UI is Phase 6/admin territory, not this
  delivery's scope, and the legacy Thymeleaf tools already do it).

## Authentication flow

Unchanged. Server components read the incoming request's cookies (`next/headers`),
forward them to Spring Boot, and redirect to `/login` on `unauthenticated`, exactly as
`dashboard/page.tsx` and `directory/page.tsx` already do. The new `/tree` page follows
the same guard.

## State management plan

No global client-state library introduced. Local component state
(`useState`/`useReducer`) inside `TreeCanvas` for selection, filters, and viewport;
React Flow owns pan/zoom/node-position state internally. This matches the existing
codebase's choice not to add Zustand/Redux for a handful of interactive components.

## Family-tree rendering approach

1. Backend returns every visible `Person` as a flat node list with parent/spouse/child
   id references (not a nested tree — spouses can co-parent, which a strict nested
   tree can't represent without duplicating nodes).
2. `useFamilyTreeLayout` converts that into Dagre input (one edge per FATHER/MOTHER→
   CHILD pair, a lighter-weight spouse edge that doesn't participate in rank layout),
   runs `dagre.layout()`, and returns React Flow `Node[]`/`Edge[]` with computed x/y.
3. `TreeCanvas` renders it with `@xyflow/react`, a custom `MemberNode` type, `Controls`,
   `Background`, `MiniMap`.
4. Line styling: solid = parent/child (the only biological-link type the data model
   has), a paired/double-style edge = spouse. No dashed/dotted styles for adopted/
   step/guardian relationships — the `RelationshipType` enum doesn't model those, and
   inventing UI for a distinction the backend can't produce would be exactly the "fake
   functionality" the brief itself says to avoid.
5. Filters (generation range, living/deceased) run client-side over the already-
   fetched node list — the dataset size (`docs/03`'s own benchmark targets: 500–10,000)
   is unknown for this specific family but is a single Bhatta family tree, so
   client-side filtering of one full fetch is the pragmatic starting point;
   server-driven viewport-scoped lazy loading (the `docs/03` "hard requirement past a
   few hundred people" note) is flagged as follow-up work once real data volume is
   known, not built speculatively now.

## Responsive design plan

- Sidebar: fixed ~232px desktop, collapses to icon rail ≥768px <1080px, becomes a
  drawer (trigger in `TopHeader`) below 768px — CSS-only breakpoints matching the
  existing `--space-*` scale, no new breakpoint system.
- Tree canvas: full-bleed under the header on mobile, floating zoom/fit controls,
  `MemberQuickView` becomes a bottom sheet instead of a right drawer below 640px.
- Existing pages (dashboard, directory) reflow inside the new shell without content
  changes — the shell is additive chrome around them.

## Migration steps

1. Retheme tokens (`app/globals.css`) — additive value changes only.
2. Add backend endpoint + tests, run `mvn test`.
3. Build `AppShell`/`Sidebar`/`TopHeader`, introduce the `(app)` route group, move
   `dashboard/` and `directory/` folders into it (URL paths unchanged — route groups
   don't affect the path). Update any relative imports.
4. Build the tree feature against the new endpoint.
5. Add "View Full Tree" link from the dashboard's family panel and the sidebar.
6. `npm run lint`, `npm run build`, `mvn test` before calling this done.

## Risks and assumptions

- **Global search** (brief section 5) is deferred: there is no backend search index
  beyond the existing `LIKE`-based `PersonApiController.search`, which is exactly the
  scaling problem `docs/03-target-architecture.md` already flags for replacement with
  indexed normalized-name search. Wiring a header search box to the current endpoint
  is fine and left in as a `TopHeader` affordance reusing `PersonSearch`'s existing
  query logic; a full command-palette / multi-entity grouped search is not built, since
  Photos/Events/Documents don't exist to search across and Members-only search already
  exists at `/directory`.
- **Notifications** (brief section 27): no backing entity or endpoint exists for any
  notification type. Not built. `TopHeader` ships without a notification bell rather
  than a decorative one that never has anything in it.
- **"Family branch"**: not a modeled concept in `Person`/`Relationship`. No branch
  filter, no branch color-coding in the tree. If the family actually wants this,
  it's a real data-model + migration decision for a future phase, not a frontend-only
  addition.
- **Assumed dataset size**: whole-tree-in-one-response is assumed acceptable for now
  (mirrors the existing `/lineage/tree` pattern). If the real Bhatta family dataset
  turns out to be large, the client-side-filter approach above will need to become
  server-driven pagination — flagged, not solved here.
- **Photo rendering**: `Person.photoPath` exists in the schema but no static file
  serving or upload path is wired up anywhere in the current app (checked: no
  `/uploads` mapping, no image rendering on the existing person-detail page either).
  Tree nodes render initials-based avatars, not broken `<img>` tags.
