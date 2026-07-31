import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { getTranslations } from "next-intl/server";
import { PageHeader } from "@/components/shell/PageHeader";
import { DataQualityReport } from "@/components/admin/DataQualityReport";
import { getAdminDataQuality } from "@/lib/api";

export default async function AdminDataQualityPage() {
  const t = await getTranslations("adminDataQualityPage");
  const cookieStore = await cookies();
  const cookieHeader = cookieStore.getAll().map((c) => `${c.name}=${c.value}`).join("; ");

  const result = await getAdminDataQuality(cookieHeader);
  if (result.kind === "unauthenticated") {
    redirect("/login");
  }

  return (
    <>
      <PageHeader title={t("title")} subtitle={t("subtitle")} />
      {result.kind === "forbidden" ? <p>{t("forbidden")}</p> : <DataQualityReport report={result.report} />}
    </>
  );
}
