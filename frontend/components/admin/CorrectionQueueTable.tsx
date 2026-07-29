"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Link } from "@/i18n/navigation";
import { Button } from "@/components/Button";
import { approveCorrection, rejectCorrection, type AdminCorrectionSummaryDto } from "@/lib/api";
import { Badge, correctionStatusTone } from "./Badge";
import styles from "./QueueTable.module.css";

interface CorrectionQueueTableProps {
  initialItems: AdminCorrectionSummaryDto[];
  showActions: boolean;
}

export function CorrectionQueueTable({ initialItems, showActions }: CorrectionQueueTableProps) {
  const t = useTranslations("adminCorrectionsPage");
  const fieldT = useTranslations("personDetailPage.correction.fields");
  const [items, setItems] = useState(initialItems);
  const [pendingId, setPendingId] = useState<number | null>(null);
  const [errorId, setErrorId] = useState<number | null>(null);

  async function handleDecision(id: number, action: "approve" | "reject") {
    setPendingId(id);
    setErrorId(null);
    try {
      const updated = action === "approve" ? await approveCorrection(id, {}) : await rejectCorrection(id, {});
      setItems((current) => current.map((item) => (item.id === id ? updated : item)));
    } catch {
      setErrorId(id);
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
            <th>{t("person")}</th>
            <th>{t("field")}</th>
            <th>{t("currentValue")}</th>
            <th>{t("proposedValue")}</th>
            <th>{t("reason")}</th>
            <th>{t("submittedBy")}</th>
            {showActions ? <th /> : <th>{t("status")}</th>}
          </tr>
        </thead>
        <tbody>
          {items.map((item) => (
            <tr key={item.id}>
              <td>
                <Link href={`/directory/${item.personId}`} className={styles.reviewLink}>
                  {item.personName}
                </Link>
              </td>
              <td>{fieldT(item.field)}</td>
              <td>{item.currentValueSnapshot ?? t("notAdded")}</td>
              <td>{item.proposedValue}</td>
              <td>{item.reason}</td>
              <td>{item.submittedByEmail}</td>
              {showActions && item.status === "PENDING" ? (
                <td>
                  <div className={styles.rowActions}>
                    <Button
                      variant="primary"
                      onClick={() => handleDecision(item.id, "approve")}
                      disabled={pendingId === item.id}
                    >
                      {t("approve")}
                    </Button>
                    <Button
                      variant="destructive"
                      onClick={() => handleDecision(item.id, "reject")}
                      disabled={pendingId === item.id}
                    >
                      {t("reject")}
                    </Button>
                  </div>
                  {errorId === item.id ? <p>{t("actionError")}</p> : null}
                </td>
              ) : (
                <td>
                  <Badge tone={correctionStatusTone(item.status)}>{item.status}</Badge>
                </td>
              )}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
