import { getTranslations } from "next-intl/server";
import { RouteLoading } from "@/components/shell/RouteLoading";

export default async function AdminAccessRequestsLoading() {
  const t = await getTranslations("adminAccessRequestsPage");
  return <RouteLoading title={t("title")} subtitle={t("subtitle")} />;
}
