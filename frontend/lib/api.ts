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

export class PasswordResetError extends Error {}

// Anonymous, pre-session calls (no login yet) -- same relative-fetch,
// CSRF-exempt pattern as submitSignup above, not adminApiRequest below
// (which assumes an authenticated session and always attaches an
// X-XSRF-TOKEN header).
export async function requestPasswordReset(email: string): Promise<{ status: string }> {
  const response = await fetch("/api/v1/password-reset/request", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email }),
  });

  const body = await response.json();
  if (!response.ok) {
    throw new PasswordResetError(body.message ?? "Password reset request failed.");
  }
  return body;
}

export async function confirmPasswordReset(
  token: string,
  newPassword: string,
  confirmNewPassword: string,
): Promise<{ status: string }> {
  const response = await fetch("/api/v1/password-reset/confirm", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ token, newPassword, confirmNewPassword }),
  });

  const body = await response.json();
  if (!response.ok) {
    throw new PasswordResetError(body.message ?? "Password reset failed.");
  }
  return body;
}

export class EmailVerificationError extends Error {}

export async function confirmEmailVerification(token: string): Promise<{ status: string }> {
  const response = await fetch("/api/v1/verify-email/confirm", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ token }),
  });

  const body = await response.json();
  if (!response.ok) {
    throw new EmailVerificationError(body.message ?? "Email verification failed.");
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

export async function convertBsToAd(year: number, month: number, day: number): Promise<{ date: string }> {
  const response = await fetch(
    `/api/v1/date-conversion/bs-to-ad?year=${year}&month=${month}&day=${day}`,
  );
  if (!response.ok) {
    throw new Error(`Failed to convert date: ${response.status}`);
  }
  return response.json();
}

// Server-side counterpart of convertAdToBs, for use in server components
// rendering a BS date alongside an AD one. Some AD dates fall outside the
// converter's supported BS range, so this resolves to null instead of
// throwing -- callers should fall back to showing the AD date alone.
export async function getBsDateForAd(
  dateAd: string,
): Promise<{ year: number; month: number; day: number } | null> {
  const response = await fetch(
    `${API_BASE_URL}/api/v1/date-conversion/ad-to-bs?date=${encodeURIComponent(dateAd)}`,
  );
  if (!response.ok) {
    return null;
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
  // Only populated in search results (see PersonProfileAssembler.summarizeForSearch
  // on the backend) -- father's name, so a picker choosing between several
  // same-named people can tell them apart.
  parentHint: string | null;
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
  // gotra/memberSince/pendingCorrectionCount describe the account and
  // its linked Person, not the family snapshot -- memberSince and
  // pendingCorrectionCount are populated even when unlinked.
  gotra: string | null;
  memberSince: string;
  pendingCorrectionCount: number;
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
  gotra: string | null;
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

// One photo in a person's picture album -- deliberately doesn't include
// who uploaded it (see PersonPhotoDto's backend doc comment); canDelete
// already answers what the current viewer needs to know.
export interface PersonPhotoDto {
  id: number;
  caption: string | null;
  uploadedAt: string;
  canDelete: boolean;
}

// Server-to-server with the browser's session cookie forwarded, same
// reasoning as getPersonDetail above.
export async function getPersonPhotos(personId: number, cookieHeader: string): Promise<PersonPhotoDto[]> {
  const response = await fetch(`${API_BASE_URL}/api/v1/persons/${personId}/photos`, {
    headers: { Cookie: cookieHeader },
    cache: "no-store",
  });

  if (!response.ok) {
    throw new Error(`Failed to load photos for person ${personId}: ${response.status}`);
  }
  return response.json();
}

export class PhotoUploadError extends Error {}

// Called directly from the browser (file input), same CSRF pattern as
// submitCorrection below -- FormData, not JSON, so Content-Type is left
// for the browser to set itself (it needs to include the multipart
// boundary, which a hand-set header can't provide).
export async function uploadPersonPhoto(personId: number, file: File, caption: string): Promise<PersonPhotoDto> {
  const xsrfToken = readXsrfTokenCookie();
  const formData = new FormData();
  formData.append("file", file);
  if (caption.trim()) {
    formData.append("caption", caption.trim());
  }

  const response = await fetch(`/api/v1/persons/${personId}/photos`, {
    method: "POST",
    headers: xsrfToken ? { "X-XSRF-TOKEN": xsrfToken } : undefined,
    body: formData,
  });

  if (!response.ok) {
    const body = await response.json().catch(() => null);
    throw new PhotoUploadError(body?.message ?? "Failed to upload photo.");
  }
  return response.json();
}

export async function deletePersonPhoto(personId: number, photoId: number): Promise<void> {
  const xsrfToken = readXsrfTokenCookie();
  const response = await fetch(`/api/v1/persons/${personId}/photos/${photoId}`, {
    method: "DELETE",
    headers: xsrfToken ? { "X-XSRF-TOKEN": xsrfToken } : undefined,
  });

  if (!response.ok) {
    const body = await response.json().catch(() => null);
    throw new PhotoUploadError(body?.message ?? "Failed to remove photo.");
  }
}

// Same-origin relative URL -- authenticated GET, browser's session
// cookie rides along automatically, same as any other /api/v1/** call
// made directly from client code.
export function personPhotoFileUrl(personId: number, photoId: number): string {
  return `/api/v1/persons/${personId}/photos/${photoId}/file`;
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
  "GOTRA",
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
export function readXsrfTokenCookie(): string | null {
  const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/);
  return match?.[1] ? decodeURIComponent(match[1]) : null;
}

// Every authenticated page's data (member profile, admin summary, etc.)
// is fetched server-to-server from Next.js's own Node process, forwarding
// the browser's Cookie header -- but any Set-Cookie the backend sends
// back on *those* requests is only ever seen by that internal fetch call
// and never reaches the actual browser. So the browser's XSRF-TOKEN
// cookie can go stale (e.g. right after login) with nothing to refresh
// it until the user's first real mutating click, which then fails once
// before self-healing on retry. Call this once on mount in a client
// component (AppShell) to force one genuine browser->backend round trip
// -- CsrfCookieFilter runs on every request through the security filter
// chain, so even hitting a permitAll, no-op endpoint like this refreshes
// the cookie before the user has a chance to click anything.
export function warmCsrfCookie(): void {
  fetch("/actuator/health", { cache: "no-store" }).catch(() => {
    // Best-effort -- a failure here just means the old self-healing
    // (fail once, retry) behavior on the first real mutation still applies.
  });
}

// Spring Security's default logout endpoint (SecurityConfig: logoutSuccessUrl
// "/login?logout"). A plain POST with the CSRF header, same pattern as
// submitCorrection -- the browser's own session cookie rides along
// same-origin. Caller is responsible for navigating to /login afterwards
// (see UserMenu), since this only performs the server-side logout.
export async function signOut(): Promise<void> {
  const xsrfToken = readXsrfTokenCookie();
  const response = await fetch("/logout", {
    method: "POST",
    headers: xsrfToken ? { "X-XSRF-TOKEN": xsrfToken } : undefined,
  });

  if (!response.ok && response.status !== 302) {
    throw new Error(`Sign out failed: ${response.status}`);
  }
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

// Mirrors com.familytree.dto.MyAdminAccessRequestStatusDto.
export type MyAdminAccessRequestStatus = "NONE" | "PENDING" | "ALREADY_ADMIN";

export type MyAdminAccessRequestStatusResult =
  | { kind: "ok"; status: MyAdminAccessRequestStatus }
  | { kind: "unauthenticated" }
  | { kind: "no-account" };

// Server-to-server with the browser's session cookie forwarded, same
// pattern as getMemberProfile -- "no-account" covers a legacy AppUser
// (admin) login, which already has admin access another way and has
// nothing to request.
export async function getMyAdminAccessRequestStatus(cookieHeader: string): Promise<MyAdminAccessRequestStatusResult> {
  const response = await fetch(`${API_BASE_URL}/api/v1/me/admin-access-request`, {
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
    throw new Error(`Failed to load admin access request status: ${response.status}`);
  }
  const body: { status: MyAdminAccessRequestStatus } = await response.json();
  return { kind: "ok", status: body.status };
}

export class AdminAccessRequestError extends Error {}

// Called directly from the browser -- same CSRF pattern as submitCorrection.
export async function requestAdminAccess(): Promise<void> {
  const xsrfToken = readXsrfTokenCookie();
  const response = await fetch("/api/v1/me/admin-access-request", {
    method: "POST",
    headers: xsrfToken ? { "X-XSRF-TOKEN": xsrfToken } : undefined,
  });

  if (!response.ok) {
    const body = await response.json().catch(() => null);
    throw new AdminAccessRequestError(body?.message ?? "Could not submit the request.");
  }
}

export interface AdminSummaryDto {
  pendingSignupCount: number;
  pendingCorrectionCount: number;
  pendingAdminAccessRequestCount: number;
  recentPendingSignups: { id: number; submittedFullName: string; submittedAt: string }[];
  recentPendingCorrections: { id: number; personName: string; field: string; submittedAt: string }[];
}

// Server-to-server with the browser's session cookie forwarded, same
// pattern as getMemberProfile/getPersonDetail. Only ever called for an
// admin session (dashboard checks this before rendering the admin
// view), so a non-403/401 failure here is a genuine error, not an
// expected "not an admin" case.
export async function getAdminSummary(cookieHeader: string): Promise<AdminSummaryDto | null> {
  const response = await fetch(`${API_BASE_URL}/api/v1/admin/summary`, {
    headers: { Cookie: cookieHeader },
    cache: "no-store",
  });

  if (response.status === 401 || response.status === 403) {
    return null;
  }
  if (!response.ok) {
    throw new Error(`Failed to load admin summary: ${response.status}`);
  }
  return response.json();
}

// Mirrors com.familytree.dto.PersonTreeNodeDto / FamilyTreeDto
// (GET /api/v1/family-tree, docs/08 Phase 5). birthDate follows the same
// per-viewer redaction rule as PersonSummaryDto -- null unless the
// viewer is an admin or this is the viewer's own linked person.
export interface PersonTreeNodeDto {
  id: number;
  englishFullName: string;
  nepaliFullName: string;
  gender: string | null;
  generationNumber: number | null;
  birthDate: string | null;
  deathDate: string | null;
  fatherId: number | null;
  motherId: number | null;
  spouseIds: number[];
  childIds: number[];
}

export interface FamilyTreeDto {
  nodes: PersonTreeNodeDto[];
  rootPersonId: number | null;
}

export type FamilyTreeResult =
  | { kind: "ok"; tree: FamilyTreeDto }
  | { kind: "unauthenticated" };

// Server-to-server with the browser's session cookie forwarded, same
// pattern as getMemberProfile/getPersonDetail.
export interface FamilyTreeWindow {
  minGeneration?: number;
  maxGeneration?: number;
}

function familyTreeWindowQuery(window?: FamilyTreeWindow): string {
  if (!window) return "";
  const params = new URLSearchParams();
  if (window.minGeneration != null) params.set("minGeneration", String(window.minGeneration));
  if (window.maxGeneration != null) params.set("maxGeneration", String(window.maxGeneration));
  const query = params.toString();
  return query ? `?${query}` : "";
}

export async function getFamilyTree(cookieHeader: string, window?: FamilyTreeWindow): Promise<FamilyTreeResult> {
  const response = await fetch(`${API_BASE_URL}/api/v1/family-tree${familyTreeWindowQuery(window)}`, {
    headers: { Cookie: cookieHeader },
    cache: "no-store",
  });

  if (response.status === 401 || response.status === 403) {
    return { kind: "unauthenticated" };
  }
  if (!response.ok) {
    throw new Error(`Failed to load family tree: ${response.status}`);
  }
  return { kind: "ok", tree: await response.json() };
}

// Mirrors the backend enums exactly (com.familytree.entity.*).
export type VerificationStatus = "PENDING" | "APPROVED" | "REJECTED" | "NEEDS_MORE_INFO";
export type MatchConfidence = "HIGH" | "MEDIUM" | "LOW";
export type CorrectionRequestStatus = "PENDING" | "APPROVED" | "REJECTED";

// Mirrors com.familytree.dto.AdminSignupSummaryDto / AdminSignupDetailDto
// (docs/08 Phase 6 admin review queues).
export interface AdminSignupSummaryDto {
  id: number;
  submittedFullName: string;
  submittedFatherName: string;
  submittedGrandfatherName: string;
  matchConfidence: MatchConfidence;
  status: VerificationStatus;
  createdAt: string;
}

// One candidate on the signup review screen. ancestorChain walks the
// FATHER line as far back as it's recorded, starting with person itself
// -- this is what lets the admin tell apart several same-named
// candidates by their actual lineage instead of a bare ID.
export interface MatchCandidateDto {
  person: PersonSummaryDto;
  ancestorChain: PersonSummaryDto[];
}

export interface AdminSignupDetailDto {
  id: number;
  submittedFullName: string;
  submittedFullNameNepali: string | null;
  submittedFatherName: string;
  submittedGrandfatherName: string;
  submittedDobAd: string | null;
  submittedDobBsYear: number | null;
  submittedDobBsMonth: number | null;
  submittedDobBsDay: number | null;
  motherName: string | null;
  placeOfBirth: string | null;
  ancestralVillage: string | null;
  familyBranch: string | null;
  knownRelativeName: string | null;
  invitationCode: string | null;
  applicantNote: string | null;
  matchConfidence: MatchConfidence;
  status: VerificationStatus;
  reviewedByUsername: string | null;
  reviewedAt: string | null;
  decisionNote: string | null;
  createdAt: string;
  candidates: MatchCandidateDto[];
  // Existing Persons found by matching the submitted father's name --
  // selecting one creates a brand-new Person for this applicant as that
  // father's child, rather than linking to an existing record.
  fatherCandidates: MatchCandidateDto[];
}

// Mirrors com.familytree.dto.AdminCorrectionSummaryDto.
export interface AdminCorrectionSummaryDto {
  id: number;
  personId: number;
  personName: string;
  field: CorrectablePersonField;
  currentValueSnapshot: string | null;
  proposedValue: string;
  reason: string;
  submittedByEmail: string;
  submittedAt: string;
  status: CorrectionRequestStatus;
  reviewedByUsername: string | null;
  reviewedAt: string | null;
  decisionNote: string | null;
}

export type AdminListResult<T> =
  | { kind: "ok"; items: T[] }
  | { kind: "unauthenticated" }
  | { kind: "forbidden" };

export type AdminDetailResult<T> =
  | { kind: "ok"; detail: T }
  | { kind: "unauthenticated" }
  | { kind: "forbidden" }
  | { kind: "not-found" };

// Server-to-server with the browser's session cookie forwarded, same
// pattern as getAdminSummary -- "forbidden" (a 403) is an expected,
// distinct outcome from "unauthenticated" (a 401): a signed-in
// non-admin member hitting an admin-only page, not a missing session.
export async function getAdminSignups(cookieHeader: string, status?: VerificationStatus): Promise<AdminListResult<AdminSignupSummaryDto>> {
  const query = status ? `?status=${status}` : "";
  const response = await fetch(`${API_BASE_URL}/api/v1/admin/signups${query}`, {
    headers: { Cookie: cookieHeader },
    cache: "no-store",
  });

  if (response.status === 401) return { kind: "unauthenticated" };
  if (response.status === 403) return { kind: "forbidden" };
  if (!response.ok) throw new Error(`Failed to load signup requests: ${response.status}`);
  return { kind: "ok", items: await response.json() };
}

export async function getAdminSignupDetail(cookieHeader: string, id: string): Promise<AdminDetailResult<AdminSignupDetailDto>> {
  const response = await fetch(`${API_BASE_URL}/api/v1/admin/signups/${encodeURIComponent(id)}`, {
    headers: { Cookie: cookieHeader },
    cache: "no-store",
  });

  if (response.status === 401) return { kind: "unauthenticated" };
  if (response.status === 403) return { kind: "forbidden" };
  if (response.status === 404) return { kind: "not-found" };
  if (!response.ok) throw new Error(`Failed to load signup request: ${response.status}`);
  return { kind: "ok", detail: await response.json() };
}

export async function getAdminCorrections(cookieHeader: string, status?: CorrectionRequestStatus): Promise<AdminListResult<AdminCorrectionSummaryDto>> {
  const query = status ? `?status=${status}` : "";
  const response = await fetch(`${API_BASE_URL}/api/v1/admin/corrections${query}`, {
    headers: { Cookie: cookieHeader },
    cache: "no-store",
  });

  if (response.status === 401) return { kind: "unauthenticated" };
  if (response.status === 403) return { kind: "forbidden" };
  if (!response.ok) throw new Error(`Failed to load correction requests: ${response.status}`);
  return { kind: "ok", items: await response.json() };
}

export interface AdminSignupDecisionRequest {
  decisionNote?: string;
  linkedPersonId?: number;
  createAsChildOfFatherId?: number;
}

export interface AdminDecisionRequest {
  decisionNote?: string;
}

export class AdminActionError extends Error {}

// Called directly from the browser -- same CSRF pattern as
// submitCorrection. Every admin mutation (decision actions, person/
// relationship CRUD) shares this one helper. A 204 (delete) has no
// body to parse, so callers that don't need a return value pass
// `void` as T and this resolves to undefined instead of calling
// response.json() on an empty body.
async function adminApiRequest<T>(path: string, method: "POST" | "PUT" | "DELETE", body?: unknown): Promise<T> {
  const xsrfToken = readXsrfTokenCookie();
  const response = await fetch(path, {
    method,
    headers: {
      ...(body !== undefined ? { "Content-Type": "application/json" } : {}),
      ...(xsrfToken ? { "X-XSRF-TOKEN": xsrfToken } : {}),
    },
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  if (!response.ok) {
    const errorBody = await response.json().catch(() => null);
    throw new AdminActionError(errorBody?.message ?? `Request failed: ${response.status}`);
  }
  if (response.status === 204) {
    return undefined as T;
  }
  return response.json();
}

export function approveSignup(id: number, body: AdminSignupDecisionRequest): Promise<AdminSignupDetailDto> {
  return adminApiRequest(`/api/v1/admin/signups/${id}/approve`, "POST", body);
}

export function rejectSignup(id: number, body: AdminSignupDecisionRequest): Promise<AdminSignupDetailDto> {
  return adminApiRequest(`/api/v1/admin/signups/${id}/reject`, "POST", body);
}

export function requestMoreInfoSignup(id: number, body: AdminSignupDecisionRequest): Promise<AdminSignupDetailDto> {
  return adminApiRequest(`/api/v1/admin/signups/${id}/request-more-info`, "POST", body);
}

export function approveCorrection(id: number, body: AdminDecisionRequest): Promise<AdminCorrectionSummaryDto> {
  return adminApiRequest(`/api/v1/admin/corrections/${id}/approve`, "POST", body);
}

export function rejectCorrection(id: number, body: AdminDecisionRequest): Promise<AdminCorrectionSummaryDto> {
  return adminApiRequest(`/api/v1/admin/corrections/${id}/reject`, "POST", body);
}

// Mirrors com.familytree.dto.AdminPersonDto / AdminPersonRequestDto.
export interface AdminPersonDto {
  id: number;
  generationNumber: number | null;
  firstName: string;
  firstNameNepali: string | null;
  middleName: string | null;
  middleNameNepali: string | null;
  lastName: string;
  lastNameNepali: string | null;
  nickname: string | null;
  gender: string | null;
  birthDate: string | null;
  deathDate: string | null;
  photoPath: string | null;
  birthPlace: string | null;
  currentAddress: string | null;
  gotra: string | null;
  notes: string | null;
}

export type AdminPersonRequest = Omit<AdminPersonDto, "id">;

export async function getAdminPersons(cookieHeader: string, keyword?: string): Promise<AdminListResult<AdminPersonDto>> {
  const query = keyword ? `?keyword=${encodeURIComponent(keyword)}` : "";
  const response = await fetch(`${API_BASE_URL}/api/v1/admin/persons${query}`, {
    headers: { Cookie: cookieHeader },
    cache: "no-store",
  });

  if (response.status === 401) return { kind: "unauthenticated" };
  if (response.status === 403) return { kind: "forbidden" };
  if (!response.ok) throw new Error(`Failed to load persons: ${response.status}`);
  return { kind: "ok", items: await response.json() };
}

export async function getAdminPersonDetail(cookieHeader: string, id: string): Promise<AdminDetailResult<AdminPersonDto>> {
  const response = await fetch(`${API_BASE_URL}/api/v1/admin/persons/${encodeURIComponent(id)}`, {
    headers: { Cookie: cookieHeader },
    cache: "no-store",
  });

  if (response.status === 401) return { kind: "unauthenticated" };
  if (response.status === 403) return { kind: "forbidden" };
  if (response.status === 404) return { kind: "not-found" };
  if (!response.ok) throw new Error(`Failed to load person: ${response.status}`);
  return { kind: "ok", detail: await response.json() };
}

export function createAdminPerson(body: AdminPersonRequest): Promise<AdminPersonDto> {
  return adminApiRequest("/api/v1/admin/persons", "POST", body);
}

export function updateAdminPerson(id: number, body: AdminPersonRequest): Promise<AdminPersonDto> {
  return adminApiRequest(`/api/v1/admin/persons/${id}`, "PUT", body);
}

export function deleteAdminPerson(id: number): Promise<void> {
  return adminApiRequest(`/api/v1/admin/persons/${id}`, "DELETE");
}

// Mirrors com.familytree.dto.AdminRelationshipDto / AdminRelationshipRequestDto.
export interface AdminRelationshipDto {
  id: number;
  personId: number;
  personName: string;
  relatedPersonId: number;
  relatedPersonName: string;
  relationshipType: "FATHER" | "MOTHER" | "SPOUSE" | "CHILD";
}

export interface AdminRelationshipRequest {
  personId: number;
  relatedPersonId: number;
  relationshipType: AdminRelationshipDto["relationshipType"];
}

export async function getAdminRelationships(cookieHeader: string): Promise<AdminListResult<AdminRelationshipDto>> {
  const response = await fetch(`${API_BASE_URL}/api/v1/admin/relationships`, {
    headers: { Cookie: cookieHeader },
    cache: "no-store",
  });

  if (response.status === 401) return { kind: "unauthenticated" };
  if (response.status === 403) return { kind: "forbidden" };
  if (!response.ok) throw new Error(`Failed to load relationships: ${response.status}`);
  return { kind: "ok", items: await response.json() };
}

export function createAdminRelationship(body: AdminRelationshipRequest): Promise<AdminRelationshipDto> {
  return adminApiRequest("/api/v1/admin/relationships", "POST", body);
}

export function deleteAdminRelationship(id: number): Promise<void> {
  return adminApiRequest(`/api/v1/admin/relationships/${id}`, "DELETE");
}

// Mirrors com.familytree.dto.AuditLogEntryDto.
export interface AuditLogEntryDto {
  id: number;
  actorUsername: string;
  action: string;
  entityType: string;
  entityId: number | null;
  summary: string;
  createdAt: string;
}

export async function getAdminAuditLog(cookieHeader: string, limit?: number): Promise<AdminListResult<AuditLogEntryDto>> {
  const query = limit ? `?limit=${limit}` : "";
  const response = await fetch(`${API_BASE_URL}/api/v1/admin/audit-log${query}`, {
    headers: { Cookie: cookieHeader },
    cache: "no-store",
  });

  if (response.status === 401) return { kind: "unauthenticated" };
  if (response.status === 403) return { kind: "forbidden" };
  if (!response.ok) throw new Error(`Failed to load audit log: ${response.status}`);
  return { kind: "ok", items: await response.json() };
}

// Mirrors com.familytree.dto.AdminUserAccountDto -- every UserAccount
// regardless of status or link state, combining what used to be split
// across a separate "unlinked accounts" tool and this one -- "Manage
// User Accounts" is the one place to link/unlink, correct the
// applicant's submitted signup info, disable/enable, or delete.
export type UserAccountStatus = "PENDING_EMAIL_VERIFICATION" | "ACTIVE" | "LOCKED" | "DISABLED";

export interface AdminUserAccountDto {
  id: number;
  email: string;
  status: UserAccountStatus;
  preferredLanguage: string | null;
  createdAt: string;
  lastLoginAt: string | null;
  isAdmin: boolean;
  linkedPersonId: number | null;
  linkedPersonName: string | null;
  submittedFullName: string | null;
  submittedFatherName: string | null;
  submittedMotherName: string | null;
  submittedGrandfatherName: string | null;
  submittedDobAd: string | null;
}

export async function getAdminAccounts(cookieHeader: string): Promise<AdminListResult<AdminUserAccountDto>> {
  const response = await fetch(`${API_BASE_URL}/api/v1/admin/accounts`, {
    headers: { Cookie: cookieHeader },
    cache: "no-store",
  });

  if (response.status === 401) return { kind: "unauthenticated" };
  if (response.status === 403) return { kind: "forbidden" };
  if (!response.ok) throw new Error(`Failed to load accounts: ${response.status}`);
  return { kind: "ok", items: await response.json() };
}

export function linkAdminAccount(id: number, personId: number): Promise<void> {
  return adminApiRequest(`/api/v1/admin/accounts/${id}/link`, "POST", { personId });
}

export function unlinkAdminAccount(id: number): Promise<void> {
  return adminApiRequest(`/api/v1/admin/accounts/${id}/unlink`, "POST");
}

export interface AdminAccountSignupInfoUpdateRequest {
  fullName: string;
  fatherName: string;
  motherName?: string;
  grandfatherName: string;
  dobAd?: string;
}

export function updateAdminAccountSignupInfo(id: number, body: AdminAccountSignupInfoUpdateRequest): Promise<void> {
  return adminApiRequest(`/api/v1/admin/accounts/${id}/signup-info`, "PUT", body);
}

// Copies the account's submitted signup name/DOB onto its linked
// Person record. Father's/mother's/grandfather's names aren't included
// -- Person has no fields for them (parentage is Relationship edges,
// not strings), so those stay informational-only; wiring up the actual
// family graph is a manual step via the Relationships tool.
export function applyAdminAccountSignupInfoToPerson(id: number): Promise<void> {
  return adminApiRequest(`/api/v1/admin/accounts/${id}/apply-signup-info-to-person`, "POST");
}

export function revokeAdminAccessForAccount(id: number): Promise<void> {
  return adminApiRequest(`/api/v1/admin/accounts/${id}/revoke-admin`, "POST");
}

export function disableAdminAccount(id: number): Promise<void> {
  return adminApiRequest(`/api/v1/admin/accounts/${id}/disable`, "POST");
}

export function enableAdminAccount(id: number): Promise<void> {
  return adminApiRequest(`/api/v1/admin/accounts/${id}/enable`, "POST");
}

export function deleteAdminAccount(id: number): Promise<void> {
  return adminApiRequest(`/api/v1/admin/accounts/${id}`, "DELETE");
}

// Mirrors com.familytree.dto.AdminAccessRequestDto.
export interface AdminAccessRequestDto {
  id: number;
  userAccountId: number;
  email: string;
  linkedPersonName: string | null;
  requestedAt: string;
}

export async function getAdminAccessRequests(cookieHeader: string): Promise<AdminListResult<AdminAccessRequestDto>> {
  const response = await fetch(`${API_BASE_URL}/api/v1/admin/admin-access-requests`, {
    headers: { Cookie: cookieHeader },
    cache: "no-store",
  });

  if (response.status === 401) return { kind: "unauthenticated" };
  if (response.status === 403) return { kind: "forbidden" };
  if (!response.ok) throw new Error(`Failed to load admin access requests: ${response.status}`);
  return { kind: "ok", items: await response.json() };
}

export function approveAdminAccessRequest(id: number): Promise<void> {
  return adminApiRequest(`/api/v1/admin/admin-access-requests/${id}/approve`, "POST");
}

export function denyAdminAccessRequest(id: number): Promise<void> {
  return adminApiRequest(`/api/v1/admin/admin-access-requests/${id}/deny`, "POST");
}

// Mirrors com.familytree.dto.AdminArticleDto / AdminArticleRequestDto --
// admin CRUD + draft/review/publish workflow for HistoricalArticle. The
// public, published-only read path stays on getPublishedArticle above.
export type ArticleStatus = "DRAFT" | "IN_REVIEW" | "PUBLISHED" | "UNPUBLISHED";

export interface AdminArticleDto {
  id: number;
  slug: string;
  titleEn: string;
  titleNe: string | null;
  bodyEn: string;
  bodyNe: string | null;
  status: ArticleStatus;
  publishedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface AdminArticleRequest {
  slug: string;
  titleEn: string;
  titleNe?: string;
  bodyEn: string;
  bodyNe?: string;
}

export async function getAdminContent(cookieHeader: string): Promise<AdminListResult<AdminArticleDto>> {
  const response = await fetch(`${API_BASE_URL}/api/v1/admin/content`, {
    headers: { Cookie: cookieHeader },
    cache: "no-store",
  });

  if (response.status === 401) return { kind: "unauthenticated" };
  if (response.status === 403) return { kind: "forbidden" };
  if (!response.ok) throw new Error(`Failed to load content: ${response.status}`);
  return { kind: "ok", items: await response.json() };
}

export function createAdminArticle(body: AdminArticleRequest): Promise<AdminArticleDto> {
  return adminApiRequest("/api/v1/admin/content", "POST", body);
}

export function updateAdminArticle(id: number, body: AdminArticleRequest): Promise<AdminArticleDto> {
  return adminApiRequest(`/api/v1/admin/content/${id}`, "PUT", body);
}

export function submitArticleForReview(id: number): Promise<AdminArticleDto> {
  return adminApiRequest(`/api/v1/admin/content/${id}/submit-for-review`, "POST");
}

export function publishArticle(id: number): Promise<AdminArticleDto> {
  return adminApiRequest(`/api/v1/admin/content/${id}/publish`, "POST");
}

export function unpublishArticle(id: number): Promise<AdminArticleDto> {
  return adminApiRequest(`/api/v1/admin/content/${id}/unpublish`, "POST");
}

export function revertArticleToDraft(id: number): Promise<AdminArticleDto> {
  return adminApiRequest(`/api/v1/admin/content/${id}/revert-to-draft`, "POST");
}

export function deleteAdminArticle(id: number): Promise<void> {
  return adminApiRequest(`/api/v1/admin/content/${id}`, "DELETE");
}

// Mirrors com.familytree.entity.AnnouncementCategory/AnnouncementStatus
// and the Announcement/AdminAnnouncement DTOs -- News & Alerts. Simpler
// draft/published workflow than articles (no IN_REVIEW): see
// AnnouncementPost's doc comment.
export type AnnouncementCategory = "APP_UPDATE" | "FAMILY_NEWS" | "CELEBRATION" | "OBITUARY" | "HELP_REQUEST";
export type AnnouncementStatus = "DRAFT" | "PUBLISHED";

export interface AnnouncementPhotoDto {
  id: number;
  caption: string | null;
  uploadedAt: string;
}

export interface AnnouncementDto {
  id: number;
  category: AnnouncementCategory;
  titleEn: string;
  titleNe: string | null;
  bodyEn: string;
  bodyNe: string | null;
  pinned: boolean;
  publishedAt: string;
  photos: AnnouncementPhotoDto[];
}

export interface AdminAnnouncementDto {
  id: number;
  category: AnnouncementCategory;
  titleEn: string;
  titleNe: string | null;
  bodyEn: string;
  bodyNe: string | null;
  status: AnnouncementStatus;
  pinned: boolean;
  publishedAt: string | null;
  createdAt: string;
  updatedAt: string;
  photos: AnnouncementPhotoDto[];
}

export interface AdminAnnouncementRequest {
  category: AnnouncementCategory;
  titleEn: string;
  titleNe?: string;
  bodyEn: string;
  bodyNe?: string;
  pinned: boolean;
}

// Server-to-server, same pattern as getPublishedArticle/getAdminSummary
// above -- authenticated (any member), not admin-gated.
export async function getAnnouncements(cookieHeader: string): Promise<AnnouncementDto[]> {
  const response = await fetch(`${API_BASE_URL}/api/v1/announcements`, {
    headers: { Cookie: cookieHeader },
    cache: "no-store",
  });

  if (!response.ok) {
    throw new Error(`Failed to load announcements: ${response.status}`);
  }
  return response.json();
}

// Mirrors com.familytree.dto.AdminContactDto -- who to reach for help,
// shown on the Help & Contact page's Contact section. Any authenticated
// member can read this (not admin-gated), same as getAnnouncements above.
export interface AdminContactDto {
  displayName: string;
  email: string | null;
}

export async function getAdminContacts(cookieHeader: string): Promise<AdminContactDto[]> {
  const response = await fetch(`${API_BASE_URL}/api/v1/admin-contacts`, {
    headers: { Cookie: cookieHeader },
    cache: "no-store",
  });

  if (!response.ok) {
    throw new Error(`Failed to load admin contacts: ${response.status}`);
  }
  return response.json();
}

// Cheap enough to call on every authenticated page load, same reasoning
// as getAdminSummary -- feeds the News & Alerts nav badge. Resolves to 0
// rather than throwing on any non-ok response so a hiccup here never
// breaks the shared layout for an unrelated page.
export async function getAnnouncementUnreadCount(cookieHeader: string): Promise<number> {
  const response = await fetch(`${API_BASE_URL}/api/v1/announcements/unread-count`, {
    headers: { Cookie: cookieHeader },
    cache: "no-store",
  });

  if (!response.ok) {
    return 0;
  }
  const body: { unreadCount: number } = await response.json();
  return body.unreadCount;
}

// Called directly from the browser when the News tab is opened, same
// CSRF pattern as submitCorrection.
export async function markAnnouncementsSeen(): Promise<void> {
  const xsrfToken = readXsrfTokenCookie();
  await fetch("/api/v1/announcements/mark-seen", {
    method: "POST",
    headers: xsrfToken ? { "X-XSRF-TOKEN": xsrfToken } : undefined,
  });
}

// Same-origin relative URL -- authenticated GET, browser's session
// cookie rides along automatically.
export function announcementPhotoFileUrl(postId: number, photoId: number): string {
  return `/api/v1/announcements/${postId}/photos/${photoId}/file`;
}

export async function getAdminAnnouncements(cookieHeader: string): Promise<AdminListResult<AdminAnnouncementDto>> {
  const response = await fetch(`${API_BASE_URL}/api/v1/admin/announcements`, {
    headers: { Cookie: cookieHeader },
    cache: "no-store",
  });

  if (response.status === 401) return { kind: "unauthenticated" };
  if (response.status === 403) return { kind: "forbidden" };
  if (!response.ok) throw new Error(`Failed to load announcements: ${response.status}`);
  return { kind: "ok", items: await response.json() };
}

export function createAdminAnnouncement(body: AdminAnnouncementRequest): Promise<AdminAnnouncementDto> {
  return adminApiRequest("/api/v1/admin/announcements", "POST", body);
}

export function updateAdminAnnouncement(id: number, body: AdminAnnouncementRequest): Promise<AdminAnnouncementDto> {
  return adminApiRequest(`/api/v1/admin/announcements/${id}`, "PUT", body);
}

export function publishAnnouncement(id: number): Promise<AdminAnnouncementDto> {
  return adminApiRequest(`/api/v1/admin/announcements/${id}/publish`, "POST");
}

export function unpublishAnnouncement(id: number): Promise<AdminAnnouncementDto> {
  return adminApiRequest(`/api/v1/admin/announcements/${id}/unpublish`, "POST");
}

export function deleteAdminAnnouncement(id: number): Promise<void> {
  return adminApiRequest(`/api/v1/admin/announcements/${id}`, "DELETE");
}

// FormData, not JSON -- same reasoning as uploadPersonPhoto.
export async function uploadAnnouncementPhoto(postId: number, file: File, caption: string): Promise<AnnouncementPhotoDto> {
  const xsrfToken = readXsrfTokenCookie();
  const formData = new FormData();
  formData.append("file", file);
  if (caption.trim()) {
    formData.append("caption", caption.trim());
  }

  const response = await fetch(`/api/v1/admin/announcements/${postId}/photos`, {
    method: "POST",
    headers: xsrfToken ? { "X-XSRF-TOKEN": xsrfToken } : undefined,
    body: formData,
  });

  if (!response.ok) {
    const body = await response.json().catch(() => null);
    throw new AdminActionError(body?.message ?? "Failed to upload photo.");
  }
  return response.json();
}

export function deleteAnnouncementPhoto(postId: number, photoId: number): Promise<void> {
  return adminApiRequest(`/api/v1/admin/announcements/${postId}/photos/${photoId}`, "DELETE");
}

// Mirrors the JSON shape RelationshipService#buildLineageTree already
// produces for the legacy /lineage page -- reused as-is (not under
// /api/v1/admin, a pre-existing endpoint) rather than building a new
// one, per docs/frontend-redesign-plan.md's "reuse existing APIs"
// approach.
export interface LineageTreeNode {
  id: number;
  dbId: number;
  parentDbId: number | null;
  generationNumber: number | null;
  name: string;
  englishName: string;
  nepaliName: string;
  photoPath: string | null;
  children: LineageTreeNode[];
}

// Server-to-server with the browser's session cookie forwarded, same
// pattern as the rest of lib/api.ts. Allowed for ADMIN or USER on the
// backend (SecurityConfig), but this app only ever links to it from
// the admin-only sidebar section.
export async function getLineageTree(cookieHeader: string): Promise<LineageTreeNode | null> {
  const response = await fetch(`${API_BASE_URL}/lineage/tree`, {
    headers: { Cookie: cookieHeader },
    cache: "no-store",
  });
  if (!response.ok) {
    throw new Error(`Failed to load lineage tree: ${response.status}`);
  }
  const data = await response.json();
  return data && data.dbId ? data : null;
}

export interface SaveLineagePersonResult {
  id: number;
  englishName: string;
  nepaliName: string;
  photoPath: string | null;
}

export interface SaveLineagePersonParams {
  fullName: string;
  personId?: number;
  parentId?: number;
  generationNumber?: number;
}

// POST /lineage/save-person is form-urlencoded, not JSON (it's the
// pre-existing legacy endpoint, unchanged) -- same CSRF pattern as the
// rest of the browser-originated mutations in this file.
export async function saveLineagePerson(params: SaveLineagePersonParams): Promise<SaveLineagePersonResult> {
  const xsrfToken = readXsrfTokenCookie();
  const body = new URLSearchParams();
  body.append("fullName", params.fullName);
  if (params.personId != null) body.append("personId", String(params.personId));
  if (params.parentId != null) body.append("parentId", String(params.parentId));
  if (params.generationNumber != null) body.append("generationNumber", String(params.generationNumber));

  const response = await fetch("/lineage/save-person", {
    method: "POST",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
      ...(xsrfToken ? { "X-XSRF-TOKEN": xsrfToken } : {}),
    },
    body: body.toString(),
  });

  if (!response.ok) {
    throw new AdminActionError(`Failed to save: ${response.status}`);
  }
  return response.json();
}

// Mirrors com.familytree.dto.ParentGapDto / RelationshipCycleDto /
// DateIssueDto / DataQualityReportDto (docs/08 Phase 6). Read-only --
// every fix happens through the existing tools this report links out to.
export interface ParentGapDto {
  personId: number;
  personName: string;
  generationNumber: number | null;
  knownParentCount: number;
}

export interface RelationshipCycleDto {
  personIds: number[];
  personNames: string[];
}

export type DateIssueType =
  | "MISSING_BIRTH_DATE"
  | "DEATH_BEFORE_BIRTH"
  | "FUTURE_BIRTH_DATE"
  | "IMPLAUSIBLE_PARENT_AGE_GAP";

export interface DateIssueDto {
  personId: number;
  personName: string;
  issueType: DateIssueType;
  detail: string;
}

export interface DataQualityReportDto {
  parentGaps: ParentGapDto[];
  cycles: RelationshipCycleDto[];
  unlinkedAccounts: AdminUserAccountDto[];
  dateIssues: DateIssueDto[];
}

export type DataQualityReportResult =
  | { kind: "ok"; report: DataQualityReportDto }
  | { kind: "unauthenticated" }
  | { kind: "forbidden" };

export async function getAdminDataQuality(cookieHeader: string): Promise<DataQualityReportResult> {
  const response = await fetch(`${API_BASE_URL}/api/v1/admin/data-quality`, {
    headers: { Cookie: cookieHeader },
    cache: "no-store",
  });

  if (response.status === 401) return { kind: "unauthenticated" };
  if (response.status === 403) return { kind: "forbidden" };
  if (!response.ok) throw new Error(`Failed to load data quality report: ${response.status}`);
  return { kind: "ok", report: await response.json() };
}

// Mirrors com.familytree.dto.DuplicatePersonSnapshotDto /
// DuplicateCandidateDto / MergeResultDto (docs/08 Phase 6). Merge is never
// automatic -- an admin reviews each candidate pair and explicitly picks
// which record survives.
export interface DuplicatePersonSnapshotDto {
  id: number;
  englishFullName: string;
  nepaliFullName: string;
  gender: string | null;
  birthDate: string | null;
  deathDate: string | null;
  generationNumber: number | null;
  populatedFieldCount: number;
}

export interface DuplicateCandidateDto {
  personA: DuplicatePersonSnapshotDto;
  personB: DuplicatePersonSnapshotDto;
  confidence: MatchConfidence;
  reasons: string[];
  hasConflict: boolean;
}

export interface MergeResultDto {
  survivorId: number;
  relationshipsRepointed: number;
  relationshipsDroppedAsDuplicate: number;
  userLinksRepointed: number;
  correctionRequestsRepointed: number;
}

export async function getAdminDuplicates(cookieHeader: string): Promise<AdminListResult<DuplicateCandidateDto>> {
  const response = await fetch(`${API_BASE_URL}/api/v1/admin/duplicates`, {
    headers: { Cookie: cookieHeader },
    cache: "no-store",
  });

  if (response.status === 401) return { kind: "unauthenticated" };
  if (response.status === 403) return { kind: "forbidden" };
  if (!response.ok) throw new Error(`Failed to load duplicate candidates: ${response.status}`);
  return { kind: "ok", items: await response.json() };
}

export function mergeAdminDuplicate(survivorId: number, loserId: number): Promise<MergeResultDto> {
  return adminApiRequest("/api/v1/admin/duplicates/merge", "POST", { survivorId, loserId });
}
