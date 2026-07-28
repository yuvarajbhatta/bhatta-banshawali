import { useTranslations } from "next-intl";
import { Link } from "@/i18n/navigation";
import { Button } from "@/components/Button";
import { LanguageSwitcher } from "@/components/LanguageSwitcher";
import styles from "./page.module.css";

export default function LandingPage() {
  const t = useTranslations();

  return (
    <main className={styles.page}>
      {/* /login and /signup are the existing Spring Boot/Thymeleaf pages,
          proxied through by nginx in production (see the banshawali.yrbhatta.com
          vhost) until they're rebuilt as Next.js pages -- they 404 under
          `next dev` locally without that proxy in front. */}
      <header className={styles.header}>
        <LanguageSwitcher />
        <Link href="/login">
          <Button variant="ghost">{t("landing.login")}</Button>
        </Link>
      </header>

      <section className={styles.hero}>
        <h1 className={styles.heroTitle}>{t("landing.heroTitle")}</h1>
        <p className={styles.heroSubtitle}>{t("landing.heroSubtitle")}</p>
        <div className={styles.actions}>
          <Link href="/signup">
            <Button variant="primary">{t("landing.requestMembership")}</Button>
          </Link>
          <Link href="/about">
            <Button variant="secondary">{t("landing.learnAboutBanshawali")}</Button>
          </Link>
        </div>
      </section>

      <footer className={styles.footer}>
        <Link href="/history">{t("historyPage.title")}</Link>
        <Link href="/membership">{t("membershipPage.linkLabel")}</Link>
        <Link href="/contact">{t("contactPage.title")}</Link>
        <Link href="/privacy">{t("privacyPage.title")}</Link>
        <Link href="/terms">{t("termsPage.title")}</Link>
      </footer>
    </main>
  );
}
