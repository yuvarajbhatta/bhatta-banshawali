import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { getTranslations } from "next-intl/server";
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
            <h2>{result.profile.person.englishFullName}</h2>
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
                <dd>{personLabel(result.profile.family.father, t("family.none"))}</dd>

                <dt>{t("family.mother")}</dt>
                <dd>{personLabel(result.profile.family.mother, t("family.none"))}</dd>

                <dt>{t("family.spouses")}</dt>
                <dd>{personListLabel(result.profile.family.spouses, t("family.none"))}</dd>

                <dt>{t("family.children")}</dt>
                <dd>{personListLabel(result.profile.family.children, t("family.none"))}</dd>
              </dl>
            </div>
          ) : null}
        </div>
      ) : null}
    </PageShell>
  );
}

function personLabel(person: PersonSummaryDto | null, emptyLabel: string): string {
  return person ? person.englishFullName : emptyLabel;
}

function personListLabel(people: PersonSummaryDto[], emptyLabel: string): string {
  return people.length > 0 ? people.map((p) => p.englishFullName).join(", ") : emptyLabel;
}
