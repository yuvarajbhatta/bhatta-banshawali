import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { getTranslations } from "next-intl/server";
import { PageHeader } from "@/components/shell/PageHeader";
import { DuplicateCandidatesManager } from "@/components/admin/DuplicateCandidatesManager";
import { getAdminDuplicates } from "@/lib/api";

export default async function AdminDuplicatesPage() {
  const t = await getTranslations("adminDuplicatesPage");
  const cookieStore = await cookies();
  const cookieHeader = cookieStore.getAll().map((c) => `${c.name}=${c.value}`).join("; ");

  const result = await getAdminDuplicates(cookieHeader);
  if (result.kind === "unauthenticated") {
    redirect("/login");
  }

  return (
    <>
      <PageHeader title={t("title")} subtitle={t("subtitle")} />
      {result.kind === "forbidden" ? <p>{t("forbidden")}</p> : <DuplicateCandidatesManager initialItems={result.items} />}
    </>
  );
}
