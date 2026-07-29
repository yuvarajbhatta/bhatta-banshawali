import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { getTranslations } from "next-intl/server";
import { PageHeader } from "@/components/shell/PageHeader";
import { PersonSearch } from "@/components/PersonSearch";
import { getMemberProfile } from "@/lib/api";

export default async function DirectoryPage() {
  const t = await getTranslations("directoryPage");
  const cookieStore = await cookies();
  const cookieHeader = cookieStore.getAll().map((c) => `${c.name}=${c.value}`).join("; ");

  // Only used here to check whether there's a valid session at all --
  // this page is open to any authenticated account (ADMIN or USER, same
  // as the legacy Thymeleaf /persons search), not just linked members,
  // so "no-account" (an AppUser admin) is fine; only "unauthenticated"
  // should bounce to /login. Without this gate, an anonymous visitor
  // would see a working-looking search box where every search silently
  // 401s and renders as "no results" instead of an honest login prompt.
  const authCheck = await getMemberProfile(cookieHeader);
  if (authCheck.kind === "unauthenticated") {
    redirect("/login");
  }

  return (
    <>
      <PageHeader title={t("title")} />
      <PersonSearch />
    </>
  );
}
