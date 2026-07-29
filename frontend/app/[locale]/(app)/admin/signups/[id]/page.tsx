import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { getTranslations } from "next-intl/server";
import { Link } from "@/i18n/navigation";
import { PageHeader } from "@/components/shell/PageHeader";
import { SignupDetail } from "@/components/admin/SignupDetail";
import { getAdminSignupDetail } from "@/lib/api";
import styles from "@/components/admin/QueueTable.module.css";

export default async function AdminSignupDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const t = await getTranslations("adminSignupsPage");
  const cookieStore = await cookies();
  const cookieHeader = cookieStore.getAll().map((c) => `${c.name}=${c.value}`).join("; ");

  const result = await getAdminSignupDetail(cookieHeader, id);
  if (result.kind === "unauthenticated") {
    redirect("/login");
  }

  return (
    <>
      <PageHeader
        title={result.kind === "ok" ? result.detail.submittedFullName : t("detailTitle")}
        actions={
          <Link href="/admin/signups" className={styles.reviewLink}>
            {t("backToList")}
          </Link>
        }
      />

      {result.kind === "forbidden" ? <p>{t("forbidden")}</p> : null}
      {result.kind === "not-found" ? <p>{t("notFound")}</p> : null}
      {result.kind === "ok" ? <SignupDetail initialDetail={result.detail} /> : null}
    </>
  );
}
