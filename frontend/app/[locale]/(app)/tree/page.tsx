import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { getTranslations } from "next-intl/server";
import { PageHeader } from "@/components/shell/PageHeader";
import { TreeExplorer } from "@/components/family-tree/TreeExplorer";
import { getFamilyTree, getMemberProfile } from "@/lib/api";
import styles from "./page.module.css";

export default async function FamilyTreePage({
  searchParams,
}: {
  searchParams: Promise<{ focus?: string }>;
}) {
  const t = await getTranslations("treePage");
  const { focus } = await searchParams;
  const cookieStore = await cookies();
  const cookieHeader = cookieStore.getAll().map((c) => `${c.name}=${c.value}`).join("; ");

  const [result, profileResult] = await Promise.all([getFamilyTree(cookieHeader), getMemberProfile(cookieHeader)]);

  if (result.kind === "unauthenticated" || profileResult.kind === "unauthenticated") {
    redirect("/login");
  }

  const focusId = focus ? Number.parseInt(focus, 10) : null;

  // Admins (AppUser login, no-account) and members not yet linked to a
  // Person just don't get the "path to you" highlight in the tree --
  // everything else on this page works the same either way.
  const selfId =
    profileResult.kind === "ok" && profileResult.profile.linked && profileResult.profile.person
      ? profileResult.profile.person.id
      : null;

  return (
    <>
      <PageHeader title={t("title")} subtitle={t("subtitle")} titleClassName={styles.compactTitle} />
      <TreeExplorer
        people={result.tree.nodes}
        initialFocusId={Number.isNaN(focusId as number) ? null : focusId}
        rootPersonId={result.tree.rootPersonId}
        selfId={selfId}
      />
    </>
  );
}
