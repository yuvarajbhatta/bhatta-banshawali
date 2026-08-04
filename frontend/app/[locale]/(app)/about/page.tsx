import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { getLocale, getTranslations } from "next-intl/server";
import { PageHeader } from "@/components/shell/PageHeader";
import { Paragraphs } from "@/components/Paragraphs";
import { getMemberProfile, getPublishedArticle, type ArticleDto } from "@/lib/api";
import { localizeArticle } from "@/lib/localize-article";
import styles from "./page.module.css";

const SLUGS = ["about-banshawali", "family-history"] as const;

export default async function AboutPage() {
  const t = await getTranslations("aboutPage");
  const locale = await getLocale();
  const cookieStore = await cookies();
  const cookieHeader = cookieStore.getAll().map((c) => `${c.name}=${c.value}`).join("; ");

  const authCheck = await getMemberProfile(cookieHeader);
  if (authCheck.kind === "unauthenticated") {
    redirect("/login");
  }

  const articles = await Promise.all(SLUGS.map((slug) => getPublishedArticle(slug)));
  const sections = articles
    .filter((article): article is ArticleDto => article !== null)
    .map((article) => localizeArticle(article, locale));

  return (
    <>
      <PageHeader title={t("title")} />

      <div className={styles.stack}>
        {sections.length === 0 ? (
          <p className={styles.empty}>{t("empty")}</p>
        ) : (
          sections.map((section) => (
            <section key={section.title} className={styles.card}>
              <h2 className={styles.sectionTitle}>{section.title}</h2>
              <div className={styles.body}>
                <Paragraphs text={section.body} />
              </div>
            </section>
          ))
        )}
      </div>
    </>
  );
}
