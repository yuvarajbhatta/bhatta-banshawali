import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { getTranslations } from "next-intl/server";
import { PageHeader } from "@/components/shell/PageHeader";
import { AccountsManager } from "@/components/admin/AccountsManager";
import { getAdminAccounts, getMemberProfile } from "@/lib/api";

export default async function AdminAccountsPage() {
  const t = await getTranslations("adminAccountsPage");
  const cookieStore = await cookies();
  const cookieHeader = cookieStore.getAll().map((c) => `${c.name}=${c.value}`).join("; ");

  const [result, profileResult] = await Promise.all([
    getAdminAccounts(cookieHeader),
    // "no-account" (legacy AppUser admin login) has no email at all, so
    // there's no account row in this list that could ever be "self" --
    // currentUserEmail stays null and every row's self-check is simply
    // false, same as today.
    getMemberProfile(cookieHeader),
  ]);
  if (result.kind === "unauthenticated") {
    redirect("/login");
  }
  const currentUserEmail = profileResult.kind === "ok" ? profileResult.profile.email : null;

  return (
    <>
      <PageHeader title={t("title")} subtitle={t("subtitle")} />
      {result.kind === "forbidden" ? (
        <p>{t("forbidden")}</p>
      ) : (
        <AccountsManager initialItems={result.items} currentUserEmail={currentUserEmail} />
      )}
    </>
  );
}
