"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/Button";
import {
  applyAdminAccountSignupInfoToPerson,
  disableAdminAccount,
  deleteAdminAccount,
  enableAdminAccount,
  linkAdminAccount,
  revokeAdminAccessForAccount,
  unlinkAdminAccount,
  updateAdminAccountSignupInfo,
  AdminActionError,
  type AdminUserAccountDto,
} from "@/lib/api";
import { PersonPicker } from "./PersonPicker";
import styles from "./AccountsManager.module.css";
import queueStyles from "./QueueTable.module.css";

type Mode = "view" | "editInfo" | "link";

interface EditForm {
  fullName: string;
  fatherName: string;
  motherName: string;
  grandfatherName: string;
  dobAd: string;
}

interface RowState {
  mode: Mode;
  status: "idle" | "saving" | "error";
  error?: string;
  editForm: EditForm;
  pickerPerson: { id: number; name: string } | null;
}

function emptyRowState(account: AdminUserAccountDto): RowState {
  return {
    mode: "view",
    status: "idle",
    editForm: {
      fullName: account.submittedFullName ?? "",
      fatherName: account.submittedFatherName ?? "",
      motherName: account.submittedMotherName ?? "",
      grandfatherName: account.submittedGrandfatherName ?? "",
      dobAd: account.submittedDobAd ?? "",
    },
    pickerPerson: null,
  };
}

