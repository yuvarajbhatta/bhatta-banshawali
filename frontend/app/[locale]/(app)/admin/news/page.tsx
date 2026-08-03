import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { getTranslations } from "next-intl/server";
import { PageHeader } from "@/components/shell/PageHeader";
import { AnnouncementManager } from "@/components/admin/AnnouncementManager";
import { getAdminAnnouncements } from "@/lib/api";

export default async function AdminNewsPage() {
  const t = await getTranslations("adminNewsPage");
  const cookieStore = await cookies();
  const cookieHeader = cookieStore.getAll().map((c) => `${c.name}=${c.value}`).join("; ");

  const result = await getAdminAnnouncements(cookieHeader);
  if (result.kind === "unauthenticated") {
    redirect("/login");
  }

  return (
    <>
      <PageHeader title={t("title")} subtitle={t("subtitle")} />
      {result.kind === "forbidden" ? <p>{t("forbidden")}</p> : <AnnouncementManager initialItems={result.items} />}
    </>
  );
}
