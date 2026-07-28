import { useTranslations } from "next-intl";
import { PageShell } from "@/components/PageShell";

export default function PrivacyPage() {
  const t = useTranslations("privacyPage");

  return (
    <PageShell title={t("title")}>
      <p>{t("intro")}</p>
      <p>{t("classification")}</p>
      <p>{t("correction")}</p>
    </PageShell>
  );
}
