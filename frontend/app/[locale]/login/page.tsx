import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { getLocale, getTranslations } from "next-intl/server";
import { LoginForm } from "@/components/LoginForm";
import { FamilyTreeWatermark } from "@/components/landing/FamilyTreeWatermark";
import { Reveal } from "@/components/motion/Reveal";
import { LanguageSwitcher } from "@/components/LanguageSwitcher";
import { Paragraphs } from "@/components/Paragraphs";
import { getMemberProfile, getPublishedArticle } from "@/lib/api";
import { localizeArticle } from "@/lib/localize-article";
import styles from "./page.module.css";

export default async function LoginPage({
  searchParams,
}: {
  searchParams: Promise<{ error?: string; logout?: string; registered?: string }>;
}) {
  const t = await getTranslations("login");
  const params = await searchParams;
  const locale = await getLocale();
  // Admin-managed "About the Banshawali" content, shown here instead of
  // on a separate /about page -- this is the one place an unauthenticated
  // visitor lands, so it doubles as the site's about section. Falls back
  // to the static tagline if the article is unpublished or missing.
  const aboutArticle = await getPublishedArticle("about-banshawali");
  const about = aboutArticle ? localizeArticle(aboutArticle, locale) : null;

  // A signed-in visitor landing here (a stale bookmark, browser back
  // button after logging in elsewhere, etc.) should see their
  // dashboard, not a login form for a session they already have --
  // params.logout is the one deliberate exception, since that's the
  // moment right after a real logout, when the session is (correctly)
  // already gone.
  if (params.logout === undefined) {
    const cookieStore = await cookies();
    const cookieHeader = cookieStore.getAll().map((c) => `${c.name}=${c.value}`).join("; ");
    const profile = await getMemberProfile(cookieHeader);
    if (profile.kind !== "unauthenticated") {
      redirect("/dashboard");
    }
  }

  return (
    <main className={styles.page}>
      <section className={`${styles.branding} stage`}>
        <FamilyTreeWatermark />
        <div className={styles.brandingOverlay} />
        <div className={styles.brandingContent}>
          <Reveal>
            <h1 className={styles.brandTitle}>{t("brandTitle")}</h1>
            {about ? (
              <div className={styles.brandAbout}>
                <Paragraphs text={about.body} />
              </div>
            ) : (
              <p className={styles.brandTagline}>{t("brandTagline")}</p>
            )}
          </Reveal>
        </div>
      </section>

      <section className={styles.formPanel}>
        <Reveal className={styles.card} delay={0.1}>
          <h2 className={styles.heading}>{t("heading")}</h2>
          <p className={styles.subtitle}>{t("subtitle")}</p>

          {params.error !== undefined ? (
            <div className={styles.notice} data-variant="error">
              {t("invalid")}
            </div>
          ) : null}
          {params.logout !== undefined ? (
            <div className={styles.notice} data-variant="success">
              {t("loggedOut")}
            </div>
          ) : null}
          {params.registered !== undefined ? (
            <div className={styles.notice} data-variant="success">
              {t("registered")}
            </div>
          ) : null}

          <LoginForm />
        </Reveal>
      </section>

      <footer className={styles.footer}>
        <p className={styles.copyright}>{t("copyright", { year: new Date().getFullYear() })}</p>
        <LanguageSwitcher />
      </footer>
    </main>
  );
}
