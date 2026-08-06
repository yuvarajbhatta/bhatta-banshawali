import { getTranslations } from "next-intl/server";
import { RouteLoading } from "@/components/shell/RouteLoading";

export default async function AdminRelationshipsLoading() {
  const t = await getTranslations("adminRelationshipsPage");
  return <RouteLoading title={t("title")} subtitle={t("subtitle")} />;
}
