"use client";

import { useState, type FormEvent } from "react";
import { Eye, EyeOff } from "lucide-react";
import { useTranslations } from "next-intl";
import { Link } from "@/i18n/navigation";
import { Button } from "@/components/Button";
import { confirmPasswordReset, PasswordResetError } from "@/lib/api";
import styles from "./ResetPasswordForm.module.css";

export function ResetPasswordForm({ token }: { token: string }) {
  const t = useTranslations("resetPassword");

  const [newPassword, setNewPassword] = useState("");
  const [confirmNewPassword, setConfirmNewPassword] = useState("");
  const [passwordVisible, setPasswordVisible] = useState(false);
  const [fieldError, setFieldError] = useState<string | null>(null);
  const [serverError, setServerError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setServerError(null);
    setFieldError(null);

    if (newPassword.length < 8) {
      setFieldError(t("passwordTooShort"));
      return;
    }
    if (newPassword !== confirmNewPassword) {
      setFieldError(t("mismatch"));
      return;
    }

    setSubmitting(true);
    try {
      await confirmPasswordReset(token, newPassword, confirmNewPassword);
      setSubmitted(true);
    } catch (error) {
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
          {t("loginLink")}
        </Link>
      </div>
    );
  }

  return (
    <form className={styles.form} onSubmit={handleSubmit} noValidate>
      {serverError ? <div className={styles.serverError}>{serverError}</div> : null}

      <div className={styles.field}>
        <label htmlFor="newPassword">{t("newPasswordLabel")}</label>
        <div className={styles.passwordWrapper}>
          <input
            id="newPassword"
            type={passwordVisible ? "text" : "password"}
            value={newPassword}
            onChange={(event) => setNewPassword(event.target.value)}
            autoComplete="new-password"
            required
          />
          <button
            type="button"
            className={styles.passwordToggle}
            onClick={() => setPasswordVisible((current) => !current)}
            aria-label={passwordVisible ? t("hidePassword") : t("showPassword")}
            aria-pressed={passwordVisible}
          >
            {passwordVisible ? <EyeOff size={18} aria-hidden="true" /> : <Eye size={18} aria-hidden="true" />}
          </button>
        </div>
      </div>

      <div className={styles.field}>
        <label htmlFor="confirmNewPassword">{t("confirmPasswordLabel")}</label>
        <input
          id="confirmNewPassword"
          type={passwordVisible ? "text" : "password"}
          value={confirmNewPassword}
          onChange={(event) => setConfirmNewPassword(event.target.value)}
          autoComplete="new-password"
          required
        />
      </div>

      {fieldError ? <div className={styles.fieldError}>{fieldError}</div> : null}

      <Button type="submit" variant="primary" className={styles.submit} disabled={submitting}>
        {submitting ? t("submitting") : t("submit")}
      </Button>
    </form>
  );
}
