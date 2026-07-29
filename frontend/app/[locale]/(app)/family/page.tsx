import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { getTranslations } from "next-intl/server";
import { PageHeader } from "@/components/shell/PageHeader";
import { YourFamilyExplorer } from "@/components/family/YourFamilyExplorer";
import { getFamilyTree, getMemberProfile } from "@/lib/api";

export default async function YourFamilyPage() {
  const t = await getTranslations("familyPage");
  const cookieStore = await cookies();
  const cookieHeader = cookieStore.getAll().map((c) => `${c.name}=${c.value}`).join("; ");

  const profileResult = await getMemberProfile(cookieHeader);
  if (profileResult.kind === "unauthenticated") {
    redirect("/login");
  }

  const selfId =
    profileResult.kind === "ok" && profileResult.profile.linked && profileResult.profile.person
      ? profileResult.profile.person.id
      : null;

  if (selfId == null) {
    return (
      <>
        <PageHeader title={t("title")} subtitle={t("subtitle")} />
        <p>{t("unlinked")}</p>
      </>
    );
  }

  const treeResult = await getFamilyTree(cookieHeader);
  if (treeResult.kind === "unauthenticated") {
    redirect("/login");
  }

  return (
    <>
      <PageHeader title={t("title")} subtitle={t("subtitle")} />
      <YourFamilyExplorer people={treeResult.tree.nodes} selfId={selfId} />
    </>
  );
}
