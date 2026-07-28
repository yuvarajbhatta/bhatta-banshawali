import type { NextConfig } from "next";
import createNextIntlPlugin from "next-intl/plugin";

const withNextIntl = createNextIntlPlugin();

const nextConfig: NextConfig = {
  // The API lives behind a reverse proxy alongside this app in production
  // (see docs/03-target-architecture.md) rather than being called
  // cross-origin, so no rewrites/proxy config is needed here yet.
  reactStrictMode: true,
};

export default withNextIntl(nextConfig);
