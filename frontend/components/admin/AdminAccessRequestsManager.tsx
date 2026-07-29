"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/Button";
import { approveAdminAccessRequest, denyAdminAccessRequest, AdminActionError, type AdminAccessRequestDto } from "@/lib/api";
import styles from "./AdminAccessRequestsManager.module.css";
import queueStyles from "./QueueTable.module.css";

type RowState = { status: "idle" | "saving" | "error"; error?: string };

export function AdminAccessRequestsManager({ initialItems }: { initialItems: AdminAccessRequestDto[] }) {
  const t = useTranslations("adminAccessRequestsPage");
  const [items, setItems] = useState(initialItems);
  const [rowStates, setRowStates] = useState<Record<number, RowState>>({});

  function rowState(id: number): RowState {
    return rowStates[id] ?? { status: "idle" };
  }

  function setRow(id: number, patch: RowState) {
    setRowStates((current) => ({ ...current, [id]: patch }));
  }

  async function handleApprove(request: AdminAccessRequestDto) {
    setRow(request.id, { status: "saving" });
    try {
      await approveAdminAccessRequest(request.id);
      setItems((current) => current.filter((item) => item.id !== request.id));
    } catch (error) {
      setRow(request.id, { status: "error", error: error instanceof AdminActionError ? error.message : t("actionError") });
    }
  }

  async function handleDeny(request: AdminAccessRequestDto) {
    setRow(request.id, { status: "saving" });
    try {
      await denyAdminAccessRequest(request.id);
      setItems((current) => current.filter((item) => item.id !== request.id));
    } catch (error) {
      setRow(request.id, { status: "error", error: error instanceof AdminActionError ? error.message : t("actionError") });
    }
  }

  if (items.length === 0) {
    return <div className={queueStyles.empty}>{t("empty")}</div>;
  }

  return (
    <div className={styles.list}>
      {items.map((request) => {
        const state = rowState(request.id);
        const saving = state.status === "saving";
        return (
          <div key={request.id} className={styles.card}>
            <div className={styles.info}>
              <p className={styles.email}>{request.email}</p>
              <p className={styles.meta}>
                {request.linkedPersonName ?? t("noLinkedPerson")} · {t("requested", { date: formatDate(request.requestedAt) })}
              </p>
            </div>

            <div className={styles.actionsRow}>
              <Button variant="primary" onClick={() => handleApprove(request)} disabled={saving}>
                {t("approve")}
              </Button>
              <Button variant="destructive" onClick={() => handleDeny(request)} disabled={saving}>
                {t("deny")}
              </Button>
            </div>

            {state.status === "error" ? <span className={styles.error}>{state.error}</span> : null}
          </div>
        );
      })}
    </div>
  );
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString(undefined, { year: "numeric", month: "short", day: "numeric" });
}
