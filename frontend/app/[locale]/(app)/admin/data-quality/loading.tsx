import { getTranslations } from "next-intl/server";
import { RouteLoading } from "@/components/shell/RouteLoading";

export default async function AdminDataQualityLoading() {
  const t = await getTranslations("adminDataQualityPage");
  return <RouteLoading title={t("title")} subtitle={t("subtitle")} />;
}
