import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { getTranslations } from "next-intl/server";
import { PageHeader } from "@/components/shell/PageHeader";
import { StatusFilterTabs } from "@/components/admin/StatusFilterTabs";
import { CorrectionQueueTable } from "@/components/admin/CorrectionQueueTable";
import { getAdminCorrections, type CorrectionRequestStatus } from "@/lib/api";

const STATUSES = ["PENDING", "APPROVED", "REJECTED"] as const;

export default async function AdminCorrectionsPage({
  searchParams,
}: {
  searchParams: Promise<{ status?: string }>;
}) {
  const t = await getTranslations("adminCorrectionsPage");
  const statusT = await getTranslations("correctionRequestStatus");
  const { status } = await searchParams;
  const selectedStatus = (STATUSES as readonly string[]).includes(status ?? "") ? (status as CorrectionRequestStatus) : "PENDING";

  const cookieStore = await cookies();
  const cookieHeader = cookieStore.getAll().map((c) => `${c.name}=${c.value}`).join("; ");

  const result = await getAdminCorrections(cookieHeader, selectedStatus);
  if (result.kind === "unauthenticated") {
    redirect("/login");
  }

  return (
    <>
      <PageHeader title={t("title")} subtitle={t("subtitle")} />

      {result.kind === "forbidden" ? (
        <p>{t("forbidden")}</p>
      ) : (
        <>
          <StatusFilterTabs
            statuses={STATUSES}
            selected={selectedStatus}
            basePath="/admin/corrections"
            labelFor={(s) => statusT(s)}
          />
          <CorrectionQueueTable initialItems={result.items} showActions={selectedStatus === "PENDING"} />
        </>
      )}
    </>
  );
}
