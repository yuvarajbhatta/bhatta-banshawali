import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { getTranslations } from "next-intl/server";
import { Link } from "@/i18n/navigation";
import { PageShell } from "@/components/PageShell";
import { getPersonDetail, type PersonSummaryDto } from "@/lib/api";
import styles from "./page.module.css";

export default async function PersonDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const t = await getTranslations("personDetailPage");
  const cookieStore = await cookies();
  const cookieHeader = cookieStore.getAll().map((c) => `${c.name}=${c.value}`).join("; ");

  const result = await getPersonDetail(id, cookieHeader);

  if (result.kind === "unauthenticated") {
    redirect("/login");
  }

  return (
    <PageShell title={result.kind === "ok" ? result.person.englishFullName : t("notFound")}>
      {result.kind === "not-found" ? <p className={styles.notice}>{t("notFound")}</p> : null}

      {result.kind === "ok" ? (
        <div className={styles.profile}>
          <div className={styles.card}>
            {result.person.nepaliFullName ? <p className={styles.nepaliName}>{result.person.nepaliFullName}</p> : null}
            {result.person.nickname ? <p className={styles.meta}>{result.person.nickname}</p> : null}

            <dl className={styles.grid}>
              <dt>{t("fields.birthDate")}</dt>
              <dd>{result.person.birthDate ?? t("notAdded")}</dd>

              <dt>{t("fields.deathDate")}</dt>
              <dd>{result.person.deathDate ?? t("notAdded")}</dd>

              <dt>{t("fields.birthPlace")}</dt>
              <dd>{result.person.birthPlace ?? t("notAdded")}</dd>

              <dt>{t("fields.currentAddress")}</dt>
              <dd>{result.person.currentAddress ?? t("notAdded")}</dd>
            </dl>
          </div>

          <div className={styles.card}>
            <h3>{t("family.title")}</h3>
            <dl className={styles.grid}>
              <dt>{t("family.father")}</dt>
              <dd>{personLink(result.person.family.father, t("family.none"))}</dd>

              <dt>{t("family.mother")}</dt>
              <dd>{personLink(result.person.family.mother, t("family.none"))}</dd>

              <dt>{t("family.spouses")}</dt>
              <dd>{personLinks(result.person.family.spouses, t("family.none"))}</dd>

              <dt>{t("family.children")}</dt>
              <dd>{personLinks(result.person.family.children, t("family.none"))}</dd>
            </dl>
          </div>

          <div className={styles.card}>
            <h3>{t("notes")}</h3>
            <p className={styles.notes}>{result.person.notes || t("noNotes")}</p>
          </div>
        </div>
      ) : null}

      <Link href="/directory" className={styles.backLink}>
        {t("backToDirectory")}
      </Link>
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
