import { useTranslations } from "next-intl";
import { PageShell } from "@/components/PageShell";

export default function HistoryPage() {
  const t = useTranslations("historyPage");

  return (
    <PageShell title={t("title")}>
      <p>{t("pending")}</p>
    </PageShell>
  );
}
