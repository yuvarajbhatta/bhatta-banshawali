import { getTranslations } from "next-intl/server";
import { RouteLoading } from "@/components/shell/RouteLoading";

export default async function AdminCorrectionsLoading() {
  const t = await getTranslations("adminCorrectionsPage");
  return <RouteLoading title={t("title")} subtitle={t("subtitle")} />;
}
