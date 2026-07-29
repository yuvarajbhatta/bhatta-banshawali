"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { LogOut, ShieldPlus } from "lucide-react";
import { useRouter } from "@/i18n/navigation";
import { requestAdminAccess, signOut, AdminAccessRequestError } from "@/lib/api";
import styles from "./SidebarFooterActions.module.css";

interface SidebarFooterActionsProps {
  /** null hides the button entirely (already admin, or a legacy admin login with nothing to request). */
  adminAccessRequestStatus: "NONE" | "PENDING" | null;
}

export function SidebarFooterActions({ adminAccessRequestStatus }: SidebarFooterActionsProps) {
  const t = useTranslations("appShell.footer");
  const router = useRouter();
  const [status, setStatus] = useState(adminAccessRequestStatus);
  const [requesting, setRequesting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [signingOut, setSigningOut] = useState(false);

  async function handleRequestAdminAccess() {
    setRequesting(true);
    setError(null);
    try {
      await requestAdminAccess();
      setStatus("PENDING");
    } catch (requestError) {
      setError(requestError instanceof AdminAccessRequestError ? requestError.message : t("requestError"));
    } finally {
      setRequesting(false);
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
      {status ? (
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
