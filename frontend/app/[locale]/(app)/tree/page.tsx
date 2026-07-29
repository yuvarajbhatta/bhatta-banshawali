import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { getTranslations } from "next-intl/server";
import { PageHeader } from "@/components/shell/PageHeader";
import { TreeExplorer } from "@/components/family-tree/TreeExplorer";
import { getFamilyTree } from "@/lib/api";

export default async function FamilyTreePage({
  searchParams,
}: {
  searchParams: Promise<{ focus?: string }>;
}) {
  const t = await getTranslations("treePage");
  const { focus } = await searchParams;
  const cookieStore = await cookies();
  const cookieHeader = cookieStore.getAll().map((c) => `${c.name}=${c.value}`).join("; ");

  const result = await getFamilyTree(cookieHeader);

  if (result.kind === "unauthenticated") {
    redirect("/login");
  }

  const focusId = focus ? Number.parseInt(focus, 10) : null;

  return (
    <>
      <PageHeader title={t("title")} subtitle={t("subtitle")} />
      <TreeExplorer people={result.tree.nodes} initialFocusId={Number.isNaN(focusId as number) ? null : focusId} />
    </>
  );
}
