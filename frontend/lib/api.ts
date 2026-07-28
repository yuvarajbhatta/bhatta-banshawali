// The backend runs alongside this app on the same host in production (see
// docs/03-target-architecture.md), so this is a server-to-server call, not
// a browser cross-origin request -- no CORS configuration needed.
const API_BASE_URL = process.env.API_BASE_URL ?? "http://127.0.0.1:8080";

export interface ArticleDto {
  slug: string;
  titleEn: string;
  titleNe: string | null;
  bodyEn: string;
  bodyNe: string | null;
  publishedAt: string | null;
}

export interface PublicStatsDto {
  documentedFamilyMembers: number;
  documentedGenerations: number;
  oldestDocumentedGeneration: number | null;
}

export async function getPublicStats(): Promise<PublicStatsDto> {
  const response = await fetch(`${API_BASE_URL}/api/v1/public-stats`, {
    next: { revalidate: 300 },
  });

  if (!response.ok) {
    throw new Error(`Failed to load public stats: ${response.status}`);
  }
  return response.json();
}

export async function getPublishedArticle(slug: string): Promise<ArticleDto | null> {
  const response = await fetch(`${API_BASE_URL}/api/v1/content/${slug}`, {
    // Admin-managed content changes rarely; revalidate periodically rather
    // than caching forever or refetching on every request.
    next: { revalidate: 300 },
  });

  if (response.status === 404) {
    return null;
  }
  if (!response.ok) {
    throw new Error(`Failed to load content "${slug}": ${response.status}`);
  }
  return response.json();
}

export interface SignupRequest {
  email: string;
  fullName: string;
  dobAd: string;
  fatherName: string;
  grandfatherName: string;
  password: string;
  confirmPassword: string;
  preferredLanguage: string;
  agreedToTerms: boolean;
  motherName?: string;
  placeOfBirth?: string;
  ancestralVillage?: string;
  familyBranch?: string;
  knownRelativeName?: string;
  invitationCode?: string;
  applicantNote?: string;
}

export interface SignupResponse {
  status: string;
}

export class SignupError extends Error {}

// Called directly from the browser (not server-to-server), so this is a
// relative path -- nginx proxies /api/ to the backend on the same origin
// in production (see banshawali.yrbhatta.com vhost), same as the live
// AD/BS conversion below.
export async function submitSignup(request: SignupRequest): Promise<SignupResponse> {
  const response = await fetch("/api/v1/signup", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });

  const body = await response.json();
  if (!response.ok) {
    throw new SignupError(body.message ?? "Signup failed.");
  }
  return body;
}

export async function convertAdToBs(dateAd: string): Promise<{ year: number; month: number; day: number }> {
  const response = await fetch(`/api/v1/date-conversion/ad-to-bs?date=${encodeURIComponent(dateAd)}`);
  if (!response.ok) {
    throw new Error(`Failed to convert date: ${response.status}`);
  }
  return response.json();
}
