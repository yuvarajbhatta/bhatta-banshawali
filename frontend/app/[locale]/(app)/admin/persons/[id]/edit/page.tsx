import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { getTranslations } from "next-intl/server";
import { PageHeader } from "@/components/shell/PageHeader";
import { EditPersonForm } from "@/components/admin/EditPersonForm";
import { getAdminPersonDetail } from "@/lib/api";

export default async function EditPersonPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const t = await getTranslations("adminPersonsPage");
  const cookieStore = await cookies();
  const cookieHeader = cookieStore.getAll().map((c) => `${c.name}=${c.value}`).join("; ");

  const result = await getAdminPersonDetail(cookieHeader, id);
  if (result.kind === "unauthenticated") {
    redirect("/login");
  }

  const title =
    result.kind === "ok" ? t("editTitle", { name: `${result.detail.firstName} ${result.detail.lastName}`.trim() }) : t("editTitle", { name: "" });

  return (
    <>
      <PageHeader title={title} />
      {result.kind === "forbidden" ? <p>{t("forbidden")}</p> : null}
      {result.kind === "not-found" ? <p>{t("notFound")}</p> : null}
      {result.kind === "ok" ? <EditPersonForm person={result.detail} /> : null}
    </>
  );
}
