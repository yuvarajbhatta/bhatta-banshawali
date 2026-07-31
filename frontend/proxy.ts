import { NextRequest, NextResponse } from "next/server";
import createMiddleware from "next-intl/middleware";
import { routing } from "./i18n/routing";

const intlMiddleware = createMiddleware(routing);

// docs/09-security-threat-model.md item 6 (XSS): a static CSP broke the app
// outright -- Next.js's App Router injects its own inline hydration/flight
// scripts on every page, which `script-src 'self'` (no nonce/hash) blocks
// wholesale, caught by a live click-through of the production build rather
// than by any build-time check. A per-request nonce is Next.js's documented
// fix: it's threaded onto framework-managed inline scripts automatically
// once the root layout reads it via headers(), no manual `nonce` prop needed
// for anything this app already renders.
function buildCsp(nonce: string): string {
  return [
    "default-src 'self'",
    `script-src 'self' 'nonce-${nonce}' 'strict-dynamic'`,
    "style-src 'self' 'unsafe-inline'",
    "img-src 'self' data:",
    "connect-src 'self'",
    "frame-ancestors 'none'",
    "base-uri 'self'",
    "form-action 'self'",
  ].join("; ");
}

export default function proxy(request: NextRequest) {
  const nonce = Buffer.from(crypto.randomUUID()).toString("base64");
  const cspHeader = buildCsp(nonce);

  const intlResponse = intlMiddleware(request);

  // A locale redirect renders nothing itself, so there's no downstream
  // request to carry the nonce to -- just tag the redirect response and
  // let the follow-up request (which does render) go through the branch
  // below.
  if (intlResponse.headers.get("location")) {
    intlResponse.headers.set("Content-Security-Policy", cspHeader);
    return intlResponse;
  }

  const requestHeaders = new Headers(request.headers);
  requestHeaders.set("x-nonce", nonce);

  const response = NextResponse.next({ request: { headers: requestHeaders } });
  intlResponse.headers.forEach((value, key) => response.headers.set(key, value));
  response.headers.set("Content-Security-Policy", cspHeader);
  return response;
}

export const config = {
  matcher: ["/((?!api|_next|_vercel|.*\\..*).*)"],
};
