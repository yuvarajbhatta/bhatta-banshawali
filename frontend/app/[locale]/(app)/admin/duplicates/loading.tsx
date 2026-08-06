import { getTranslations } from "next-intl/server";
import { RouteLoading } from "@/components/shell/RouteLoading";

export default async function AdminDuplicatesLoading() {
  const t = await getTranslations("adminDuplicatesPage");
  return <RouteLoading title={t("title")} subtitle={t("subtitle")} />;
}