export function AccountsManager({ initialItems }: { initialItems: AdminUserAccountDto[] }) {
  const t = useTranslations("adminAccountsPage");
  const [items, setItems] = useState(initialItems);
  const [rowStates, setRowStates] = useState<Record<number, RowState>>({});

  function rowState(account: AdminUserAccountDto): RowState {
    return rowStates[account.id] ?? emptyRowState(account);
  }

  function setRow(account: AdminUserAccountDto, patch: Partial<RowState>) {
    setRowStates((current) => ({
      ...current,
      [account.id]: { ...(current[account.id] ?? emptyRowState(account)), ...patch },
    }));
  }

  function updateItem(id: number, patch: Partial<AdminUserAccountDto>) {
    setItems((current) => current.map((item) => (item.id === id ? { ...item, ...patch } : item)));
  }

  async function handleToggleStatus(account: AdminUserAccountDto) {
    setRow(account, { status: "saving" });
    try {
      if (account.status === "DISABLED") {
        await enableAdminAccount(account.id);
        updateItem(account.id, { status: "ACTIVE" });
      } else {
        await disableAdminAccount(account.id);
        updateItem(account.id, { status: "DISABLED" });
      }
      setRow(account, { status: "idle" });
    } catch (error) {
      setRow(account, { status: "error", error: error instanceof AdminActionError ? error.message : t("actionError") });
    }
  }

  async function handleSaveInfo(account: AdminUserAccountDto) {
    const form = rowState(account).editForm;
    setRow(account, { status: "saving" });
    try {
      await updateAdminAccountSignupInfo(account.id, {
        fullName: form.fullName.trim(),
        fatherName: form.fatherName.trim(),
        motherName: form.motherName.trim() || undefined,
        grandfatherName: form.grandfatherName.trim(),
        dobAd: form.dobAd || undefined,
      });
      updateItem(account.id, {
        submittedFullName: form.fullName.trim(),
        submittedFatherName: form.fatherName.trim(),
        submittedMotherName: form.motherName.trim() || null,
        submittedGrandfatherName: form.grandfatherName.trim(),
        submittedDobAd: form.dobAd || null,
      });
      setRow(account, { status: "idle", mode: "view" });
    } catch (error) {
      setRow(account, { status: "error", error: error instanceof AdminActionError ? error.message : t("actionError") });
    }
  }

  async function handleApplyToPerson(account: AdminUserAccountDto) {
    if (!window.confirm(t("applyToPersonConfirm", { name: account.linkedPersonName ?? "" }))) return;
    setRow(account, { status: "saving" });
    try {
      await applyAdminAccountSignupInfoToPerson(account.id);
      setRow(account, { status: "idle" });
    } catch (error) {
      setRow(account, { status: "error", error: error instanceof AdminActionError ? error.message : t("actionError") });
    }
  }

  async function handleRevokeAdmin(account: AdminUserAccountDto) {
    if (!window.confirm(t("revokeAdminConfirm", { email: account.email }))) return;
    setRow(account, { status: "saving" });
    try {
      await revokeAdminAccessForAccount(account.id);
      updateItem(account.id, { isAdmin: false });
      setRow(account, { status: "idle" });
    } catch (error) {
      setRow(account, { status: "error", error: error instanceof AdminActionError ? error.message : t("actionError") });
    }
  }

  async function handleLink(account: AdminUserAccountDto) {
    const person = rowState(account).pickerPerson;
    if (!person) return;
    setRow(account, { status: "saving" });
    try {
      await linkAdminAccount(account.id, person.id);
      updateItem(account.id, { linkedPersonId: person.id, linkedPersonName: person.name });
      setRow(account, { status: "idle", mode: "view", pickerPerson: null });
    } catch (error) {
      setRow(account, { status: "error", error: error instanceof AdminActionError ? error.message : t("actionError") });
    }
  }

  async function handleUnlink(account: AdminUserAccountDto) {
    if (!window.confirm(t("unlinkConfirm", { email: account.email }))) return;
    setRow(account, { status: "saving" });
    try {
      await unlinkAdminAccount(account.id);
      updateItem(account.id, { linkedPersonId: null, linkedPersonName: null });
      setRow(account, { status: "idle" });
    } catch (error) {
      setRow(account, { status: "error", error: error instanceof AdminActionError ? error.message : t("actionError") });
    }
  }

  async function handleDelete(account: AdminUserAccountDto) {
    if (!window.confirm(t("deleteConfirm", { email: account.email }))) return;
    setRow(account, { status: "saving" });
    try {
      await deleteAdminAccount(account.id);
      setItems((current) => current.filter((item) => item.id !== account.id));
    } catch (error) {
      setRow(account, { status: "error", error: error instanceof AdminActionError ? error.message : t("actionError") });
    }
  }

  if (items.length === 0) {
    return <div className={queueStyles.empty}>{t("empty")}</div>;
  }

  return (
    <div className={styles.list}>
      {items.map((account) => {
        const state = rowState(account);
        const isDisabled = account.status === "DISABLED";
        const isLinked = account.linkedPersonId != null;
        const hasSignupRecord = account.submittedFullName != null;
        const saving = state.status === "saving";

        return (
          <div key={account.id} className={styles.card}>
            <div className={styles.header}>
              <div className={styles.info}>
                <p className={styles.email}>{account.email}</p>
                <p className={styles.meta}>
                  {isLinked ? t("linkedTo", { name: account.linkedPersonName ?? "" }) : t("noLinkedPerson")} ·{" "}
                  {t("joined", { date: formatDate(account.createdAt) })}
                </p>
                {account.submittedFullName ? (
                  <p className={styles.submitted}>
                    {t("submittedAs", { name: account.submittedFullName })}
                    {account.submittedFatherName ? ` · ${t("father")}: ${account.submittedFatherName}` : ""}
                    {account.submittedMotherName ? ` · ${t("mother")}: ${account.submittedMotherName}` : ""}
                    {account.submittedGrandfatherName ? ` · ${t("grandfather")}: ${account.submittedGrandfatherName}` : ""}
                  </p>
                ) : null}
              </div>
              <div className={styles.badges}>
                {account.isAdmin ? <span className={styles.adminBadge}>{t("adminBadge")}</span> : null}
                <span className={isDisabled ? `${styles.statusBadge} ${styles.statusDisabled}` : styles.statusBadge}>
                  {t(`status.${account.status}`)}
                </span>
              </div>
            </div>

            {state.mode === "editInfo" ? (
              <div className={styles.editForm}>
                <label>
                  {t("fields.fullName")}
                  <input
                    type="text"
                    value={state.editForm.fullName}
                    onChange={(event) => setRow(account, { editForm: { ...state.editForm, fullName: event.target.value } })}
                  />
                </label>
                <label>
                  {t("fields.fatherName")}
                  <input
                    type="text"
                    value={state.editForm.fatherName}
                    onChange={(event) => setRow(account, { editForm: { ...state.editForm, fatherName: event.target.value } })}
                  />
                </label>
                <label>
                  {t("fields.motherName")}
                  <input
                    type="text"
                    value={state.editForm.motherName}
                    onChange={(event) => setRow(account, { editForm: { ...state.editForm, motherName: event.target.value } })}
                  />
                </label>
                <label>
                  {t("fields.grandfatherName")}
                  <input
                    type="text"
                    value={state.editForm.grandfatherName}
                    onChange={(event) =>
                      setRow(account, { editForm: { ...state.editForm, grandfatherName: event.target.value } })
                    }
                  />
                </label>
                <label>
                  {t("fields.dobAd")}
                  <input
                    type="date"
                    value={state.editForm.dobAd}
                    onChange={(event) => setRow(account, { editForm: { ...state.editForm, dobAd: event.target.value } })}
                  />
                </label>
                <div className={styles.actionsRow}>
                  <Button variant="primary" onClick={() => handleSaveInfo(account)} disabled={saving}>
                    {t("save")}
                  </Button>
                  <Button variant="ghost" onClick={() => setRow(account, { mode: "view", status: "idle" })} disabled={saving}>
                    {t("cancel")}
                  </Button>
                </div>
              </div>
            ) : state.mode === "link" ? (
              <div className={styles.linkArea}>
                <div className={styles.picker}>
                  <PersonPicker
                    label={t("selectPerson")}
                    placeholder={t("selectPersonPlaceholder")}
                    clearLabel={t("clear")}
                    selected={state.pickerPerson}
                    onChange={(person) => setRow(account, { pickerPerson: person })}
                  />
                </div>
                <div className={styles.actionsRow}>
                  <Button variant="primary" onClick={() => handleLink(account)} disabled={!state.pickerPerson || saving}>
                    {t("link")}
                  </Button>
                  <Button
                    variant="ghost"
                    onClick={() => setRow(account, { mode: "view", status: "idle", pickerPerson: null })}
                    disabled={saving}
                  >
                    {t("cancel")}
                  </Button>
                </div>
              </div>
            ) : (
              <div className={styles.actionsRow}>
                <Button
                  variant="secondary"
                  onClick={() => setRow(account, { mode: "editInfo", status: "idle" })}
                  disabled={saving || !hasSignupRecord}
                >
                  {t("editInfo")}
                </Button>
                {isLinked ? (
                  <Button variant="secondary" onClick={() => handleUnlink(account)} disabled={saving}>
                    {t("unlink")}
                  </Button>
                ) : (
                  <Button variant="secondary" onClick={() => setRow(account, { mode: "link", status: "idle" })} disabled={saving}>
                    {t("link")}
                  </Button>
                )}
                {isLinked && hasSignupRecord ? (
                  <Button variant="secondary" onClick={() => handleApplyToPerson(account)} disabled={saving}>
                    {t("applyToPerson")}
                  </Button>
                ) : null}
                {account.isAdmin ? (
                  <Button variant="destructive" onClick={() => handleRevokeAdmin(account)} disabled={saving}>
                    {t("revokeAdmin")}
                  </Button>
                ) : null}
                <Button variant={isDisabled ? "secondary" : "destructive"} onClick={() => handleToggleStatus(account)} disabled={saving}>
                  {isDisabled ? t("enable") : t("disable")}
                </Button>
                <Button variant="destructive" onClick={() => handleDelete(account)} disabled={saving}>
                  {t("delete")}
                </Button>
              </div>
            )}

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
