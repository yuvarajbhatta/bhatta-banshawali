import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { getTranslations } from "next-intl/server";
import { PageHeader } from "@/components/shell/PageHeader";
import { GenerationsList } from "@/components/admin/GenerationsList";
import { getAdminPersons } from "@/lib/api";

export default async function AdminGenerationsPage() {
  const t = await getTranslations("adminGenerationsPage");
  const cookieStore = await cookies();
  const cookieHeader = cookieStore.getAll().map((c) => `${c.name}=${c.value}`).join("; ");

  const result = await getAdminPersons(cookieHeader);
  if (result.kind === "unauthenticated") {
    redirect("/login");
  }

  return (
    <>
      <PageHeader title={t("title")} subtitle={t("subtitle")} />
      {result.kind === "forbidden" ? <p>{t("forbidden")}</p> : <GenerationsList people={result.items} />}
    </>
  );
}
