import { getTranslations } from "next-intl/server";
import { Link } from "@/i18n/navigation";
import type { DataQualityReportDto } from "@/lib/api";
import queueStyles from "./QueueTable.module.css";
import styles from "./DataQualityReport.module.css";

export async function DataQualityReport({ report }: { report: DataQualityReportDto }) {
  const t = await getTranslations("adminDataQualityPage");

  return (
    <div>
      <section className={styles.section}>
        <h2 className={styles.heading}>{t("sections.parentGaps")}</h2>
        {report.parentGaps.length === 0 ? (
          <div className={queueStyles.empty}>{t("empty.parentGaps")}</div>
        ) : (
          <div className={queueStyles.tableWrapper}>
            <table className={queueStyles.table}>
              <thead>
                <tr>
                  <th>{t("columns.person")}</th>
                  <th>{t("columns.generation")}</th>
                  <th>{t("columns.knownParents")}</th>
                </tr>
              </thead>
              <tbody>
                {report.parentGaps.map((gap) => (
                  <tr key={gap.personId}>
                    <td>
                      <Link href={`/admin/persons/${gap.personId}/edit`} className={queueStyles.reviewLink}>
                        {gap.personName}
                      </Link>
                    </td>
                    <td>{gap.generationNumber ?? "—"}</td>
                    <td>{gap.knownParentCount}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <section className={styles.section}>
        <h2 className={styles.heading}>{t("sections.cycles")}</h2>
        {report.cycles.length === 0 ? (
          <div className={queueStyles.empty}>{t("empty.cycles")}</div>
        ) : (
          <ul className={styles.cycleList}>
            {report.cycles.map((cycle, cycleIndex) => (
              <li key={cycleIndex} className={styles.cycleItem}>
                {cycle.personIds.map((personId, index) => (
                  <span key={personId}>
                    <Link href={`/admin/persons/${personId}/edit`} className={queueStyles.reviewLink}>
                      {cycle.personNames[index]}
                    </Link>
                    <span className={styles.cycleArrow} aria-hidden="true">
                      →
                    </span>
                  </span>
                ))}
                <span>{cycle.personNames[0]}</span>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className={styles.section}>
        <h2 className={styles.heading}>{t("sections.unlinkedAccounts")}</h2>
        {report.unlinkedAccounts.length === 0 ? (
          <div className={queueStyles.empty}>{t("empty.unlinkedAccounts")}</div>
        ) : (
          <div className={queueStyles.tableWrapper}>
            <table className={queueStyles.table}>
              <thead>
                <tr>
                  <th>{t("columns.email")}</th>
                  <th>{t("columns.status")}</th>
                </tr>
              </thead>
              <tbody>
                {report.unlinkedAccounts.map((account) => (
                  <tr key={account.id}>
                    <td>
                      <Link href="/admin/accounts" className={queueStyles.reviewLink}>
                        {account.email}
                      </Link>
                    </td>
                    <td>{account.status}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <section className={styles.section}>
        <h2 className={styles.heading}>{t("sections.dateIssues")}</h2>
        {report.dateIssues.length === 0 ? (
          <div className={queueStyles.empty}>{t("empty.dateIssues")}</div>
        ) : (
          <div className={queueStyles.tableWrapper}>
            <table className={queueStyles.table}>
              <thead>
                <tr>
                  <th>{t("columns.person")}</th>
                  <th>{t("columns.issue")}</th>
                  <th>{t("columns.detail")}</th>
                </tr>
              </thead>
              <tbody>
                {report.dateIssues.map((issue, index) => (
                  <tr key={index}>
                    <td>
                      <Link href={`/admin/persons/${issue.personId}/edit`} className={queueStyles.reviewLink}>
                        {issue.personName}
                      </Link>
                    </td>
                    <td>{t(`issueTypes.${issue.issueType}`)}</td>
                    <td>{issue.detail}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}
