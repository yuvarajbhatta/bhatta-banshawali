import { getTranslations } from "next-intl/server";
import { RouteLoading } from "@/components/shell/RouteLoading";

export default async function AdminContentLoading() {
  const t = await getTranslations("adminContentPage");
  return <RouteLoading title={t("title")} subtitle={t("subtitle")} />;
}
