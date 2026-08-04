import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { getLocale, getTranslations } from "next-intl/server";
import { PageHeader } from "@/components/shell/PageHeader";
import { Paragraphs } from "@/components/Paragraphs";
import { CollapsibleSection } from "@/components/CollapsibleSection";
import { ContactTeaser } from "@/components/help/ContactTeaser";
import { getAdminContacts, getMemberProfile, getPublishedArticle } from "@/lib/api";
import { localizeArticle } from "@/lib/localize-article";
import styles from "./page.module.css";

export default async function HelpPage() {
  const t = await getTranslations("helpPage");
  const locale = await getLocale();
  const cookieStore = await cookies();
  const cookieHeader = cookieStore.getAll().map((c) => `${c.name}=${c.value}`).join("; ");

  const authCheck = await getMemberProfile(cookieHeader);
  if (authCheck.kind === "unauthenticated") {
    redirect("/login");
  }

  const verificationArticle = await getPublishedArticle("membership-verification");
  const verification = verificationArticle ? localizeArticle(verificationArticle, locale) : null;
  const adminContacts = await getAdminContacts(cookieHeader);

  const faqKeys = ["joinRequirements", "correctInfo", "addPhotos", "whoCanSee", "reachAdmin"] as const;

  return (
    <>
      <PageHeader title={t("title")} />

      <div className={styles.stack}>
        {verification ? (
          <CollapsibleSection title={verification.title}>
            <div className={styles.body}>
              <Paragraphs text={verification.body} />
            </div>
          </CollapsibleSection>
        ) : null}

        <CollapsibleSection title={t("faq.title")}>
          <dl className={styles.faqList}>
            {faqKeys.map((key) => (
              <div key={key} className={styles.faqItem}>
                <dt className={styles.faqQuestion}>{t(`faq.${key}.question`)}</dt>
                <dd className={styles.faqAnswer}>{t(`faq.${key}.answer`)}</dd>
              </div>
            ))}
          </dl>
        </CollapsibleSection>

        <ContactTeaser label={t("contact.teaser")} />
      </div>

      <section id="contact-section" className={styles.contactSection}>
        <h2 className={styles.sectionTitle}>{t("contact.title")}</h2>
        <p className={styles.body}>{t("contact.body")}</p>
        {adminContacts.length > 0 ? (
          <ul className={styles.adminList}>
            {adminContacts.map((admin, index) => (
              <li key={index}>{admin.email ? <a href={`mailto:${admin.email}`}>{admin.displayName}</a> : admin.displayName}</li>
            ))}
          </ul>
        ) : null}
      </section>
    </>
  );
}
