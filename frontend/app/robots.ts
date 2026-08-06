import type { MetadataRoute } from "next";

// This is a private family record, not a public site (see [locale]/page.tsx's
// own comment and [locale]/layout.tsx's metadata.robots) -- disallow
// everything for well-behaved crawlers. The per-page <meta name="robots">
// noindex tag is the real backstop for crawlers that ignore this file.
export default function robots(): MetadataRoute.Robots {
  return {
    rules: {
      userAgent: "*",
      disallow: "/",
    },
  };
}
