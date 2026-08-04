"use client";

import { useMemo, useState } from "react";
import { useTranslations } from "next-intl";
import { ChevronDown, Search } from "lucide-react";
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
  expanded: boolean;
  mode: Mode;
  status: "idle" | "saving" | "error";
  error?: string;
  editForm: EditForm;
  pickerPerson: { id: number; name: string } | null;
}

function emptyRowState(account: AdminUserAccountDto): RowState {
  return {
    expanded: false,
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

// What to show as the account's name on the collapsed row -- the linked
// family record's name if there is one, else whatever they typed at
// signup, else just the email (in which case there's no separate email
// line to show below it, since that'd just repeat the same string).
function displayName(account: AdminUserAccountDto): string {
  return account.linkedPersonName ?? account.submittedFullName ?? account.email;
}

interface AccountsManagerProps {
  initialItems: AdminUserAccountDto[];
  // The logged-in admin's own email -- null for a legacy AppUser login,
  // which has no email and so can never match any account row. Used to
  // disable actions the backend already self-blocks (revoke own admin
  // access, disable own account) instead of letting them fail with an
  // error only after the click.
  currentUserEmail: string | null;
}

export function AccountsManager({ initialItems, currentUserEmail }: AccountsManagerProps) {
  const t = useTranslations("adminAccountsPage");
  const [items, setItems] = useState(initialItems);
  const [rowStates, setRowStates] = useState<Record<number, RowState>>({});
  const [query, setQuery] = useState("");

  // Filters the accounts already loaded for this page -- deliberately not
  // the header's global /api/v1/persons search (that's the whole family
  // tree, not "who has a login here"), and no extra request needed since
  // listAll() already brought every account down at once.
  const visibleItems = useMemo(() => {
    const needle = query.trim().toLowerCase();
    if (!needle) return items;
    return items.filter((account) => {
      const haystack = [account.email, account.submittedFullName, account.linkedPersonName]
        .filter((value): value is string => Boolean(value))
        .join(" ")
        .toLowerCase();
      return haystack.includes(needle);
    });
  }, [items, query]);

  function rowState(account: AdminUserAccountDto): RowState {
    return rowStates[account.id] ?? emptyRowState(account);
  }

  function toggleExpanded(account: AdminUserAccountDto) {
    setRow(account, { expanded: !rowState(account).expanded });
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
      <div className={styles.searchWrapper}>
        <Search size={16} aria-hidden="true" />
        <input
          type="search"
          className={styles.searchInput}
          placeholder={t("searchPlaceholder")}
          aria-label={t("searchPlaceholder")}
          value={query}
          onChange={(event) => setQuery(event.target.value)}
        />
      </div>

      {visibleItems.length === 0 ? <div className={queueStyles.empty}>{t("noResults")}</div> : null}

      {visibleItems.map((account) => {
        const state = rowState(account);
        const isDisabled = account.status === "DISABLED";
        const isLinked = account.linkedPersonId != null;
        const hasSignupRecord = account.submittedFullName != null;
        const saving = state.status === "saving";
        const isSelf = currentUserEmail != null && account.email.toLowerCase() === currentUserEmail.toLowerCase();
        const name = displayName(account);
        const showEmailLine = name !== account.email;
        const expanded = state.expanded || state.mode !== "view";

        return (
          <div key={account.id} className={styles.card}>
            <button
              type="button"
              className={styles.cardHeader}
              onClick={() => toggleExpanded(account)}
              aria-expanded={expanded}
            >
              <div className={styles.identity}>
                <p className={styles.name}>{name}</p>
                {showEmailLine ? <p className={styles.emailSmall}>{account.email}</p> : null}
              </div>
              <div className={styles.headerRight}>
                <div className={styles.badges}>
                  {account.isAdmin ? <span className={styles.adminBadge}>{t("adminBadge")}</span> : null}
                  <span className={isDisabled ? `${styles.statusBadge} ${styles.statusDisabled}` : styles.statusBadge}>
                    {t(`status.${account.status}`)}
                  </span>
                </div>
                <ChevronDown size={18} aria-hidden="true" className={expanded ? styles.chevronOpen : styles.chevron} />
              </div>
            </button>

            {expanded ? (
              <div className={styles.expandedContent}>
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

                {state.mode === "editInfo" ? (
                  <div className={styles.editForm}>
                    <label>
                      {t("fields.fullName")}
                      <input
                        type="text"
                        value={state.editForm.fullName}
                        onChange={(event) =>
                          setRow(account, { editForm: { ...state.editForm, fullName: event.target.value } })
                        }
                      />
                    </label>
                    <label>
                      {t("fields.fatherName")}
                      <input
                        type="text"
                        value={state.editForm.fatherName}
                        onChange={(event) =>
                          setRow(account, { editForm: { ...state.editForm, fatherName: event.target.value } })
                        }
                      />
                    </label>
                    <label>
                      {t("fields.motherName")}
                      <input
                        type="text"
                        value={state.editForm.motherName}
                        onChange={(event) =>
                          setRow(account, { editForm: { ...state.editForm, motherName: event.target.value } })
                        }
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
                      size="xs"
                      onClick={() => setRow(account, { mode: "editInfo", status: "idle" })}
                      disabled={saving || !hasSignupRecord}
                    >
                      {t("editInfo")}
                    </Button>
                    {isLinked ? (
                      <Button variant="secondary" size="xs" onClick={() => handleUnlink(account)} disabled={saving}>
                        {t("unlink")}
                      </Button>
                    ) : (
                      <Button
                        variant="secondary"
                        size="xs"
                        onClick={() => setRow(account, { mode: "link", status: "idle" })}
                        disabled={saving}
                      >
                        {t("link")}
                      </Button>
                    )}
                    {isLinked && hasSignupRecord ? (
                      <Button variant="secondary" size="xs" onClick={() => handleApplyToPerson(account)} disabled={saving}>
                        {t("applyToPerson")}
                      </Button>
                    ) : null}
                    {account.isAdmin ? (
                      <Button
                        variant="destructive"
                        size="xs"
                        onClick={() => handleRevokeAdmin(account)}
                        disabled={saving || isSelf}
                        title={isSelf ? t("cannotActOnOwnAccount") : undefined}
                      >
                        {t("revokeAdmin")}
                      </Button>
                    ) : null}
                    <Button
                      variant={isDisabled ? "secondary" : "destructive"}
                      size="xs"
                      onClick={() => handleToggleStatus(account)}
                      disabled={saving || (isSelf && !isDisabled)}
                      title={isSelf && !isDisabled ? t("cannotActOnOwnAccount") : undefined}
                    >
                      {isDisabled ? t("enable") : t("disable")}
                    </Button>
                    <Button
                      variant="destructive"
                      size="xs"
                      onClick={() => handleDelete(account)}
                      disabled={saving || account.isAdmin}
                      title={account.isAdmin ? t("deleteBlockedForAdmin") : undefined}
                    >
                      {t("delete")}
                    </Button>
                  </div>
                )}

                {state.status === "error" ? <span className={styles.error}>{state.error}</span> : null}
              </div>
            ) : null}
          </div>
        );
      })}
    </div>
  );
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString(undefined, { year: "numeric", month: "short", day: "numeric" });
}
