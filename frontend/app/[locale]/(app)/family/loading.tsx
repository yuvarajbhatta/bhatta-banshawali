import { getTranslations } from "next-intl/server";
import { RouteLoading } from "@/components/shell/RouteLoading";

export default async function FamilyLoading() {
  const t = await getTranslations("familyPage");
  return <RouteLoading title={t("title")} />;
}
