import { getTranslations } from "next-intl/server";
import { RouteLoading } from "@/components/shell/RouteLoading";

export default async function AdminAccountsLoading() {
  const t = await getTranslations("adminAccountsPage");
  return <RouteLoading title={t("title")} />;
}
