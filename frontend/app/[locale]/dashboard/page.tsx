import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { getTranslations } from "next-intl/server";
import { Link } from "@/i18n/navigation";
import { PageShell } from "@/components/PageShell";
import { getMemberProfile, type PersonSummaryDto } from "@/lib/api";
import styles from "./page.module.css";

export default async function DashboardPage() {
  const t = await getTranslations("dashboardPage");
  const cookieStore = await cookies();
  const cookieHeader = cookieStore.getAll().map((c) => `${c.name}=${c.value}`).join("; ");

  const result = await getMemberProfile(cookieHeader);

  if (result.kind === "unauthenticated") {
    redirect("/login");
  }

  return (
    <PageShell title={t("title")}>
      {result.kind === "no-account" ? <p className={styles.notice}>{t("noAccount")}</p> : null}

      {result.kind === "ok" && !result.profile.linked ? <p className={styles.notice}>{t("unlinked")}</p> : null}

      {result.kind === "ok" && result.profile.linked && result.profile.person ? (
        <div className={styles.profile}>
          <div className={styles.card}>
            <h2>
              <Link href={`/directory/${result.profile.person.id}`}>{result.profile.person.englishFullName}</Link>
            </h2>
            {result.profile.person.nepaliFullName ? <p className={styles.nepaliName}>{result.profile.person.nepaliFullName}</p> : null}
            {result.profile.person.generationNumber != null ? (
              <p className={styles.meta}>{t("generation", { number: result.profile.person.generationNumber })}</p>
            ) : null}
          </div>

          {result.profile.family ? (
            <div className={styles.card}>
              <h3>{t("family.title")}</h3>
              <dl className={styles.familyGrid}>
                <dt>{t("family.father")}</dt>
                <dd>{personLink(result.profile.family.father, t("family.none"))}</dd>

                <dt>{t("family.mother")}</dt>
                <dd>{personLink(result.profile.family.mother, t("family.none"))}</dd>

                <dt>{t("family.spouses")}</dt>
                <dd>{personLinks(result.profile.family.spouses, t("family.none"))}</dd>

                <dt>{t("family.children")}</dt>
                <dd>{personLinks(result.profile.family.children, t("family.none"))}</dd>
              </dl>
            </div>
          ) : null}
        </div>
      ) : null}
    </PageShell>
  );
}

function personLink(person: PersonSummaryDto | null, emptyLabel: string) {
  if (!person) {
    return emptyLabel;
  }
  return <Link href={`/directory/${person.id}`}>{person.englishFullName}</Link>;
}

function personLinks(people: PersonSummaryDto[], emptyLabel: string) {
  if (people.length === 0) {
    return emptyLabel;
  }
  return people.map((person, index) => (
    <span key={person.id}>
      {index > 0 ? ", " : ""}
      <Link href={`/directory/${person.id}`}>{person.englishFullName}</Link>
    </span>
  ));
}
