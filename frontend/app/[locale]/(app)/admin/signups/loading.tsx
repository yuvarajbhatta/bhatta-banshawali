import { getTranslations } from "next-intl/server";
import { RouteLoading } from "@/components/shell/RouteLoading";

export default async function AdminSignupsLoading() {
  const t = await getTranslations("adminSignupsPage");
  return <RouteLoading title={t("title")} subtitle={t("subtitle")} />;
}
