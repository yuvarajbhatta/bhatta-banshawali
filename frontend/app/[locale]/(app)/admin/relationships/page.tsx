import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { getTranslations } from "next-intl/server";
import { PageHeader } from "@/components/shell/PageHeader";
import { RelationshipManager } from "@/components/admin/RelationshipManager";
import { getAdminRelationships } from "@/lib/api";

export default async function AdminRelationshipsPage() {
  const t = await getTranslations("adminRelationshipsPage");
  const cookieStore = await cookies();
  const cookieHeader = cookieStore.getAll().map((c) => `${c.name}=${c.value}`).join("; ");

  const result = await getAdminRelationships(cookieHeader);
  if (result.kind === "unauthenticated") {
    redirect("/login");
  }

  return (
    <>
      <PageHeader title={t("title")} subtitle={t("subtitle")} />
      {result.kind === "forbidden" ? <p>{t("forbidden")}</p> : <RelationshipManager initialItems={result.items} />}
    </>
  );
}
