import { getTranslations } from "next-intl/server";
import { RouteLoading } from "@/components/shell/RouteLoading";

export default async function AdminLineageLoading() {
  const t = await getTranslations("adminLineagePage");
  return <RouteLoading title={t("title")} subtitle={t("subtitle")} />;
}
