import { useTranslations } from "next-intl";
import { PageShell } from "@/components/PageShell";

export default function ContactPage() {
  const t = useTranslations("contactPage");

  return (
    <PageShell title={t("title")}>
      <p>{t("intro")}</p>
    </PageShell>
  );
}
