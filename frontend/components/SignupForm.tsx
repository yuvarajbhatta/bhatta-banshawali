"use client";

import { useEffect, useState, type FormEvent } from "react";
import { useLocale, useTranslations } from "next-intl";
import { Link, usePathname, useRouter } from "@/i18n/navigation";
import { routing } from "@/i18n/routing";
import { Button } from "@/components/Button";
import { convertAdToBs, convertBsToAd, submitSignup, SignupError, type SignupRequest } from "@/lib/api";
import styles from "./SignupForm.module.css";

// Selecting a language on the signup page navigates to the same page
// under that locale (like LanguageSwitcher does everywhere else in the
// app), which is a full route change and would otherwise wipe whatever
// the applicant already typed. Stash/restore the in-progress form
// across that navigation so switching language mid-fill is safe.
const DRAFT_STORAGE_KEY = "signupFormDraft";

function loadDraft(): Partial<FormState> | null {
  if (typeof window === "undefined") return null;
  try {
    const raw = window.sessionStorage.getItem(DRAFT_STORAGE_KEY);
    return raw ? (JSON.parse(raw) as Partial<FormState>) : null;
  } catch {
    return null;
  }
}

interface FormState {
  fullName: string;
  email: string;
  dobAd: string;
  dobBsYear: string;
  dobBsMonth: string;
  dobBsDay: string;
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
  dobBsYear: "",
  dobBsMonth: "",
  dobBsDay: "",
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
  const tLanguage = useTranslations("language");
  const locale = useLocale();
  const pathname = usePathname();
  const router = useRouter();

  const [form, setForm] = useState<FormState>(() => ({ ...EMPTY_FORM, ...loadDraft() }));
  const [errors, setErrors] = useState<FormErrors>({});
  const [submitting, setSubmitting] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);
  const [dobBsError, setDobBsError] = useState<string | null>(null);

  useEffect(() => {
    if (typeof window === "undefined") return;
    window.sessionStorage.setItem(DRAFT_STORAGE_KEY, JSON.stringify(form));
  }, [form]);

  // AD is the value submitted to the backend. When it changes (either
  // because the applicant edited the AD field directly, or because the
  // BS->AD effect below just derived it from the BS fields), keep the
  // BS fields in sync -- but only overwrite them if they'd actually
  // change, so this doesn't fight with the BS->AD effect and cause a
  // render loop.
  useEffect(() => {
    if (!form.dobAd) {
      return;
    }
    let cancelled = false;
    convertAdToBs(form.dobAd)
      .then((bs) => {
        if (cancelled) return;
        const year = String(bs.year);
        const month = String(bs.month);
        const day = String(bs.day);
        setForm((prev) =>
          prev.dobBsYear === year && prev.dobBsMonth === month && prev.dobBsDay === day
            ? prev
            : { ...prev, dobBsYear: year, dobBsMonth: month, dobBsDay: day },
        );
      })
      .catch(() => {
        // AD date outside the calendar's supported BS range -- leave
        // whatever is in the BS fields alone.
      });
    return () => {
      cancelled = true;
    };
  }, [form.dobAd]);

  // BS fields are what most applicants will actually type into. Once
  // all three are filled with plausible values, derive AD from them --
  // AD stays the single value actually submitted to the backend.
  const dobBsFieldsComplete = Boolean(form.dobBsYear && form.dobBsMonth && form.dobBsDay);

  useEffect(() => {
    const year = Number(form.dobBsYear);
    const month = Number(form.dobBsMonth);
    const day = Number(form.dobBsDay);
    if (!form.dobBsYear || !form.dobBsMonth || !form.dobBsDay) {
      return;
    }
    if (!Number.isInteger(year) || !Number.isInteger(month) || !Number.isInteger(day)) {
      return;
    }
    let cancelled = false;
    convertBsToAd(year, month, day)
      .then((ad) => {
        if (cancelled) return;
        setDobBsError(null);
        setForm((prev) => (prev.dobAd === ad.date ? prev : { ...prev, dobAd: ad.date }));
      })
      .catch(() => {
        if (!cancelled) {
          setDobBsError(t("errors.dobBsInvalid"));
        }
      });
    return () => {
      cancelled = true;
    };
  }, [form.dobBsYear, form.dobBsMonth, form.dobBsDay, t]);

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
      if (typeof window !== "undefined") {
        window.sessionStorage.removeItem(DRAFT_STORAGE_KEY);
      }
      router.push(`/verify-email?email=${encodeURIComponent(request.email)}`);
    } catch (error) {
      setServerError(error instanceof SignupError ? error.message : t("errors.generic"));
      setSubmitting(false);
    }
  }

  return (
    <form className={styles.form} onSubmit={handleSubmit} noValidate>
      <p className={styles.subtitle}>{t("subtitle")}</p>

      <div className={styles.languageRow}>
        <span className={styles.languageLabel}>{tLanguage("label")}</span>
        <div className={styles.languageOptions} role="group" aria-label={tLanguage("label")}>
          {routing.locales.map((loc) => (
            <Link
              key={loc}
              href={pathname}
              locale={loc}
              className={loc === locale ? `${styles.languageOption} ${styles.languageOptionActive}` : styles.languageOption}
              aria-current={loc === locale ? "true" : undefined}
            >
              {loc === "ne" ? tLanguage("nepali") : tLanguage("english")}
            </Link>
          ))}
        </div>
      </div>

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

        <div className={styles.dobRow}>
          <div className={styles.dobGroup}>
            <span className={styles.dobGroupLabel}>{t("fields.dobBs")}</span>
            <div className={styles.dobBsInputs}>
              <input
                type="number"
                inputMode="numeric"
                aria-label={t("fields.dobBsYear")}
                placeholder={t("fields.dobBsYear")}
                value={form.dobBsYear}
                onChange={(event) => update("dobBsYear", event.target.value)}
                min={2000}
                max={2090}
              />
              <input
                type="number"
                inputMode="numeric"
                aria-label={t("fields.dobBsMonth")}
                placeholder={t("fields.dobBsMonth")}
                value={form.dobBsMonth}
                onChange={(event) => update("dobBsMonth", event.target.value)}
                min={1}
                max={12}
              />
              <input
                type="number"
                inputMode="numeric"
                aria-label={t("fields.dobBsDay")}
                placeholder={t("fields.dobBsDay")}
                value={form.dobBsDay}
                onChange={(event) => update("dobBsDay", event.target.value)}
                min={1}
                max={32}
              />
            </div>
            {dobBsFieldsComplete && dobBsError ? <div className={styles.fieldError}>{dobBsError}</div> : null}
          </div>

          <div className={styles.dobGroup}>
            <span className={styles.dobGroupLabel}>{t("fields.dobAd")}</span>
            <input
              type="date"
              value={form.dobAd}
              onChange={(event) => update("dobAd", event.target.value)}
              required
            />
            {errors.dobAd ? <div className={styles.fieldError}>{errors.dobAd}</div> : null}
          </div>
        </div>

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
        {t("alreadyHaveAccount")} <Link href="/login">{t("login")}</Link>
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
