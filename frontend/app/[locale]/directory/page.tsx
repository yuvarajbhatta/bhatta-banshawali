import { getTranslations } from "next-intl/server";
import { PageShell } from "@/components/PageShell";
import { PersonSearch } from "@/components/PersonSearch";

export default async function DirectoryPage() {
  const t = await getTranslations("directoryPage");

  return (
    <PageShell title={t("title")}>
      <PersonSearch />
    </PageShell>
  );
}
