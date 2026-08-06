import { getTranslations } from "next-intl/server";
import { RouteLoading } from "@/components/shell/RouteLoading";

export default async function AdminAuditLogLoading() {
  const t = await getTranslations("adminAuditLogPage");
  return <RouteLoading title={t("title")} subtitle={t("subtitle")} />;
}
