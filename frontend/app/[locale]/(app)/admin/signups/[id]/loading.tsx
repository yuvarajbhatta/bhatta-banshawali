import { getTranslations } from "next-intl/server";
import { RouteLoading } from "@/components/shell/RouteLoading";

export default async function AdminSignupDetailLoading() {
  const t = await getTranslations("adminSignupsPage");
  return <RouteLoading title={t("detailTitle")} />;
}
