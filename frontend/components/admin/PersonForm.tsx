"use client";

import { useState, type FormEvent } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/Button";
import type { AdminPersonDto, AdminPersonRequest } from "@/lib/api";
import styles from "./PersonForm.module.css";

const EMPTY: AdminPersonRequest = {
  generationNumber: null,
  firstName: "",
  firstNameNepali: null,
  middleName: null,
  middleNameNepali: null,
  lastName: "",
  lastNameNepali: null,
  nickname: null,
  gender: null,
  birthDate: null,
  deathDate: null,
  photoPath: null,
  birthPlace: null,
  currentAddress: null,
  gotra: null,
  notes: null,
};

interface PersonFormProps {
  initial?: AdminPersonDto;
  submitLabel: string;
  onSubmit: (body: AdminPersonRequest) => Promise<void>;
}

export function PersonForm({ initial, submitLabel, onSubmit }: PersonFormProps) {
  const t = useTranslations("adminPersonsPage.form");
  const [values, setValues] = useState<AdminPersonRequest>(initial ?? EMPTY);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<{ firstName?: string; lastName?: string }>({});

  function set<K extends keyof AdminPersonRequest>(key: K, value: AdminPersonRequest[K]) {
    setValues((current) => ({ ...current, [key]: value }));
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    const errors: typeof fieldErrors = {};
    if (!values.firstName.trim()) errors.firstName = t("firstNameRequired");
    if (!values.lastName.trim()) errors.lastName = t("lastNameRequired");
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) return;

    setSubmitting(true);
    setError(null);
    try {
      await onSubmit(values);
    } catch {
      setError(t("saveError"));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form className={styles.form} onSubmit={handleSubmit}>
      <div className={styles.section}>
        <h2>{t("nameSection")}</h2>
        <div className={styles.row}>
          <div className={styles.field}>
            <label htmlFor="firstName">{t("firstName")}</label>
            <input
              id="firstName"
              value={values.firstName}
              onChange={(event) => set("firstName", event.target.value)}
              required
            />
            {fieldErrors.firstName ? <span className={styles.fieldError}>{fieldErrors.firstName}</span> : null}
          </div>
          <div className={styles.field}>
            <label htmlFor="firstNameNepali">{t("firstNameNepali")}</label>
            <input
              id="firstNameNepali"
              value={values.firstNameNepali ?? ""}
              onChange={(event) => set("firstNameNepali", event.target.value || null)}
            />
          </div>
        </div>
        <div className={styles.row}>
          <div className={styles.field}>
            <label htmlFor="middleName">{t("middleName")}</label>
            <input
              id="middleName"
              value={values.middleName ?? ""}
              onChange={(event) => set("middleName", event.target.value || null)}
            />
          </div>
          <div className={styles.field}>
            <label htmlFor="middleNameNepali">{t("middleNameNepali")}</label>
            <input
              id="middleNameNepali"
              value={values.middleNameNepali ?? ""}
              onChange={(event) => set("middleNameNepali", event.target.value || null)}
            />
          </div>
        </div>
        <div className={styles.row}>
          <div className={styles.field}>
            <label htmlFor="lastName">{t("lastName")}</label>
            <input
              id="lastName"
              value={values.lastName}
              onChange={(event) => set("lastName", event.target.value)}
              required
            />
            {fieldErrors.lastName ? <span className={styles.fieldError}>{fieldErrors.lastName}</span> : null}
          </div>
          <div className={styles.field}>
            <label htmlFor="lastNameNepali">{t("lastNameNepali")}</label>
            <input
              id="lastNameNepali"
              value={values.lastNameNepali ?? ""}
              onChange={(event) => set("lastNameNepali", event.target.value || null)}
            />
          </div>
        </div>
        <div className={styles.row}>
          <div className={styles.field}>
            <label htmlFor="nickname">{t("nickname")}</label>
            <input id="nickname" value={values.nickname ?? ""} onChange={(event) => set("nickname", event.target.value || null)} />
          </div>
          <div className={styles.field}>
            <label htmlFor="gender">{t("gender")}</label>
            <select id="gender" value={values.gender ?? ""} onChange={(event) => set("gender", event.target.value || null)}>
              <option value="">{t("genderUnspecified")}</option>
              <option value="Male">{t("genderMale")}</option>
              <option value="Female">{t("genderFemale")}</option>
            </select>
          </div>
        </div>
      </div>

      <div className={styles.section}>
        <h2>{t("datesSection")}</h2>
        <div className={styles.row}>
          <div className={styles.field}>
            <label htmlFor="generationNumber">{t("generationNumber")}</label>
            <input
              id="generationNumber"
              type="number"
              value={values.generationNumber ?? ""}
              onChange={(event) => set("generationNumber", event.target.value === "" ? null : Number(event.target.value))}
            />
          </div>
          <div />
        </div>
        <div className={styles.row}>
          <div className={styles.field}>
            <label htmlFor="birthDate">{t("birthDate")}</label>
            <input
              id="birthDate"
              type="date"
              value={values.birthDate ?? ""}
              onChange={(event) => set("birthDate", event.target.value || null)}
            />
          </div>
          <div className={styles.field}>
            <label htmlFor="deathDate">{t("deathDate")}</label>
            <input
              id="deathDate"
              type="date"
              value={values.deathDate ?? ""}
              onChange={(event) => set("deathDate", event.target.value || null)}
            />
          </div>
        </div>
        <div className={styles.row}>
          <div className={styles.field}>
            <label htmlFor="birthPlace">{t("birthPlace")}</label>
            <input
              id="birthPlace"
              value={values.birthPlace ?? ""}
              onChange={(event) => set("birthPlace", event.target.value || null)}
            />
          </div>
          <div className={styles.field}>
            <label htmlFor="currentAddress">{t("currentAddress")}</label>
            <input
              id="currentAddress"
              value={values.currentAddress ?? ""}
              onChange={(event) => set("currentAddress", event.target.value || null)}
            />
          </div>
        </div>
        <div className={styles.row}>
          <div className={styles.field}>
            <label htmlFor="gotra">{t("gotra")}</label>
            <input id="gotra" value={values.gotra ?? ""} onChange={(event) => set("gotra", event.target.value || null)} />
          </div>
          <div />
        </div>
      </div>

      <div className={styles.section}>
        <h2>{t("notesSection")}</h2>
        <div className={styles.field}>
          <label htmlFor="notes">{t("notes")}</label>
          <textarea id="notes" rows={4} value={values.notes ?? ""} onChange={(event) => set("notes", event.target.value || null)} />
        </div>
      </div>

      <div className={styles.actions}>
        <Button type="submit" variant="primary" disabled={submitting}>
          {submitLabel}
        </Button>
        {error ? <span className={styles.errorNotice}>{error}</span> : null}
      </div>
    </form>
  );
}
