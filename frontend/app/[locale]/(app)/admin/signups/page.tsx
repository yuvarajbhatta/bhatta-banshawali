import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { getTranslations } from "next-intl/server";
import { Link } from "@/i18n/navigation";
import { PageHeader } from "@/components/shell/PageHeader";
import { StatusFilterTabs } from "@/components/admin/StatusFilterTabs";
import { Badge, matchConfidenceTone } from "@/components/admin/Badge";
import { getAdminSignups, type VerificationStatus } from "@/lib/api";
import styles from "@/components/admin/QueueTable.module.css";

const STATUSES = ["PENDING", "APPROVED", "REJECTED", "NEEDS_MORE_INFO"] as const;

export default async function AdminSignupsPage({
  searchParams,
}: {
  searchParams: Promise<{ status?: string }>;
}) {
  const t = await getTranslations("adminSignupsPage");
  const statusT = await getTranslations("verificationStatus");
  const { status } = await searchParams;
  const selectedStatus = (STATUSES as readonly string[]).includes(status ?? "") ? (status as VerificationStatus) : "PENDING";

  const cookieStore = await cookies();
  const cookieHeader = cookieStore.getAll().map((c) => `${c.name}=${c.value}`).join("; ");

  const result = await getAdminSignups(cookieHeader, selectedStatus);
  if (result.kind === "unauthenticated") {
    redirect("/login");
  }

  return (
    <>
      <PageHeader title={t("title")} subtitle={t("subtitle")} />

      {result.kind === "forbidden" ? (
        <p>{t("forbidden")}</p>
      ) : (
        <>
          <StatusFilterTabs
            statuses={STATUSES}
            selected={selectedStatus}
            basePath="/admin/signups"
            labelFor={(s) => statusT(s)}
          />

          {result.items.length === 0 ? (
            <div className={styles.empty}>{t("empty")}</div>
          ) : (
            <div className={styles.tableWrapper}>
              <table className={styles.table}>
                <thead>
                  <tr>
                    <th>{t("submittedName")}</th>
                    <th>{t("fatherName")}</th>
                    <th>{t("grandfatherName")}</th>
                    <th>{t("confidence")}</th>
                    <th>{t("submittedAt")}</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {result.items.map((item) => (
                    <tr key={item.id}>
                      <td>{item.submittedFullName}</td>
                      <td>{item.submittedFatherName}</td>
                      <td>{item.submittedGrandfatherName}</td>
                      <td>
                        <Badge tone={matchConfidenceTone(item.matchConfidence)}>{item.matchConfidence}</Badge>
                      </td>
                      <td>{formatDate(item.createdAt)}</td>
                      <td>
                        <Link href={`/admin/signups/${item.id}`} className={styles.reviewLink}>
                          {t("review")}
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}
    </>
  );
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString(undefined, { year: "numeric", month: "short", day: "numeric" });
}
