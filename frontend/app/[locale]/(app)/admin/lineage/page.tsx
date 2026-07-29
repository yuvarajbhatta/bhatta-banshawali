import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { getTranslations } from "next-intl/server";
import { PageHeader } from "@/components/shell/PageHeader";
import { LineageBuilder } from "@/components/admin/LineageBuilder";
import { getAdminSummary, getLineageTree, getMemberProfile } from "@/lib/api";

export default async function AdminLineagePage() {
  const t = await getTranslations("adminLineagePage");
  const cookieStore = await cookies();
  const cookieHeader = cookieStore.getAll().map((c) => `${c.name}=${c.value}`).join("; ");

  const [auth, adminSummary] = await Promise.all([getMemberProfile(cookieHeader), getAdminSummary(cookieHeader)]);
  if (auth.kind === "unauthenticated") {
    redirect("/login");
  }

  return (
    <>
      <PageHeader title={t("title")} subtitle={t("subtitle")} />
      {!adminSummary ? <p>{t("forbidden")}</p> : <LineageBuilder initialTree={await getLineageTree(cookieHeader)} />}
    </>
  );
}
