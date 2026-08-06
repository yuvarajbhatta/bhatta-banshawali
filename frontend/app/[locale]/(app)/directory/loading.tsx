import { getTranslations } from "next-intl/server";
import { RouteLoading } from "@/components/shell/RouteLoading";

export default async function DirectoryLoading() {
  const t = await getTranslations("directoryPage");
  return <RouteLoading title={t("title")} subtitle={t("subtitle")} />;
}
