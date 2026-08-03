"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/Button";
import {
  createAdminAnnouncement,
  deleteAdminAnnouncement,
  publishAnnouncement,
  unpublishAnnouncement,
  updateAdminAnnouncement,
  AdminActionError,
  type AdminAnnouncementDto,
  type AnnouncementCategory,
  type AnnouncementPhotoDto,
  type AdminAnnouncementRequest,
} from "@/lib/api";
import { AnnouncementPhotoManager } from "./AnnouncementPhotoManager";
import styles from "./AnnouncementManager.module.css";
import queueStyles from "./QueueTable.module.css";

const CATEGORIES: AnnouncementCategory[] = ["APP_UPDATE", "FAMILY_NEWS", "CELEBRATION", "OBITUARY", "HELP_REQUEST"];

interface AnnouncementForm {
  category: AnnouncementCategory;
  titleEn: string;
  titleNe: string;
  bodyEn: string;
  bodyNe: string;
  pinned: boolean;
}

const EMPTY_FORM: AnnouncementForm = {
  category: "FAMILY_NEWS",
  titleEn: "",
  titleNe: "",
  bodyEn: "",
  bodyNe: "",
  pinned: false,
};

function formFor(post: AdminAnnouncementDto): AnnouncementForm {
  return {
    category: post.category,
    titleEn: post.titleEn,
    titleNe: post.titleNe ?? "",
    bodyEn: post.bodyEn,
    bodyNe: post.bodyNe ?? "",
    pinned: post.pinned,
  };
}

function toRequest(form: AnnouncementForm): AdminAnnouncementRequest {
  return {
    category: form.category,
    titleEn: form.titleEn.trim(),
    titleNe: form.titleNe.trim() || undefined,
    bodyEn: form.bodyEn,
    bodyNe: form.bodyNe.trim() || undefined,
    pinned: form.pinned,
  };
}

interface RowState {
  editing: boolean;
  form: AnnouncementForm;
  status: "idle" | "saving" | "error";
  error?: string;
}

