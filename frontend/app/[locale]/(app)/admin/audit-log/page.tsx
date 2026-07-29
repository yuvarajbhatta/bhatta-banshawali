import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { getTranslations } from "next-intl/server";
import { PageHeader } from "@/components/shell/PageHeader";
import { AuditLogList } from "@/components/admin/AuditLogList";
import { getAdminAuditLog } from "@/lib/api";
import queueStyles from "@/components/admin/QueueTable.module.css";

export default async function AdminAuditLogPage() {
  const t = await getTranslations("adminAuditLogPage");
  const cookieStore = await cookies();
  const cookieHeader = cookieStore.getAll().map((c) => `${c.name}=${c.value}`).join("; ");

  const result = await getAdminAuditLog(cookieHeader);
  if (result.kind === "unauthenticated") {
    redirect("/login");
  }

  return (
    <>
      <PageHeader title={t("title")} subtitle={t("subtitle")} />
      {result.kind === "forbidden" ? (
        <p>{t("forbidden")}</p>
      ) : result.items.length === 0 ? (
        <div className={queueStyles.empty}>{t("empty")}</div>
      ) : (
        <AuditLogList entries={result.items} />
      )}
    </>
  );
}
