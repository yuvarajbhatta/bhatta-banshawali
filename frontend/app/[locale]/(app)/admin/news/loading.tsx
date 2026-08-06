import { getTranslations } from "next-intl/server";
import { RouteLoading } from "@/components/shell/RouteLoading";

export default async function AdminNewsLoading() {
  const t = await getTranslations("adminNewsPage");
  return <RouteLoading title={t("title")} />;
}