export function AnnouncementManager({ initialItems }: { initialItems: AdminAnnouncementDto[] }) {
  const t = useTranslations("adminNewsPage");
  const [items, setItems] = useState(initialItems);
  const [rowStates, setRowStates] = useState<Record<number, RowState>>({});
  const [creating, setCreating] = useState(false);
  const [createForm, setCreateForm] = useState<AnnouncementForm>(EMPTY_FORM);
  const [createStatus, setCreateStatus] = useState<"idle" | "saving" | "error">("idle");
  const [createError, setCreateError] = useState<string | null>(null);

  function rowState(post: AdminAnnouncementDto): RowState {
    return rowStates[post.id] ?? { editing: false, form: formFor(post), status: "idle" };
  }

  function setRow(post: AdminAnnouncementDto, patch: Partial<RowState>) {
    setRowStates((current) => ({
      ...current,
      [post.id]: { ...(current[post.id] ?? { editing: false, form: formFor(post), status: "idle" }), ...patch },
    }));
  }

  function updateItem(id: number, updated: AdminAnnouncementDto) {
    setItems((current) => current.map((item) => (item.id === id ? updated : item)));
  }

  function updatePhotos(post: AdminAnnouncementDto, photos: AnnouncementPhotoDto[]) {
    updateItem(post.id, { ...post, photos });
  }

  async function handleCreate() {
    setCreateStatus("saving");
    setCreateError(null);
    try {
      const created = await createAdminAnnouncement(toRequest(createForm));
      setItems((current) => [created, ...current]);
      setCreating(false);
      setCreateForm(EMPTY_FORM);
      setCreateStatus("idle");
    } catch (error) {
      setCreateStatus("error");
      setCreateError(error instanceof AdminActionError ? error.message : t("actionError"));
    }
  }

  async function handleSave(post: AdminAnnouncementDto) {
    const form = rowState(post).form;
    setRow(post, { status: "saving" });
    try {
      const updated = await updateAdminAnnouncement(post.id, toRequest(form));
      updateItem(post.id, { ...updated, photos: post.photos });
      setRow(post, { status: "idle", editing: false });
    } catch (error) {
      setRow(post, { status: "error", error: error instanceof AdminActionError ? error.message : t("actionError") });
    }
  }

  async function handleTransition(post: AdminAnnouncementDto, action: (id: number) => Promise<AdminAnnouncementDto>) {
    setRow(post, { status: "saving" });
    try {
      const updated = await action(post.id);
      updateItem(post.id, { ...updated, photos: post.photos });
      setRow(post, { status: "idle" });
    } catch (error) {
      setRow(post, { status: "error", error: error instanceof AdminActionError ? error.message : t("actionError") });
    }
  }

  async function handleDelete(post: AdminAnnouncementDto) {
    if (!window.confirm(t("deleteConfirm", { title: post.titleEn }))) return;
    setRow(post, { status: "saving" });
    try {
      await deleteAdminAnnouncement(post.id);
      setItems((current) => current.filter((item) => item.id !== post.id));
    } catch (error) {
      setRow(post, { status: "error", error: error instanceof AdminActionError ? error.message : t("actionError") });
    }
  }

  return (
    <div className={styles.wrapper}>
      <div className={styles.toolbar}>
        <Button variant="primary" onClick={() => setCreating((current) => !current)}>
          {creating ? t("cancel") : t("newAnnouncement")}
        </Button>
      </div>

      {creating ? (
        <div className={styles.card}>
          <AnnouncementFields form={createForm} onChange={setCreateForm} t={t} />
          <div className={styles.actionsRow}>
            <Button variant="primary" onClick={handleCreate} disabled={createStatus === "saving"}>
              {t("create")}
            </Button>
            <Button variant="ghost" onClick={() => setCreating(false)} disabled={createStatus === "saving"}>
              {t("cancel")}
            </Button>
          </div>
          {createStatus === "error" ? <span className={styles.error}>{createError}</span> : null}
        </div>
      ) : null}

      {items.length === 0 ? (
        <div className={queueStyles.empty}>{t("empty")}</div>
      ) : (
        <div className={styles.list}>
          {items.map((post) => {
            const state = rowState(post);
            const saving = state.status === "saving";

            return (
              <div key={post.id} className={styles.card}>
                <div className={styles.header}>
                  <div className={styles.info}>
                    <p className={styles.title}>
                      {post.pinned ? "📌 " : ""}
                      {post.titleEn}
                    </p>
                    <p className={styles.meta}>
                      {t(`categories.${post.category}`)} · {t("updated", { date: formatDate(post.updatedAt) })}
                    </p>
                  </div>
                  <span className={`${styles.statusBadge} ${styles[`status${post.status}`]}`}>
                    {t(`status.${post.status}`)}
                  </span>
                </div>

                {state.editing ? (
                  <>
                    <AnnouncementFields form={state.form} onChange={(form) => setRow(post, { form })} t={t} />
                    <div className={styles.actionsRow}>
                      <Button variant="primary" onClick={() => handleSave(post)} disabled={saving}>
                        {t("save")}
                      </Button>
                      <Button variant="ghost" onClick={() => setRow(post, { editing: false, status: "idle" })} disabled={saving}>
                        {t("cancel")}
                      </Button>
                    </div>
                  </>
                ) : (
                  <div className={styles.actionsRow}>
                    <Button
                      variant="secondary"
                      onClick={() => setRow(post, { editing: true, form: formFor(post), status: "idle" })}
                      disabled={saving}
                    >
                      {t("edit")}
                    </Button>
                    {post.status !== "PUBLISHED" ? (
                      <Button variant="primary" onClick={() => handleTransition(post, publishAnnouncement)} disabled={saving}>
                        {t("publish")}
                      </Button>
                    ) : (
                      <Button variant="secondary" onClick={() => handleTransition(post, unpublishAnnouncement)} disabled={saving}>
                        {t("unpublish")}
                      </Button>
                    )}
                    <Button variant="destructive" onClick={() => handleDelete(post)} disabled={saving}>
                      {t("delete")}
                    </Button>
                  </div>
                )}

                {state.status === "error" ? <span className={styles.error}>{state.error}</span> : null}

                <AnnouncementPhotoManager
                  postId={post.id}
                  photos={post.photos}
                  onPhotosChange={(photos) => updatePhotos(post, photos)}
                />
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

function AnnouncementFields({
  form,
  onChange,
  t,
}: {
  form: AnnouncementForm;
  onChange: (form: AnnouncementForm) => void;
  t: ReturnType<typeof useTranslations>;
}) {
  return (
    <div className={styles.form}>
      <label>
        {t("fields.category")}
        <select
          value={form.category}
          onChange={(event) => onChange({ ...form, category: event.target.value as AnnouncementCategory })}
        >
          {CATEGORIES.map((category) => (
            <option key={category} value={category}>
              {t(`categories.${category}`)}
            </option>
          ))}
        </select>
      </label>
      <div className={styles.formRow}>
        <label>
          {t("fields.titleEn")}
          <input type="text" value={form.titleEn} onChange={(event) => onChange({ ...form, titleEn: event.target.value })} />
        </label>
        <label>
          {t("fields.titleNe")}
          <input type="text" value={form.titleNe} onChange={(event) => onChange({ ...form, titleNe: event.target.value })} />
        </label>
      </div>
      <div className={styles.formRow}>
        <label>
          {t("fields.bodyEn")}
          <textarea rows={6} value={form.bodyEn} onChange={(event) => onChange({ ...form, bodyEn: event.target.value })} />
        </label>
        <label>
          {t("fields.bodyNe")}
          <textarea rows={6} value={form.bodyNe} onChange={(event) => onChange({ ...form, bodyNe: event.target.value })} />
        </label>
      </div>
      <label className={styles.checkboxLabel}>
        <input type="checkbox" checked={form.pinned} onChange={(event) => onChange({ ...form, pinned: event.target.checked })} />
        {t("fields.pinned")}
      </label>
      <p className={styles.hint}>{t("bodyHint")}</p>
    </div>
  );
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString(undefined, { year: "numeric", month: "short", day: "numeric" });
}
