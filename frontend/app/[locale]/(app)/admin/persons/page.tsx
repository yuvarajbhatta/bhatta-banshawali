import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { getTranslations } from "next-intl/server";
import { Link } from "@/i18n/navigation";
import { Button } from "@/components/Button";
import { PageHeader } from "@/components/shell/PageHeader";
import { PersonQueueTable } from "@/components/admin/PersonQueueTable";
import { getAdminPersons } from "@/lib/api";
import queueStyles from "@/components/admin/QueueTable.module.css";

export default async function AdminPersonsPage({
  searchParams,
}: {
  searchParams: Promise<{ q?: string }>;
}) {
  const t = await getTranslations("adminPersonsPage");
  const { q } = await searchParams;
  const cookieStore = await cookies();
  const cookieHeader = cookieStore.getAll().map((c) => `${c.name}=${c.value}`).join("; ");

  const result = await getAdminPersons(cookieHeader, q);
  if (result.kind === "unauthenticated") {
    redirect("/login");
  }

  return (
    <>
      <PageHeader
        title={t("title")}
        subtitle={t("subtitle")}
        actions={
          <Link href="/admin/persons/new">
            <Button variant="primary">{t("addPerson")}</Button>
          </Link>
        }
      />

      {result.kind === "forbidden" ? (
        <p>{t("forbidden")}</p>
      ) : (
        <>
          <form action="/admin/persons" method="get" className={queueStyles.searchForm}>
            <input
              type="search"
              name="q"
              defaultValue={q ?? ""}
              placeholder={t("searchPlaceholder")}
              aria-label={t("searchPlaceholder")}
              className={queueStyles.searchInput}
            />
          </form>
          <PersonQueueTable initialItems={result.items} />
        </>
      )}
    </>
  );
}
