"use client";

import { useState, type FormEvent } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/Button";
import { CORRECTABLE_PERSON_FIELDS, CorrectionError, submitCorrection, type CorrectablePersonField } from "@/lib/api";
import styles from "./CorrectionForm.module.css";

const DATE_FIELDS: ReadonlySet<CorrectablePersonField> = new Set(["BIRTH_DATE", "DEATH_DATE"]);
const NUMBER_FIELDS: ReadonlySet<CorrectablePersonField> = new Set(["GENERATION_NUMBER"]);

export function CorrectionForm({ personId }: { personId: number }) {
  const t = useTranslations("personDetailPage.correction");
  const [open, setOpen] = useState(false);
  const [field, setField] = useState<CorrectablePersonField>("FIRST_NAME");
  const [proposedValue, setProposedValue] = useState("");
  const [reason, setReason] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [submitted, setSubmitted] = useState(false);

  const inputType = DATE_FIELDS.has(field) ? "date" : NUMBER_FIELDS.has(field) ? "number" : "text";

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await submitCorrection(personId, { field, proposedValue, reason });
      setSubmitted(true);
    } catch (err) {
      setError(err instanceof CorrectionError ? err.message : t("errorGeneric"));
    } finally {
      setSubmitting(false);
    }
  }

  if (!open) {
    return (
      <button type="button" className={styles.toggle} onClick={() => setOpen(true)}>
        {t("toggle")}
      </button>
    );
  }

  if (submitted) {
    return <p className={styles.success}>{t("success")}</p>;
  }

  return (
    <form className={styles.form} onSubmit={handleSubmit}>
      <h3>{t("title")}</h3>

      {error ? <div className={styles.error}>{error}</div> : null}

      <label className={styles.label}>
        {t("fieldLabel")}
        <select value={field} onChange={(event) => setField(event.target.value as CorrectablePersonField)}>
          {CORRECTABLE_PERSON_FIELDS.map((value) => (
            <option key={value} value={value}>
              {t(`fields.${value}`)}
            </option>
          ))}
        </select>
      </label>

      <label className={styles.label}>
        {t("valueLabel")}
        <input
          type={inputType}
          value={proposedValue}
          onChange={(event) => setProposedValue(event.target.value)}
          required
        />
      </label>

      <label className={styles.label}>
        {t("reasonLabel")}
        <textarea
          value={reason}
          onChange={(event) => setReason(event.target.value)}
          placeholder={t("reasonPlaceholder")}
          rows={3}
          required
        />
      </label>

      <Button type="submit" variant="primary" disabled={submitting}>
        {submitting ? t("submitting") : t("submit")}
      </Button>
    </form>
  );
}
