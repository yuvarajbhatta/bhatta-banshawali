import { useTranslations } from "next-intl";
import { PageShell } from "@/components/PageShell";

export default function TermsPage() {
  const t = useTranslations("termsPage");

  return (
    <PageShell title={t("title")}>
      <p>{t("intro")}</p>
      <p>{t("conduct")}</p>
    </PageShell>
  );
}
