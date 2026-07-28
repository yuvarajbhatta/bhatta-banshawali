import { useTranslations } from "next-intl";
import { Link } from "@/i18n/navigation";
import { Button } from "@/components/Button";
import { Card } from "@/components/Card";
import { LanguageSwitcher } from "@/components/LanguageSwitcher";
import styles from "./page.module.css";

export default function LandingPage() {
  const t = useTranslations("landing");

  return (
    <main className={styles.page}>
      <header className={styles.header}>
        <LanguageSwitcher />
        <Link href="/login">
          <Button variant="ghost">{t("login")}</Button>
        </Link>
      </header>

      <section className={styles.hero}>
        <h1 className={styles.heroTitle}>{t("heroTitle")}</h1>
        <p className={styles.heroSubtitle}>{t("heroSubtitle")}</p>
        <div className={styles.actions}>
          <Button variant="primary">{t("requestMembership")}</Button>
          <Button variant="secondary">{t("learnAboutBanshawali")}</Button>
        </div>
      </section>

      <Card title="Design system preview">
        This page demonstrates the design tokens, Button/Card components, and bilingual
        routing (English/Nepali) wired up as the Phase 1 frontend foundation.
      </Card>
    </main>
  );
}
