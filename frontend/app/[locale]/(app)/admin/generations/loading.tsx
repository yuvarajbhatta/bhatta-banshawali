import { getTranslations } from "next-intl/server";
import { RouteLoading } from "@/components/shell/RouteLoading";

export default async function AdminGenerationsLoading() {
  const t = await getTranslations("adminGenerationsPage");
  return <RouteLoading title={t("title")} subtitle={t("subtitle")} />;
}
