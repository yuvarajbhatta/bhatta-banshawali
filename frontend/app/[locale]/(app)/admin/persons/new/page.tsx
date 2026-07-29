import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { getTranslations } from "next-intl/server";
import { PageHeader } from "@/components/shell/PageHeader";
import { NewPersonForm } from "@/components/admin/NewPersonForm";
import { getAdminSummary, getMemberProfile } from "@/lib/api";

export default async function NewPersonPage() {
  const t = await getTranslations("adminPersonsPage");
  const cookieStore = await cookies();
  const cookieHeader = cookieStore.getAll().map((c) => `${c.name}=${c.value}`).join("; ");

  const [auth, adminSummary] = await Promise.all([getMemberProfile(cookieHeader), getAdminSummary(cookieHeader)]);
  if (auth.kind === "unauthenticated") {
    redirect("/login");
  }

  return (
    <>
      <PageHeader title={t("newTitle")} />
      {adminSummary ? <NewPersonForm /> : <p>{t("forbidden")}</p>}
    </>
  );
}
