"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Link } from "@/i18n/navigation";
import { Button } from "@/components/Button";
import { confirmEmailVerification, EmailVerificationError } from "@/lib/api";
import styles from "./VerifyEmailAction.module.css";

// Deliberately does NOT confirm on mount -- some corporate email-security
// scanners pre-fetch every link in an email before a human ever clicks it.
// If loading this page directly triggered the (single-use) confirm call,
// a scanner would burn the token before the real user gets to it. Only an
// explicit button click fires the POST.
export function VerifyEmailAction({ token }: { token: string }) {
  const t = useTranslations("verifyEmail");

  const [status, setStatus] = useState<"idle" | "verifying" | "success" | "error">("idle");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  async function handleConfirm() {
    setStatus("verifying");
    setErrorMessage(null);
    try {
      await confirmEmailVerification(token);
      setStatus("success");
    } catch (error) {
      setErrorMessage(error instanceof EmailVerificationError ? error.message : t("errors.generic"));
      setStatus("error");
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
    <div className={styles.action}>
      <p className={styles.subtitle}>{t("subtitle")}</p>
      {status === "error" && errorMessage ? <div className={styles.serverError}>{errorMessage}</div> : null}
      <Button variant="primary" onClick={handleConfirm} disabled={status === "verifying"}>
        {status === "verifying" ? t("verifying") : t("confirmButton")}
      </Button>
    </div>
  );
}
