import { getTranslations } from "next-intl/server";
import { RouteLoading } from "@/components/shell/RouteLoading";

export default async function PersonDetailLoading() {
  const t = await getTranslations("directoryPage");
  return <RouteLoading title={t("title")} />;
}
