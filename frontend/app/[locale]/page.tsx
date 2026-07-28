import { getTranslations } from "next-intl/server";
import { Link } from "@/i18n/navigation";
import { Button } from "@/components/Button";
import { LanguageSwitcher } from "@/components/LanguageSwitcher";
import { getPublicStats, type PublicStatsDto } from "@/lib/api";
import styles from "./page.module.css";

export default async function LandingPage() {
  const t = await getTranslations();
  const stats = await loadStatsQuietly();

  return (
    <main className={styles.page}>
      {/* /login and /signup are the existing Spring Boot/Thymeleaf pages,
          proxied through by nginx in production (see the banshawali.yrbhatta.com
          vhost) until they're rebuilt as Next.js pages -- they 404 under
          `next dev` locally without that proxy in front.

          Deliberately plain <a> tags, not the locale-aware Link from
          @/i18n/navigation: that Link always prefixes the current locale
          (e.g. "/en/login"), which is correct for real Next.js routes but
          wrong here -- these paths don't exist inside this app at all, and
          a locale-prefixed URL 404s in Next.js's own router before nginx
          ever gets a chance to send it to the backend. This is exactly
          the bug that was reported: login redirecting to /en/login. */}
      <header className={styles.header}>
        <LanguageSwitcher />
        {/* eslint-disable-next-line @next/next/no-html-link-for-pages -- intentionally leaving this app, see comment above */}
        <a href="/login">
          <Button variant="ghost">{t("landing.login")}</Button>
        </a>
      </header>

      <section className={styles.hero}>
        <h1 className={styles.heroTitle}>{t("landing.heroTitle")}</h1>
        <p className={styles.heroSubtitle}>{t("landing.heroSubtitle")}</p>
        <div className={styles.actions}>
          {/* eslint-disable-next-line @next/next/no-html-link-for-pages -- intentionally leaving this app, see comment above */}
          <a href="/signup">
            <Button variant="primary">{t("landing.requestMembership")}</Button>
          </a>
          <Link href="/about">
            <Button variant="secondary">{t("landing.learnAboutBanshawali")}</Button>
          </Link>
        </div>
      </section>

      {stats ? (
        <section className={styles.stats} aria-label={t("stats.title")}>
          <div className={styles.statItem}>
            <span className={styles.statValue}>{stats.documentedFamilyMembers}</span>
            <span className={styles.statLabel}>{t("stats.familyMembers")}</span>
          </div>
          <div className={styles.statItem}>
            <span className={styles.statValue}>{stats.documentedGenerations}</span>
            <span className={styles.statLabel}>{t("stats.generations")}</span>
          </div>
          {stats.oldestDocumentedGeneration !== null ? (
            <div className={styles.statItem}>
              <span className={styles.statValue}>{stats.oldestDocumentedGeneration}</span>
              <span className={styles.statLabel}>{t("stats.oldestGeneration")}</span>
            </div>
          ) : null}
        </section>
      ) : null}

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

async function loadStatsQuietly(): Promise<PublicStatsDto | null> {
  try {
    return await getPublicStats();
  } catch {
    // The landing page must render even if the stats endpoint is briefly
    // unavailable -- this section is a nice-to-have, not load-bearing.
    return null;
  }
}
