"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/Button";
import {
  createAdminArticle,
  deleteAdminArticle,
  publishArticle,
  revertArticleToDraft,
  submitArticleForReview,
  unpublishArticle,
  updateAdminArticle,
  AdminActionError,
  type AdminArticleDto,
  type AdminArticleRequest,
} from "@/lib/api";
import styles from "./ContentManager.module.css";
import queueStyles from "./QueueTable.module.css";

interface ArticleForm {
  slug: string;
  titleEn: string;
  titleNe: string;
  bodyEn: string;
  bodyNe: string;
}

const EMPTY_FORM: ArticleForm = { slug: "", titleEn: "", titleNe: "", bodyEn: "", bodyNe: "" };

function formFor(article: AdminArticleDto): ArticleForm {
  return {
    slug: article.slug,
    titleEn: article.titleEn,
    titleNe: article.titleNe ?? "",
    bodyEn: article.bodyEn,
    bodyNe: article.bodyNe ?? "",
  };
}

function toRequest(form: ArticleForm): AdminArticleRequest {
  return {
    slug: form.slug.trim(),
    titleEn: form.titleEn.trim(),
    titleNe: form.titleNe.trim() || undefined,
    bodyEn: form.bodyEn,
    bodyNe: form.bodyNe.trim() || undefined,
  };
}

interface RowState {
  editing: boolean;
  form: ArticleForm;
  status: "idle" | "saving" | "error";
  error?: string;
}

