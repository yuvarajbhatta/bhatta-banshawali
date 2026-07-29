"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/Button";
import { linkAccountToPerson, type UnlinkedAccountDto } from "@/lib/api";
import { PersonPicker } from "./PersonPicker";
import styles from "./UnlinkedAccountsManager.module.css";
import queueStyles from "./QueueTable.module.css";

type RowState = { person: { id: number; name: string } | null; status: "idle" | "saving" | "done" | "error" };

export function UnlinkedAccountsManager({ initialItems }: { initialItems: UnlinkedAccountDto[] }) {
  const t = useTranslations("adminUnlinkedAccountsPage");
  const [items, setItems] = useState(initialItems);
  const [rowStates, setRowStates] = useState<Record<number, RowState>>({});

  function rowState(id: number): RowState {
    return rowStates[id] ?? { person: null, status: "idle" };
  }

  function setRow(id: number, patch: Partial<RowState>) {
    setRowStates((current) => ({ ...current, [id]: { ...rowState(id), ...patch } }));
  }

  async function handleLink(account: UnlinkedAccountDto) {
    const state = rowState(account.userAccountId);
    if (!state.person) return;
    setRow(account.userAccountId, { status: "saving" });
    try {
      await linkAccountToPerson(account.userAccountId, state.person.id);
      setRow(account.userAccountId, { status: "done" });
      setItems((current) => current.filter((item) => item.userAccountId !== account.userAccountId));
    } catch {
      setRow(account.userAccountId, { status: "error" });
    }
  }

  if (items.length === 0) {
    return <div className={queueStyles.empty}>{t("empty")}</div>;
  }

  return (
    <div className={styles.list}>
      {items.map((account) => {
        const state = rowState(account.userAccountId);
        return (
          <div key={account.userAccountId} className={styles.card}>
            <div className={styles.info}>
              <p className={styles.email}>{account.email}</p>
              {account.submittedFullName ? (
                <p className={styles.submitted}>
                  {t("submittedAs", { name: account.submittedFullName })}
                  {account.submittedFatherName ? ` · ${t("father")}: ${account.submittedFatherName}` : ""}
                  {account.submittedGrandfatherName ? ` · ${t("grandfather")}: ${account.submittedGrandfatherName}` : ""}
                </p>
              ) : (
                <p className={styles.submitted}>{t("noSignupRecord")}</p>
              )}
              <p className={styles.meta}>{formatDate(account.createdAt)}</p>
            </div>

            <div className={styles.linkArea}>
              <div className={styles.picker}>
                <PersonPicker
                  label={t("selectPerson")}
                  placeholder={t("selectPersonPlaceholder")}
                  clearLabel={t("clear")}
                  selected={state.person}
                  onChange={(person) => setRow(account.userAccountId, { person, status: "idle" })}
                />
              </div>
              <Button
                variant="primary"
                onClick={() => handleLink(account)}
                disabled={!state.person || state.status === "saving"}
              >
                {t("link")}
              </Button>
            </div>

            {state.status === "error" ? <span className={styles.error}>{t("linkError")}</span> : null}
          </div>
        );
      })}
    </div>
  );
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString(undefined, { year: "numeric", month: "short", day: "numeric" });
}
