import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { getTranslations } from "next-intl/server";
import { PageShell } from "@/components/PageShell";
import { MemberDashboard } from "@/components/dashboard/MemberDashboard";
import { AdminDashboard } from "@/components/dashboard/AdminDashboard";
import { getAdminSummary, getMemberProfile, getPublicStats } from "@/lib/api";

export default async function DashboardPage() {
  const t = await getTranslations("dashboardPage");
  const cookieStore = await cookies();
  const cookieHeader = cookieStore.getAll().map((c) => `${c.name}=${c.value}`).join("; ");

  const result = await getMemberProfile(cookieHeader);

  if (result.kind === "unauthenticated") {
    redirect("/login");
  }

  if (result.kind === "no-account") {
    // No UserAccount at all -- this is the old AppUser login path
    // (admins today, matching how existing accounts still work). Not
    // every AppUser is necessarily an admin though, so this still
    // checks with the backend rather than assuming: getAdminSummary
    // returns null on a 403, and AdminDashboard shows a plain notice
    // instead of leaking review-queue data to a non-admin.
    const [summary, stats] = await Promise.all([
      getAdminSummary(cookieHeader),
      getPublicStats().catch(() => null),
    ]);
    return (
      <PageShell title={t("title")}>
        <AdminDashboard summary={summary} stats={stats} />
      </PageShell>
    );
  }

  return (
    <PageShell title={t("title")}>
      <MemberDashboard profile={result.profile} />
    </PageShell>
  );
}
