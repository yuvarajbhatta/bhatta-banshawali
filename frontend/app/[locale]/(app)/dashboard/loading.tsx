import { getTranslations } from "next-intl/server";
import { RouteLoading } from "@/components/shell/RouteLoading";

export default async function DashboardLoading() {
  const t = await getTranslations("dashboardPage");
  return <RouteLoading title={t("title")} />;
}
