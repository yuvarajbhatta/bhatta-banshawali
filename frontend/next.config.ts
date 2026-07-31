import type { NextConfig } from "next";
import createNextIntlPlugin from "next-intl/plugin";

const withNextIntl = createNextIntlPlugin();

// docs/09-security-threat-model.md item 6 (XSS): the CSP itself now lives in
// proxy.ts (middleware), not here -- it needs a fresh per-request nonce for
// Next.js's own inline hydration scripts, which a static header (as this used
// to be) can't provide. style-src still needs 'unsafe-inline' there because
// TreeCanvas/useFamilyTreeLayout.ts set real inline style={{...}} attributes
// on tree nodes/edges -- a stricter style policy breaks the tree view.

const nextConfig: NextConfig = {
  // The API lives behind a reverse proxy alongside this app in production
  // (see docs/03-target-architecture.md) rather than being called
  // cross-origin, so no rewrites/proxy config is needed here yet.
  reactStrictMode: true,
};

export default withNextIntl(nextConfig);
