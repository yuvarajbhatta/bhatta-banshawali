import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { getTranslations } from "next-intl/server";
import { PageHeader } from "@/components/shell/PageHeader";
import { UnlinkedAccountsManager } from "@/components/admin/UnlinkedAccountsManager";
import { getUnlinkedAccounts } from "@/lib/api";

export default async function AdminUnlinkedAccountsPage() {
  const t = await getTranslations("adminUnlinkedAccountsPage");
  const cookieStore = await cookies();
  const cookieHeader = cookieStore.getAll().map((c) => `${c.name}=${c.value}`).join("; ");

  const result = await getUnlinkedAccounts(cookieHeader);
  if (result.kind === "unauthenticated") {
    redirect("/login");
  }

  return (
    <>
      <PageHeader title={t("title")} subtitle={t("subtitle")} />
      {result.kind === "forbidden" ? <p>{t("forbidden")}</p> : <UnlinkedAccountsManager initialItems={result.items} />}
    </>
  );
}
