"use client";

import { useEffect, useState, type FormEvent } from "react";
import { useLocale, useTranslations } from "next-intl";
import { Link } from "@/i18n/navigation";
import { Button } from "@/components/Button";
import { convertAdToBs, submitSignup, SignupError, type SignupRequest } from "@/lib/api";
import styles from "./SignupForm.module.css";

interface FormState {
  fullName: string;
  email: string;
  dobAd: string;
  placeOfBirth: string;
  fatherName: string;
  grandfatherName: string;
  motherName: string;
  ancestralVillage: string;
  familyBranch: string;
  knownRelativeName: string;
  invitationCode: string;
  password: string;
  confirmPassword: string;
  applicantNote: string;
  agreedToTerms: boolean;
}

const EMPTY_FORM: FormState = {
  fullName: "",
  email: "",
  dobAd: "",
  placeOfBirth: "",
  fatherName: "",
  grandfatherName: "",
  motherName: "",
  ancestralVillage: "",
  familyBranch: "",
  knownRelativeName: "",
  invitationCode: "",
  password: "",
  confirmPassword: "",
  applicantNote: "",
  agreedToTerms: false,
};

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

type FormErrors = Partial<Record<keyof FormState, string>>;

export function SignupForm() {
  const t = useTranslations("signupPage");
  const locale = useLocale();

  const [form, setForm] = useState<FormState>(EMPTY_FORM);
  const [errors, setErrors] = useState<FormErrors>({});
  const [submitting, setSubmitting] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);
  const [submitted, setSubmitted] = useState(false);
  const [bsPreview, setBsPreview] = useState<string | null>(null);

  useEffect(() => {
    if (!form.dobAd) {
      return;
    }
    let cancelled = false;
    convertAdToBs(form.dobAd)
      .then((bs) => {
        if (!cancelled) {
          setBsPreview(`${bs.year}-${String(bs.month).padStart(2, "0")}-${String(bs.day).padStart(2, "0")}`);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setBsPreview(null);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [form.dobAd]);

  function update<K extends keyof FormState>(field: K, value: FormState[K]) {
    setForm((prev) => ({ ...prev, [field]: value }));
  }

  function validate(): FormErrors {
    const next: FormErrors = {};
    if (!form.fullName.trim()) next.fullName = t("errors.fullNameRequired");
    if (!form.email.trim()) next.email = t("errors.emailRequired");
    else if (!EMAIL_PATTERN.test(form.email.trim())) next.email = t("errors.emailInvalid");
    if (!form.dobAd) next.dobAd = t("errors.dobRequired");
    else if (new Date(form.dobAd) >= new Date()) next.dobAd = t("errors.dobFuture");
    if (!form.fatherName.trim()) next.fatherName = t("errors.fatherNameRequired");
    if (!form.grandfatherName.trim()) next.grandfatherName = t("errors.grandfatherNameRequired");
    if (!form.password) next.password = t("errors.passwordRequired");
    else if (form.password.length < 8) next.password = t("errors.passwordTooShort");
    if (!form.confirmPassword) next.confirmPassword = t("errors.confirmPasswordRequired");
    else if (form.password !== form.confirmPassword) next.confirmPassword = t("errors.passwordMismatch");
    if (!form.agreedToTerms) next.agreedToTerms = t("errors.agreeToTermsRequired");
    return next;
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setServerError(null);

    const validationErrors = validate();
    setErrors(validationErrors);
    if (Object.keys(validationErrors).length > 0) {
      return;
    }

    const request: SignupRequest = {
      email: form.email.trim(),
      fullName: form.fullName.trim(),
      dobAd: form.dobAd,
      fatherName: form.fatherName.trim(),
      grandfatherName: form.grandfatherName.trim(),
      password: form.password,
      confirmPassword: form.confirmPassword,
      preferredLanguage: locale,
      agreedToTerms: form.agreedToTerms,
      motherName: form.motherName.trim() || undefined,
      placeOfBirth: form.placeOfBirth.trim() || undefined,
      ancestralVillage: form.ancestralVillage.trim() || undefined,
      familyBranch: form.familyBranch.trim() || undefined,
      knownRelativeName: form.knownRelativeName.trim() || undefined,
      invitationCode: form.invitationCode.trim() || undefined,
      applicantNote: form.applicantNote.trim() || undefined,
    };

    setSubmitting(true);
    try {
      await submitSignup(request);
      setSubmitted(true);
    } catch (error) {
      setServerError(error instanceof SignupError ? error.message : t("errors.generic"));
    } finally {
      setSubmitting(false);
    }
  }

  if (submitted) {
    return (
      <div className={styles.success}>
        <h2>{t("success.title")}</h2>
        <p>{t("success.body")}</p>
        <Link href="/" className={styles.backHomeLink}>
          {t("success.backHome")}
        </Link>
      </div>
    );
  }

  return (
    <form className={styles.form} onSubmit={handleSubmit} noValidate>
      <p className={styles.subtitle}>{t("subtitle")}</p>

      {serverError ? <div className={styles.serverError}>{serverError}</div> : null}

      <fieldset className={styles.fieldset}>
        <legend>{t("sections.personal")}</legend>

        <Field
          label={t("fields.fullName")}
          error={errors.fullName}
          input={
            <input
              type="text"
              value={form.fullName}
              onChange={(event) => update("fullName", event.target.value)}
              required
            />
          }
        />

        <Field
          label={t("fields.email")}
          error={errors.email}
          input={
            <input
              type="email"
              value={form.email}
              onChange={(event) => update("email", event.target.value)}
              required
            />
          }
        />

        <Field
          label={t("fields.dobAd")}
          error={errors.dobAd}
          hint={form.dobAd && bsPreview ? t("fields.dobBsPreview", { date: bsPreview }) : undefined}
          input={
            <input
              type="date"
              value={form.dobAd}
              onChange={(event) => update("dobAd", event.target.value)}
              required
            />
          }
        />

        <Field
          label={`${t("fields.placeOfBirth")} ${t("optional")}`}
          input={
            <input
              type="text"
              value={form.placeOfBirth}
              onChange={(event) => update("placeOfBirth", event.target.value)}
            />
          }
        />
      </fieldset>

      <fieldset className={styles.fieldset}>
        <legend>{t("sections.family")}</legend>

        <Field
          label={t("fields.fatherName")}
          error={errors.fatherName}
          input={
            <input
              type="text"
              value={form.fatherName}
              onChange={(event) => update("fatherName", event.target.value)}
              required
            />
          }
        />

        <Field
          label={t("fields.grandfatherName")}
          error={errors.grandfatherName}
          input={
            <input
              type="text"
              value={form.grandfatherName}
              onChange={(event) => update("grandfatherName", event.target.value)}
              required
            />
          }
        />

        <Field
          label={`${t("fields.motherName")} ${t("optional")}`}
          input={
            <input
              type="text"
              value={form.motherName}
              onChange={(event) => update("motherName", event.target.value)}
            />
          }
        />

        <Field
          label={`${t("fields.ancestralVillage")} ${t("optional")}`}
          input={
            <input
              type="text"
              value={form.ancestralVillage}
              onChange={(event) => update("ancestralVillage", event.target.value)}
            />
          }
        />

        <Field
          label={`${t("fields.familyBranch")} ${t("optional")}`}
          input={
            <input
              type="text"
              value={form.familyBranch}
              onChange={(event) => update("familyBranch", event.target.value)}
            />
          }
        />

        <Field
          label={`${t("fields.knownRelativeName")} ${t("optional")}`}
          input={
            <input
              type="text"
              value={form.knownRelativeName}
              onChange={(event) => update("knownRelativeName", event.target.value)}
            />
          }
        />

        <Field
          label={t("fields.invitationCode")}
          hint={t("fields.invitationCodeHint")}
          input={
            <input
              type="text"
              value={form.invitationCode}
              onChange={(event) => update("invitationCode", event.target.value)}
            />
          }
        />
      </fieldset>

      <fieldset className={styles.fieldset}>
        <legend>{t("sections.account")}</legend>

        <Field
          label={t("fields.password")}
          error={errors.password}
          input={
            <input
              type="password"
              value={form.password}
              onChange={(event) => update("password", event.target.value)}
              required
            />
          }
        />

        <Field
          label={t("fields.confirmPassword")}
          error={errors.confirmPassword}
          input={
            <input
              type="password"
              value={form.confirmPassword}
              onChange={(event) => update("confirmPassword", event.target.value)}
              required
            />
          }
        />
      </fieldset>

      <fieldset className={styles.fieldset}>
        <legend>{t("sections.additional")}</legend>

        <Field
          label={`${t("fields.applicantNote")} ${t("optional")}`}
          input={
            <textarea
              value={form.applicantNote}
              onChange={(event) => update("applicantNote", event.target.value)}
              maxLength={2000}
              rows={4}
            />
          }
        />
      </fieldset>

      <label className={styles.checkboxRow}>
        <input
          type="checkbox"
          checked={form.agreedToTerms}
          onChange={(event) => update("agreedToTerms", event.target.checked)}
        />
        <span>
          {t.rich("agreeToTerms", {
            terms: (chunks) => <Link href="/terms">{chunks}</Link>,
            privacy: (chunks) => <Link href="/privacy">{chunks}</Link>,
          })}
        </span>
      </label>
      {errors.agreedToTerms ? <div className={styles.fieldError}>{errors.agreedToTerms}</div> : null}

      <Button type="submit" variant="primary" disabled={submitting} className={styles.submitButton}>
        {submitting ? t("submitting") : t("submit")}
      </Button>

      <p className={styles.bottomLink}>
        {t("alreadyHaveAccount")}{" "}
        {/* eslint-disable-next-line @next/next/no-html-link-for-pages -- /login is the backend's own page, not a route in this app, see page.tsx */}
        <a href="/login">{t("login")}</a>
      </p>
    </form>
  );
}

function Field({
  label,
  error,
  hint,
  input,
}: {
  label: string;
  error?: string;
  hint?: string;
  input: React.ReactNode;
}) {
  return (
    <div className={styles.field}>
      <label className={styles.label}>
        {label}
        {input}
      </label>
      {hint && !error ? <div className={styles.hint}>{hint}</div> : null}
      {error ? <div className={styles.fieldError}>{error}</div> : null}
    </div>
  );
}
