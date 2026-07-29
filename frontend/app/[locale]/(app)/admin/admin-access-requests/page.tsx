import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { getTranslations } from "next-intl/server";
import { PageHeader } from "@/components/shell/PageHeader";
import { AdminAccessRequestsManager } from "@/components/admin/AdminAccessRequestsManager";
import { getAdminAccessRequests } from "@/lib/api";

export default async function AdminAccessRequestsPage() {
  const t = await getTranslations("adminAccessRequestsPage");
  const cookieStore = await cookies();
  const cookieHeader = cookieStore.getAll().map((c) => `${c.name}=${c.value}`).join("; ");

  const result = await getAdminAccessRequests(cookieHeader);
  if (result.kind === "unauthenticated") {
    redirect("/login");
  }

  return (
    <>
      <PageHeader title={t("title")} subtitle={t("subtitle")} />
      {result.kind === "forbidden" ? <p>{t("forbidden")}</p> : <AdminAccessRequestsManager initialItems={result.items} />}
    </>
  );
}