export function ContentManager({ initialItems }: { initialItems: AdminArticleDto[] }) {
  const t = useTranslations("adminContentPage");
  const [items, setItems] = useState(initialItems);
  const [rowStates, setRowStates] = useState<Record<number, RowState>>({});
  const [creating, setCreating] = useState(false);
  const [createForm, setCreateForm] = useState<ArticleForm>(EMPTY_FORM);
  const [createStatus, setCreateStatus] = useState<"idle" | "saving" | "error">("idle");
  const [createError, setCreateError] = useState<string | null>(null);

  function rowState(article: AdminArticleDto): RowState {
    return rowStates[article.id] ?? { editing: false, form: formFor(article), status: "idle" };
  }

  function setRow(article: AdminArticleDto, patch: Partial<RowState>) {
    setRowStates((current) => ({
      ...current,
      [article.id]: { ...(current[article.id] ?? { editing: false, form: formFor(article), status: "idle" }), ...patch },
    }));
  }

  function updateItem(id: number, updated: AdminArticleDto) {
    setItems((current) => current.map((item) => (item.id === id ? updated : item)));
  }

  async function handleCreate() {
    setCreateStatus("saving");
    setCreateError(null);
    try {
      const created = await createAdminArticle(toRequest(createForm));
      setItems((current) => [created, ...current]);
      setCreating(false);
      setCreateForm(EMPTY_FORM);
      setCreateStatus("idle");
    } catch (error) {
      setCreateStatus("error");
      setCreateError(error instanceof AdminActionError ? error.message : t("actionError"));
    }
  }

  async function handleSave(article: AdminArticleDto) {
    const form = rowState(article).form;
    setRow(article, { status: "saving" });
    try {
      const updated = await updateAdminArticle(article.id, toRequest(form));
      updateItem(article.id, updated);
      setRow(article, { status: "idle", editing: false });
    } catch (error) {
      setRow(article, { status: "error", error: error instanceof AdminActionError ? error.message : t("actionError") });
    }
  }

  async function handleTransition(article: AdminArticleDto, action: (id: number) => Promise<AdminArticleDto>) {
    setRow(article, { status: "saving" });
    try {
      const updated = await action(article.id);
      updateItem(article.id, updated);
      setRow(article, { status: "idle" });
    } catch (error) {
      setRow(article, { status: "error", error: error instanceof AdminActionError ? error.message : t("actionError") });
    }
  }

  async function handleDelete(article: AdminArticleDto) {
    if (!window.confirm(t("deleteConfirm", { slug: article.slug }))) return;
    setRow(article, { status: "saving" });
    try {
      await deleteAdminArticle(article.id);
      setItems((current) => current.filter((item) => item.id !== article.id));
    } catch (error) {
      setRow(article, { status: "error", error: error instanceof AdminActionError ? error.message : t("actionError") });
    }
  }

  return (
    <div className={styles.wrapper}>
      <div className={styles.toolbar}>
        <Button variant="primary" onClick={() => setCreating((current) => !current)}>
          {creating ? t("cancel") : t("newArticle")}
        </Button>
      </div>

      {creating ? (
        <div className={styles.card}>
          <ArticleFields form={createForm} onChange={setCreateForm} t={t} />
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
          {items.map((article) => {
            const state = rowState(article);
            const saving = state.status === "saving";

            return (
              <div key={article.id} className={styles.card}>
                <div className={styles.header}>
                  <div className={styles.info}>
                    <p className={styles.title}>{article.titleEn}</p>
                    <p className={styles.meta}>
                      /{article.slug} · {t("updated", { date: formatDate(article.updatedAt) })}
                    </p>
                  </div>
                  <span className={`${styles.statusBadge} ${styles[`status${article.status}`]}`}>
                    {t(`status.${article.status}`)}
                  </span>
                </div>

                {state.editing ? (
                  <>
                    <ArticleFields form={state.form} onChange={(form) => setRow(article, { form })} t={t} />
                    <div className={styles.actionsRow}>
                      <Button variant="primary" onClick={() => handleSave(article)} disabled={saving}>
                        {t("save")}
                      </Button>
                      <Button variant="ghost" onClick={() => setRow(article, { editing: false, status: "idle" })} disabled={saving}>
                        {t("cancel")}
                      </Button>
                    </div>
                  </>
                ) : (
                  <div className={styles.actionsRow}>
                    <Button
                      variant="secondary"
                      onClick={() => setRow(article, { editing: true, form: formFor(article), status: "idle" })}
                      disabled={saving}
                    >
                      {t("edit")}
                    </Button>
                    {article.status === "DRAFT" ? (
                      <Button variant="secondary" onClick={() => handleTransition(article, submitArticleForReview)} disabled={saving}>
                        {t("submitForReview")}
                      </Button>
                    ) : null}
                    {article.status !== "PUBLISHED" ? (
                      <Button variant="primary" onClick={() => handleTransition(article, publishArticle)} disabled={saving}>
                        {t("publish")}
                      </Button>
                    ) : null}
                    {article.status === "PUBLISHED" ? (
                      <Button variant="secondary" onClick={() => handleTransition(article, unpublishArticle)} disabled={saving}>
                        {t("unpublish")}
                      </Button>
                    ) : null}
                    {article.status !== "DRAFT" ? (
                      <Button variant="secondary" onClick={() => handleTransition(article, revertArticleToDraft)} disabled={saving}>
                        {t("revertToDraft")}
                      </Button>
                    ) : null}
                    <Button variant="destructive" onClick={() => handleDelete(article)} disabled={saving}>
                      {t("delete")}
                    </Button>
                  </div>
                )}

                {state.status === "error" ? <span className={styles.error}>{state.error}</span> : null}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

function ArticleFields({
  form,
  onChange,
  t,
}: {
  form: ArticleForm;
  onChange: (form: ArticleForm) => void;
  t: ReturnType<typeof useTranslations>;
}) {
  return (
    <div className={styles.form}>
      <label>
        {t("fields.slug")}
        <input type="text" value={form.slug} onChange={(event) => onChange({ ...form, slug: event.target.value })} />
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
          <textarea rows={8} value={form.bodyEn} onChange={(event) => onChange({ ...form, bodyEn: event.target.value })} />
        </label>
        <label>
          {t("fields.bodyNe")}
          <textarea rows={8} value={form.bodyNe} onChange={(event) => onChange({ ...form, bodyNe: event.target.value })} />
        </label>
      </div>
      <p className={styles.hint}>{t("bodyHint")}</p>
    </div>
  );
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString(undefined, { year: "numeric", month: "short", day: "numeric" });
}
