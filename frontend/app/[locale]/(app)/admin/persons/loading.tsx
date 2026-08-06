import { getTranslations } from "next-intl/server";
import { RouteLoading } from "@/components/shell/RouteLoading";

export default async function AdminPersonsLoading() {
  const t = await getTranslations("adminPersonsPage");
  return <RouteLoading title={t("title")} subtitle={t("subtitle")} />;
}
