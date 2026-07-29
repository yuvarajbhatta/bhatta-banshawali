"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useRouter } from "@/i18n/navigation";
import { Link } from "@/i18n/navigation";
import { deleteAdminPerson, type AdminPersonDto } from "@/lib/api";
import styles from "./QueueTable.module.css";

export function PersonQueueTable({ initialItems }: { initialItems: AdminPersonDto[] }) {
  const t = useTranslations("adminPersonsPage");
  const router = useRouter();
  const [items, setItems] = useState(initialItems);
  const [pendingId, setPendingId] = useState<number | null>(null);
  const [errorId, setErrorId] = useState<number | null>(null);

  async function handleDelete(person: AdminPersonDto) {
    const name = `${person.firstName} ${person.lastName}`.trim();
    if (!window.confirm(t("deleteConfirm", { name }))) {
      return;
    }
    setPendingId(person.id);
    setErrorId(null);
    try {
      await deleteAdminPerson(person.id);
      setItems((current) => current.filter((item) => item.id !== person.id));
      router.refresh();
    } catch {
      setErrorId(person.id);
    } finally {
      setPendingId(null);
    }
  }

  if (items.length === 0) {
    return <div className={styles.empty}>{t("empty")}</div>;
  }

  return (
    <div className={styles.tableWrapper}>
      <table className={styles.table}>
        <thead>
          <tr>
            <th>{t("columns.name")}</th>
            <th>{t("columns.generation")}</th>
            <th>{t("columns.birthDate")}</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {items.map((person) => (
            <tr key={person.id}>
              <td>
                <Link href={`/directory/${person.id}`} className={styles.reviewLink}>
                  {`${person.firstName} ${person.lastName}`.trim()}
                </Link>
              </td>
              <td>{person.generationNumber ?? "—"}</td>
              <td>{person.birthDate ?? "—"}</td>
              <td>
                <div className={styles.rowActions}>
                  <Link href={`/admin/persons/${person.id}/edit`} className={styles.reviewLink}>
                    {t("edit")}
                  </Link>
                  <button
                    type="button"
                    className={styles.reviewLink}
                    onClick={() => handleDelete(person)}
                    disabled={pendingId === person.id}
                  >
                    {t("delete")}
                  </button>
                </div>
                {errorId === person.id ? <p>{t("deleteError")}</p> : null}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
