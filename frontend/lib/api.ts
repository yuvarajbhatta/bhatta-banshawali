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

export interface PersonSummaryDto {
  id: number;
  englishFullName: string;
  nepaliFullName: string;
  generationNumber: number | null;
  gender: string | null;
  birthDate: string | null;
}

export interface FamilySnapshotDto {
  father: PersonSummaryDto | null;
  mother: PersonSummaryDto | null;
  spouses: PersonSummaryDto[];
  children: PersonSummaryDto[];
}

export interface MemberProfileDto {
  email: string;
  linked: boolean;
  person: PersonSummaryDto | null;
  family: FamilySnapshotDto | null;
}

export type MemberProfileResult =
  | { kind: "ok"; profile: MemberProfileDto }
  | { kind: "unauthenticated" }
  | { kind: "no-account" };

// Server-to-server (see API_BASE_URL above), but /api/v1/me is
// session-authenticated -- the caller must forward the browser's own
// Cookie header along, since this app's Node process has no session of
// its own and the backend only recognizes the end user's JSESSIONID.
export async function getMemberProfile(cookieHeader: string): Promise<MemberProfileResult> {
  const response = await fetch(`${API_BASE_URL}/api/v1/me`, {
    headers: { Cookie: cookieHeader },
    cache: "no-store",
  });

  if (response.status === 401 || response.status === 403) {
    return { kind: "unauthenticated" };
  }
  if (response.status === 404) {
    return { kind: "no-account" };
  }
  if (!response.ok) {
    throw new Error(`Failed to load member profile: ${response.status}`);
  }
  return { kind: "ok", profile: await response.json() };
}

export interface PersonDetailDto {
  id: number;
  englishFullName: string;
  nepaliFullName: string;
  nickname: string | null;
  gender: string | null;
  generationNumber: number | null;
  birthDate: string | null;
  deathDate: string | null;
  birthPlace: string | null;
  currentAddress: string | null;
  notes: string | null;
  photoPath: string | null;
  family: FamilySnapshotDto;
}

export type PersonDetailResult =
  | { kind: "ok"; person: PersonDetailDto }
  | { kind: "unauthenticated" }
  | { kind: "not-found" };

// Server-to-server with the browser's session cookie forwarded, same
// reasoning as getMemberProfile above.
export async function getPersonDetail(id: string, cookieHeader: string): Promise<PersonDetailResult> {
  const response = await fetch(`${API_BASE_URL}/api/v1/persons/${encodeURIComponent(id)}`, {
    headers: { Cookie: cookieHeader },
    cache: "no-store",
  });

  if (response.status === 401 || response.status === 403) {
    return { kind: "unauthenticated" };
  }
  if (response.status === 404) {
    return { kind: "not-found" };
  }
  if (!response.ok) {
    throw new Error(`Failed to load person ${id}: ${response.status}`);
  }
  return { kind: "ok", person: await response.json() };
}

export class UnauthenticatedError extends Error {}

// Called directly from the browser as the user types -- a relative path
// so the session cookie rides along automatically (same-origin), same
// as submitSignup above.
export async function searchPersons(keyword: string): Promise<PersonSummaryDto[]> {
  const response = await fetch(`/api/v1/persons?keyword=${encodeURIComponent(keyword)}`);
  if (response.status === 401) {
    throw new UnauthenticatedError("Not signed in.");
  }
  if (!response.ok) {
    throw new Error(`Search failed: ${response.status}`);
  }
  return response.json();
}

export const CORRECTABLE_PERSON_FIELDS = [
  "FIRST_NAME",
  "MIDDLE_NAME",
  "LAST_NAME",
  "FIRST_NAME_NEPALI",
  "MIDDLE_NAME_NEPALI",
  "LAST_NAME_NEPALI",
  "NICKNAME",
  "GENDER",
  "BIRTH_DATE",
  "DEATH_DATE",
  "BIRTH_PLACE",
  "CURRENT_ADDRESS",
  "NOTES",
  "GENERATION_NUMBER",
] as const;

export type CorrectablePersonField = (typeof CORRECTABLE_PERSON_FIELDS)[number];

export interface CorrectionRequest {
  field: CorrectablePersonField;
  proposedValue: string;
  reason: string;
}

export class CorrectionError extends Error {}

// This is a session-authenticated POST, unlike submitSignup (anonymous,
// CSRF-exempt on the backend). The backend issues its CSRF token as a
// plain "XSRF-TOKEN" cookie (CookieCsrfTokenRepository.withHttpOnlyFalse())
// specifically so client-side JS can read it here and echo it back in
// the "X-XSRF-TOKEN" header -- without this, every submission would be
// rejected with 403 regardless of how valid the session is.
function readXsrfTokenCookie(): string | null {
  const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/);
  return match ? decodeURIComponent(match[1]) : null;
}

// Called directly from the browser -- same-origin relative path, same
// reasoning as submitSignup and searchPersons above.
export async function submitCorrection(personId: number, request: CorrectionRequest): Promise<void> {
  const xsrfToken = readXsrfTokenCookie();
  const response = await fetch(`/api/v1/persons/${personId}/corrections`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...(xsrfToken ? { "X-XSRF-TOKEN": xsrfToken } : {}),
    },
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    const body = await response.json().catch(() => null);
    throw new CorrectionError(body?.message ?? "Failed to submit correction.");
  }
}
