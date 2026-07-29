# FamilyTree Frontend

Next.js (App Router, TypeScript strict) frontend for the Bhatta Banshawali
platform. See `docs/03-target-architecture.md` and
`docs/adr/001-frontend-and-backend-architecture.md` at the repo root for why
this replaces the Thymeleaf UI while the Spring Boot API is kept.

## Status

Through Phase 5 of `docs/08-implementation-roadmap.md`: design system tokens
(`app/globals.css`), public content pages, signup/verification, member
dashboard and profiles, and the interactive family tree (`/tree`,
`@xyflow/react` + Dagre). Administration (Phase 6) is still the legacy
Thymeleaf UI (`/persons`, `/relationships`, `/lineage`, `/admin/*`), linked
from the admin dashboard rather than rebuilt here. See
`docs/frontend-redesign-plan.md` for the reasoning behind that scope and
`docs/08-implementation-roadmap.md` for what's still ahead.

## Development

```bash
npm install
npm run dev      # http://localhost:3000, redirects to /en
npm run lint
npm run test     # vitest -- currently covers the family-tree layout logic
npm run build
```

This app expects to run **behind the same reverse proxy as the Spring Boot
API**, same as production (`docs/03-target-architecture.md`) -- client-side
code calls same-origin relative paths like `/api/v1/...` and `/logout`, which
only resolve correctly when both are served from one origin. Running
`npm run dev` standalone against a bare `localhost:8080` backend will 404 on
every client-side API call (login, search, sign-out, etc.); put a local
reverse proxy (nginx, Caddy, or similar) in front of both dev servers to test
those flows locally.

## Internationalization

- Locales: `en` (default), `ne`. Routing is locale-prefixed (`/en/...`,
  `/ne/...`); `middleware`/`proxy.ts` handles detection and redirects.
- `messages/en.json` and `messages/ne.json` were generated from the
  backend's `messages.properties` / `messages_ne.properties` so existing
  translations didn't need to be redone. A handful of backend keys are used
  both as a label and as a prefix for sub-keys (e.g. `persons.delete` /
  `persons.delete.confirm`) -- flat Java properties allow that, nested JSON
  can't, so those leaves are suffixed `.label` on the frontend only. The
  Java property keys themselves are unchanged.
- New frontend-only copy (not yet in the backend catalog) goes under its
  own namespace, e.g. `landing.*`.

## Design system

Tokens (color, typography, spacing, radius, shadow) live in
`app/globals.css` as CSS custom properties, matching
`docs/06-ui-ux-specification.md`. Components consume the variables rather
than redefining values; add new components as CSS Modules alongside their
`.tsx` file, following `components/Button.tsx` / `Button.module.css`.

## Known issue

`npm audit` currently reports high-severity advisories in `postcss` and
`sharp`, both bundled transitively by Next.js itself for build-time CSS/image
processing, not by anything this app calls directly. Dependabot is
configured to track `frontend/` and will pick up the fix once Next.js ships
one.
