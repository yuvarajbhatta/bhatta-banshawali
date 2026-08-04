"use client";

import { useEffect, useState, type FormEvent } from "react";
import { useTranslations } from "next-intl";
import { Link } from "@/i18n/navigation";
import { Button } from "@/components/Button";
import { confirmEmailVerification, resendEmailVerification, EmailVerificationError } from "@/lib/api";
import styles from "./VerifyEmailOtpForm.module.css";

const RESEND_COOLDOWN_SECONDS = 60;

export function VerifyEmailOtpForm({ email }: { email: string }) {
  const t = useTranslations("verifyEmail");

  const [code, setCode] = useState("");
  const [status, setStatus] = useState<"idle" | "verifying" | "success" | "error">("idle");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [resendState, setResendState] = useState<"idle" | "sending" | "sent">("idle");
  const [cooldown, setCooldown] = useState(0);

  useEffect(() => {
    if (cooldown <= 0) return;
    const timer = setInterval(() => setCooldown((seconds) => Math.max(0, seconds - 1)), 1000);
    return () => clearInterval(timer);
  }, [cooldown]);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (!code.trim()) {
      setErrorMessage(t("errors.codeRequired"));
      setStatus("error");
      return;
    }

    setStatus("verifying");
    setErrorMessage(null);
    try {
      await confirmEmailVerification(email, code.trim());
      setStatus("success");
    } catch (error) {
      setErrorMessage(error instanceof EmailVerificationError ? error.message : t("errors.generic"));
      setStatus("error");
    }
  }

  async function handleResend() {
    setResendState("sending");
    try {
      await resendEmailVerification(email);
      setResendState("sent");
      setCooldown(RESEND_COOLDOWN_SECONDS);
    } catch (error) {
      setErrorMessage(error instanceof EmailVerificationError ? error.message : t("errors.generic"));
      setStatus("error");
      setResendState("idle");
    }
  }

  if (status === "success") {
    return (
      <div className={styles.result}>
        <p>{t("successMessage")}</p>
        <Link href="/login" className={styles.link}>
          {t("loginLink")}
        </Link>
      </div>
    );
  }

  return (
    <form className={styles.form} onSubmit={handleSubmit}>
      <p className={styles.subtitle}>{t("subtitle", { email })}</p>
      {errorMessage ? <div className={styles.serverError}>{errorMessage}</div> : null}

      <label className={styles.codeLabel}>
        {t("codeLabel")}
        <input
          type="text"
          inputMode="numeric"
          autoComplete="one-time-code"
          maxLength={6}
          value={code}
          onChange={(event) => setCode(event.target.value.replace(/\D/g, ""))}
          className={styles.codeInput}
        />
      </label>

      <Button type="submit" variant="primary" disabled={status === "verifying"}>
        {status === "verifying" ? t("verifying") : t("confirmButton")}
      </Button>

      <div className={styles.resendRow}>
        {resendState === "sent" && cooldown > 0 ? (
          <span className={styles.resendHint}>{t("resendCooldown", { seconds: cooldown })}</span>
        ) : (
          <button
            type="button"
            className={styles.resendButton}
            onClick={handleResend}
            disabled={resendState === "sending" || cooldown > 0}
          >
            {resendState === "sending" ? t("resending") : t("resendButton")}
          </button>
        )}
        {resendState === "sent" ? <span className={styles.resendHint}>{t("resendSuccess")}</span> : null}
      </div>
    </form>
  );
}
