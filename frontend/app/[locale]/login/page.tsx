import { getTranslations } from "next-intl/server";
import { LoginForm } from "@/components/LoginForm";
import { Reveal } from "@/components/motion/Reveal";
import { LanguageSwitcher } from "@/components/LanguageSwitcher";
import styles from "./page.module.css";

export default async function LoginPage({
  searchParams,
}: {
  searchParams: Promise<{ error?: string; logout?: string; registered?: string }>;
}) {
  const t = await getTranslations("login");
  const params = await searchParams;

  return (
    <main className={styles.page}>
      <section className={`${styles.branding} stage`}>
        <Reveal>
          <h1 className={styles.brandTitle}>{t("brandTitle")}</h1>
          <p className={styles.brandTagline}>{t("brandTagline")}</p>
        </Reveal>
      </section>

      <section className={styles.formPanel}>
        <div className={styles.languageSwitcherRow}>
          <LanguageSwitcher />
        </div>

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
    </main>
  );
}
