"use client";

import { useState, type FormEvent } from "react";
import { useTranslations } from "next-intl";
import { LogOut, ShieldPlus } from "lucide-react";
import { useRouter } from "@/i18n/navigation";
import { requestAdminAccess, confirmAdminAccessRequest, signOut, AdminAccessRequestError } from "@/lib/api";
import styles from "./SidebarFooterActions.module.css";

interface SidebarFooterActionsProps {
  /** null hides the section entirely (already admin, or a legacy admin login with nothing to request). */
  adminAccessRequestStatus: "NONE" | "AWAITING_OTP" | "PENDING" | null;
}

export function SidebarFooterActions({ adminAccessRequestStatus }: SidebarFooterActionsProps) {
  const t = useTranslations("appShell.footer");
  const router = useRouter();
  const [status, setStatus] = useState(adminAccessRequestStatus);
  const [requesting, setRequesting] = useState(false);
  const [code, setCode] = useState("");
  const [confirming, setConfirming] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [signingOut, setSigningOut] = useState(false);

  async function handleRequestAdminAccess() {
    setRequesting(true);
    setError(null);
    try {
      await requestAdminAccess();
      setStatus("AWAITING_OTP");
    } catch (requestError) {
      setError(requestError instanceof AdminAccessRequestError ? requestError.message : t("requestError"));
    } finally {
      setRequesting(false);
    }
  }

  async function handleConfirm(event: FormEvent) {
    event.preventDefault();
    if (!code.trim()) {
      setError(t("codeRequired"));
      return;
    }
    setConfirming(true);
    setError(null);
    try {
      await confirmAdminAccessRequest(code.trim());
      setStatus("PENDING");
    } catch (confirmError) {
      setError(confirmError instanceof AdminAccessRequestError ? confirmError.message : t("confirmError"));
    } finally {
      setConfirming(false);
    }
  }

  async function handleSignOut() {
    setSigningOut(true);
    try {
      await signOut();
    } catch {
      // Fall through to redirect regardless -- worst case the user lands
      // on /login and the session was already invalid anyway.
    }
    router.push("/login?logout");
    router.refresh();
  }

  return (
    <div className={styles.wrapper}>
      {status === "AWAITING_OTP" ? (
        <form className={styles.otpForm} onSubmit={handleConfirm}>
          <span className={styles.otpHint}>{t("otpHint")}</span>
          <input
            type="text"
            inputMode="numeric"
            autoComplete="one-time-code"
            maxLength={6}
            value={code}
            onChange={(event) => setCode(event.target.value.replace(/\D/g, ""))}
            className={styles.otpInput}
            aria-label={t("otpCodeLabel")}
          />
          <button type="submit" className={styles.actionButton} disabled={confirming}>
            <ShieldPlus size={16} aria-hidden="true" />
            {confirming ? t("confirming") : t("confirmCode")}
          </button>
          <button
            type="button"
            className={styles.resendLink}
            onClick={handleRequestAdminAccess}
            disabled={requesting}
          >
            {t("resendCode")}
          </button>
        </form>
      ) : status ? (
        <button
          type="button"
          className={styles.actionButton}
          onClick={handleRequestAdminAccess}
          disabled={requesting || status === "PENDING"}
        >
          <ShieldPlus size={16} aria-hidden="true" />
          {status === "PENDING" ? t("requestPending") : t("requestAdminAccess")}
        </button>
      ) : null}
      {error ? <span className={styles.error}>{error}</span> : null}
      <button type="button" className={styles.actionButton} onClick={handleSignOut} disabled={signingOut}>
        <LogOut size={16} aria-hidden="true" />
        {t("logOut")}
      </button>
    </div>
  );
}
