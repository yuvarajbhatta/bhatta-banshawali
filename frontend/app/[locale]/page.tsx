import { getTranslations } from "next-intl/server";
import { Link } from "@/i18n/navigation";
import { Button } from "@/components/Button";
import { LanguageSwitcher } from "@/components/LanguageSwitcher";
import { HeroScene } from "@/components/landing/HeroScene";
import { Reveal } from "@/components/motion/Reveal";
import { ScrollReveal } from "@/components/motion/ScrollReveal";
import { getPublicStats, type PublicStatsDto } from "@/lib/api";
import styles from "./page.module.css";

export default async function LandingPage() {
  const t = await getTranslations();
  const stats = await loadStatsQuietly();

  return (
    <main className={styles.page}>
      <section className={`${styles.hero} stage`}>
        <HeroScene />

        <header className={styles.header}>
          <LanguageSwitcher />
          {/* /login used to need a plain <a> here (it was a backend-only
              path with no route in this app, so the locale-aware Link's
              automatic "/en/login" prefixing 404'd). It's a real Next.js
              route now (see app/[locale]/login), so the normal
              locale-aware Link is correct, same as /signup below. */}
          <Link href="/login">
            <Button variant="ghost">{t("landing.login")}</Button>
          </Link>
        </header>

        <div className={styles.heroContent}>
          <Reveal>
            <h1 className={styles.heroTitle}>{t("landing.heroTitle")}</h1>
          </Reveal>
          <Reveal delay={0.1}>
            <p className={styles.heroSubtitle}>{t("landing.heroSubtitle")}</p>
          </Reveal>
          <Reveal delay={0.2}>
            <div className={styles.actions}>
              <Link href="/signup">
                <Button variant="primary">{t("landing.requestMembership")}</Button>
              </Link>
              <Link href="/about">
                <Button variant="secondary">{t("landing.learnAboutBanshawali")}</Button>
              </Link>
            </div>
          </Reveal>
        </div>
      </section>

      {stats ? (
        <ScrollReveal className={styles.stats} targets={`.${styles.statItem}`}>
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
        </ScrollReveal>
      ) : null}

      <Reveal className={styles.footerWrapper}>
        <footer className={styles.footer}>
          <Link href="/history">{t("historyPage.title")}</Link>
          <Link href="/membership">{t("membershipPage.linkLabel")}</Link>
          <Link href="/contact">{t("contactPage.title")}</Link>
          <Link href="/privacy">{t("privacyPage.title")}</Link>
          <Link href="/terms">{t("termsPage.title")}</Link>
        </footer>
      </Reveal>
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
