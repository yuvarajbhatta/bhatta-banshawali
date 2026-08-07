"use client";

import { useState, useSyncExternalStore } from "react";
import { X } from "lucide-react";
import { useTranslations } from "next-intl";
import styles from "./TouchHint.module.css";

const DISMISSED_KEY = "familytree.tree.touchHintDismissed";

// No live updates to subscribe to -- pointer type and prior-dismissal
// state only ever need reading once, not watched for changes after
// mount -- but useSyncExternalStore still requires a subscribe function.
function subscribe() {
  return () => {};
}

function shouldShowOnThisDevice() {
  const isCoarsePointer = window.matchMedia("(pointer: coarse)").matches;
  const alreadyDismissed = window.localStorage.getItem(DISMISSED_KEY) === "1";
  return isCoarsePointer && !alreadyDismissed;
}

function shouldShowDuringServerRender() {
  return false;
}

/**
 * One-finger swipe on the tree canvas pans the diagram, not the page (see
 * TreeCanvas.module.css's touch-action override for the rest of that fix)
 * -- coarse-pointer (touch) devices only get a one-time hint explaining
 * two-finger navigation, since mouse/trackpad users never hit this at all.
 * Dismissal is remembered in localStorage so it only ever shows once per
 * device, not once per visit.
 *
 * useSyncExternalStore, not useState+useEffect: matchMedia/localStorage are
 * browser-only, so the server-rendered HTML must show nothing (no window)
 * while the client's first real read may immediately say otherwise --
 * useSyncExternalStore's getServerSnapshot/getSnapshot split is the one
 * hook React lets reconcile that without a hydration-mismatch warning or a
 * setState-in-effect extra render pass.
 */
export function TouchHint() {
  const t = useTranslations("treePage");
  const showBasedOnDevice = useSyncExternalStore(subscribe, shouldShowOnThisDevice, shouldShowDuringServerRender);
  const [dismissedThisSession, setDismissedThisSession] = useState(false);
  const visible = showBasedOnDevice && !dismissedThisSession;

  function dismiss() {
    setDismissedThisSession(true);
    window.localStorage.setItem(DISMISSED_KEY, "1");
  }

  if (!visible) {
    return null;
  }

  return (
    <div className={styles.hint} role="status">
      <span>{t("touchHint")}</span>
      <button type="button" className={styles.dismiss} onClick={dismiss} aria-label={t("touchHintDismiss")}>
        <X size={16} aria-hidden="true" />
      </button>
    </div>
  );
}
