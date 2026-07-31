"use client";

import { useState, type FormEvent } from "react";
import { useTranslations } from "next-intl";
import { Link } from "@/i18n/navigation";
import { Button } from "@/components/Button";
import { requestPasswordReset, PasswordResetError } from "@/lib/api";
import styles from "./ForgotPasswordForm.module.css";

export function ForgotPasswordForm() {
  const t = useTranslations("forgotPassword");

  const [email, setEmail] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);
  const [submitted, setSubmitted] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setServerError(null);
    setSubmitting(true);
    try {
      await requestPasswordReset(email.trim());
      setSubmitted(true);
    } catch (error) {
      // Matches the backend's deliberate, clear "no account found"
      // message (not a generic anti-enumeration-safe response) -- see
      // PasswordResetService/AccountNotFoundException.
      setServerError(error instanceof PasswordResetError ? error.message : t("errors.generic"));
    } finally {
      setSubmitting(false);
    }
  }

  if (submitted) {
    return (
      <div className={styles.success}>
        <h2>{t("successTitle")}</h2>
        <p>{t("successBody")}</p>
        <Link href="/login" className={styles.backLink}>
          {t("backToLogin")}
        </Link>
      </div>
    );
  }

  return (
    <form className={styles.form} onSubmit={handleSubmit} noValidate>
      <p className={styles.subtitle}>{t("subtitle")}</p>

      {serverError ? <div className={styles.serverError}>{serverError}</div> : null}

      <div className={styles.field}>
        <label htmlFor="email">{t("emailLabel")}</label>
        <input
          id="email"
          type="email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          autoComplete="email"
          required
        />
      </div>

      <Button type="submit" variant="primary" className={styles.submit} disabled={submitting}>
        {submitting ? t("submitting") : t("submit")}
      </Button>

      <p className={styles.bottom}>
        <Link href="/login">{t("backToLogin")}</Link>
      </p>
    </form>
  );
}
