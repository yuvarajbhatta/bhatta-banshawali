import type { ReactNode } from "react";
import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { getTranslations } from "next-intl/server";
import { AppShell } from "@/components/shell/AppShell";
import { getAdminSummary, getMemberProfile } from "@/lib/api";

/**
 * Shared chrome (sidebar + header) for the authenticated section of the
 * app -- docs/frontend-redesign-plan.md. A route group, so it doesn't
 * change any URL (/dashboard, /directory, /tree stay exactly where they
 * were). Every page under here can assume it's already authenticated;
 * individual pages still resolve their own profile/role-specific data
 * (e.g. the admin-vs-member dashboard branch) same as before.
 */
export default async function AuthenticatedLayout({ children }: { children: ReactNode }) {
  const t = await getTranslations("appShell.role");
  const cookieStore = await cookies();
  const cookieHeader = cookieStore.getAll().map((c) => `${c.name}=${c.value}`).join("; ");

  const [result, adminSummary] = await Promise.all([
    getMemberProfile(cookieHeader),
    // Cheap enough to call for every authenticated page load: returns
    // null on a 403 (not an admin), which also doubles as the sidebar's
    // "should the Administration section render at all" check.
    getAdminSummary(cookieHeader),
  ]);

  if (result.kind === "unauthenticated") {
    redirect("/login");
  }

  let displayName: string;
  let roleLabel: string;

  if (result.kind === "no-account") {
    // Legacy AppUser (admin) login -- no UserAccount/Person link at all,
    // so there's no name to show beyond a role label (see
    // MemberProfileController: this is the expected shape for today's
    // admins, not an error state).
    displayName = t("admin");
    roleLabel = t("admin");
  } else if (result.profile.linked && result.profile.person) {
    displayName = result.profile.person.englishFullName;
    roleLabel = t("member");
  } else {
    displayName = result.profile.email;
    roleLabel = t("pending");
  }

  return (
    <AppShell displayName={displayName} roleLabel={roleLabel} adminCounts={adminSummary}>
      {children}
    </AppShell>
  );
}
