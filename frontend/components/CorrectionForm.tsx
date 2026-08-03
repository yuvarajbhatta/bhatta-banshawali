"use client";

import { useEffect, useState, type FormEvent } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/Button";
import {
  CORRECTABLE_PERSON_FIELDS,
  CorrectionError,
  convertAdToBs,
  convertBsToAd,
  submitCorrection,
  type CorrectablePersonField,
} from "@/lib/api";
import styles from "./CorrectionForm.module.css";

const DATE_FIELDS: ReadonlySet<CorrectablePersonField> = new Set(["BIRTH_DATE", "DEATH_DATE"]);
const NUMBER_FIELDS: ReadonlySet<CorrectablePersonField> = new Set(["GENERATION_NUMBER"]);

export function CorrectionForm({ personId }: { personId: number }) {
  const t = useTranslations("personDetailPage.correction");
  const [open, setOpen] = useState(false);
  const [field, setField] = useState<CorrectablePersonField>("FIRST_NAME");
  const [proposedValue, setProposedValue] = useState("");
  const [bsYear, setBsYear] = useState("");
  const [bsMonth, setBsMonth] = useState("");
  const [bsDay, setBsDay] = useState("");
  const [bsError, setBsError] = useState<string | null>(null);
  const [reason, setReason] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [submitted, setSubmitted] = useState(false);

  const isDateField = DATE_FIELDS.has(field);
  const inputType = isDateField ? "date" : NUMBER_FIELDS.has(field) ? "number" : "text";

  function handleFieldChange(next: CorrectablePersonField) {
    setField(next);
    setProposedValue("");
    setBsYear("");
    setBsMonth("");
    setBsDay("");
    setBsError(null);
  }

  // AD -> BS: keep the BS fields in sync when the AD date changes, whether
  // typed directly or just derived by the BS -> AD effect below.
  useEffect(() => {
    if (!isDateField || !proposedValue) {
      return;
    }
    let cancelled = false;
    convertAdToBs(proposedValue)
      .then((bs) => {
        if (cancelled) return;
        const year = String(bs.year);
        const month = String(bs.month);
        const day = String(bs.day);
        setBsYear((prev) => (prev === year ? prev : year));
        setBsMonth((prev) => (prev === month ? prev : month));
        setBsDay((prev) => (prev === day ? prev : day));
      })
      .catch(() => {
        // AD date outside the calendar's supported BS range -- leave the
        // BS fields alone.
      });
    return () => {
      cancelled = true;
    };
  }, [proposedValue, isDateField]);

  const bsFieldsComplete = Boolean(bsYear && bsMonth && bsDay);

  // BS -> AD: once all three BS fields are filled, derive the AD date --
  // that's the single value actually submitted.
  useEffect(() => {
    if (!isDateField || !bsFieldsComplete) {
      return;
    }
    const year = Number(bsYear);
    const month = Number(bsMonth);
    const day = Number(bsDay);
    if (!Number.isInteger(year) || !Number.isInteger(month) || !Number.isInteger(day)) {
      return;
    }
    let cancelled = false;
    convertBsToAd(year, month, day)
      .then((ad) => {
        if (cancelled) return;
        setBsError(null);
        setProposedValue((prev) => (prev === ad.date ? prev : ad.date));
      })
      .catch(() => {
        if (!cancelled) {
          setBsError(t("dobBsInvalid"));
        }
      });
    return () => {
      cancelled = true;
    };
  }, [bsYear, bsMonth, bsDay, isDateField, bsFieldsComplete, t]);

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
        <select value={field} onChange={(event) => handleFieldChange(event.target.value as CorrectablePersonField)}>
          {CORRECTABLE_PERSON_FIELDS.map((value) => (
            <option key={value} value={value}>
              {t(`fields.${value}`)}
            </option>
          ))}
        </select>
      </label>

      {isDateField ? (
        <div className={styles.dobRow}>
          <div className={styles.dobGroup}>
            <span className={styles.dobGroupLabel}>{t("dobBs")}</span>
            <div className={styles.dobBsInputs}>
              <input
                type="number"
                inputMode="numeric"
                aria-label={t("dobBsYear")}
                placeholder={t("dobBsYear")}
                value={bsYear}
                onChange={(event) => setBsYear(event.target.value)}
                min={2000}
                max={2090}
              />
              <input
                type="number"
                inputMode="numeric"
                aria-label={t("dobBsMonth")}
                placeholder={t("dobBsMonth")}
                value={bsMonth}
                onChange={(event) => setBsMonth(event.target.value)}
                min={1}
                max={12}
              />
              <input
                type="number"
                inputMode="numeric"
                aria-label={t("dobBsDay")}
                placeholder={t("dobBsDay")}
                value={bsDay}
                onChange={(event) => setBsDay(event.target.value)}
                min={1}
                max={32}
              />
            </div>
            {bsFieldsComplete && bsError ? <div className={styles.fieldError}>{bsError}</div> : null}
          </div>

          <div className={styles.dobGroup}>
            <span className={styles.dobGroupLabel}>{t("dobAd")}</span>
            <input
              type="date"
              value={proposedValue}
              onChange={(event) => setProposedValue(event.target.value)}
              required
            />
          </div>
        </div>
      ) : (
        <label className={styles.label}>
          {t("valueLabel")}
          <input
            type={inputType}
            value={proposedValue}
            onChange={(event) => setProposedValue(event.target.value)}
            required
          />
        </label>
      )}

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
