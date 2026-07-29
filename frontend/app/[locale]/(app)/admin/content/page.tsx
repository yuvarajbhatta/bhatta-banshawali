import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { getTranslations } from "next-intl/server";
import { PageHeader } from "@/components/shell/PageHeader";
import { ContentManager } from "@/components/admin/ContentManager";
import { getAdminContent } from "@/lib/api";

export default async function AdminContentPage() {
  const t = await getTranslations("adminContentPage");
  const cookieStore = await cookies();
  const cookieHeader = cookieStore.getAll().map((c) => `${c.name}=${c.value}`).join("; ");

  const result = await getAdminContent(cookieHeader);
  if (result.kind === "unauthenticated") {
    redirect("/login");
  }

  return (
    <>
      <PageHeader title={t("title")} subtitle={t("subtitle")} />
      {result.kind === "forbidden" ? <p>{t("forbidden")}</p> : <ContentManager initialItems={result.items} />}
    </>
  );
}
