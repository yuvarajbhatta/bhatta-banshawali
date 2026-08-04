"use client";

import { useEffect, useRef, useState, type ReactNode } from "react";
import { AnimatePresence, motion, useReducedMotion } from "framer-motion";
import { X } from "lucide-react";
import { useTranslations } from "next-intl";
import { usePathname } from "@/i18n/navigation";
import { LanguageSwitcher } from "@/components/LanguageSwitcher";
import { Sidebar, type AdminNavCounts } from "./Sidebar";
import { SidebarFooterActions } from "./SidebarFooterActions";
import { TopHeader } from "./TopHeader";
import { DURATION, EASE_OUT } from "@/lib/motion";
import { warmCsrfCookie } from "@/lib/api";
import styles from "./AppShell.module.css";

interface AppShellProps {
  displayName: string;
  roleLabel: string;
  email: string | null;
  adminAccessRequestStatus: "NONE" | "AWAITING_OTP" | "PENDING" | null;
  adminCounts?: AdminNavCounts | null;
  announcementUnreadCount?: number;
  children: ReactNode;
}

export function AppShell({
  displayName,
  roleLabel,
  email,
  adminAccessRequestStatus,
  adminCounts,
  announcementUnreadCount,
  children,
}: AppShellProps) {
  const t = useTranslations("appShell.header");
  const pathname = usePathname();
  const [mobileNavOpen, setMobileNavOpen] = useState(false);
  const shouldReduceMotion = useReducedMotion();
  const drawerRef = useRef<HTMLDivElement>(null);
  const menuButtonFocusRef = useRef<HTMLElement | null>(null);

  // Closing on route change covers both an explicit nav-link tap and any
  // other navigation (back button, etc.) while the drawer is open. Adjusting
  // state during render (rather than in an effect) per
  // https://react.dev/learn/you-might-not-need-an-effect#adjusting-some-state-when-a-prop-changes.
  const [lastPathname, setLastPathname] = useState(pathname);
  if (pathname !== lastPathname) {
    setLastPathname(pathname);
    setMobileNavOpen(false);
  }

  useEffect(() => {
    warmCsrfCookie();
  }, []);

  useEffect(() => {
    if (!mobileNavOpen) {
      return;
    }

    menuButtonFocusRef.current = document.activeElement as HTMLElement;
    const drawer = drawerRef.current;
    const focusable = drawer?.querySelectorAll<HTMLElement>(
      'a[href], button:not([disabled]), input, [tabindex]:not([tabindex="-1"])',
    );
    focusable?.[0]?.focus();

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        setMobileNavOpen(false);
        return;
      }
      if (event.key !== "Tab" || !focusable || focusable.length === 0) {
        return;
      }
      const first = focusable.item(0);
      const last = focusable.item(focusable.length - 1);
      if (!first || !last) {
        return;
      }
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    }

    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("keydown", handleKeyDown);
      menuButtonFocusRef.current?.focus();
    };
  }, [mobileNavOpen]);

  return (
    <div className={styles.shell}>
      <aside className={styles.desktopSidebar}>
        <Sidebar
          adminCounts={adminCounts}
          announcementUnreadCount={announcementUnreadCount}
          footer={<SidebarFooterActions adminAccessRequestStatus={adminAccessRequestStatus} />}
        />
      </aside>

      <div className={styles.main}>
        <TopHeader
          displayName={displayName}
          roleLabel={roleLabel}
          email={email}
          onOpenMobileNav={() => setMobileNavOpen(true)}
        />
        <div className={styles.content}>{children}</div>
      </div>

      <AnimatePresence>
        {mobileNavOpen ? (
          <>
            <motion.div
              className={styles.backdrop}
              onClick={() => setMobileNavOpen(false)}
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              transition={{ duration: DURATION.fast }}
            />
            <motion.div
              className={styles.drawer}
              ref={drawerRef}
              role="dialog"
              aria-modal="true"
              aria-label={t("openMenu")}
              initial={shouldReduceMotion ? { opacity: 0 } : { x: "-100%" }}
              animate={shouldReduceMotion ? { opacity: 1 } : { x: 0 }}
              exit={shouldReduceMotion ? { opacity: 0 } : { x: "-100%" }}
              transition={{ duration: DURATION.base, ease: EASE_OUT }}
            >
              <div className={styles.drawerHeader}>
                <LanguageSwitcher />
                <button
                  type="button"
                  className={styles.drawerCloseButton}
                  onClick={() => setMobileNavOpen(false)}
                  aria-label={t("closeMenu")}
                >
                  <X size={20} aria-hidden="true" />
                </button>
              </div>
              <div className={styles.drawerBody}>
                <Sidebar
                  onNavigate={() => setMobileNavOpen(false)}
                  adminCounts={adminCounts}
                  announcementUnreadCount={announcementUnreadCount}
                  footer={<SidebarFooterActions adminAccessRequestStatus={adminAccessRequestStatus} />}
                />
              </div>
            </motion.div>
          </>
        ) : null}
      </AnimatePresence>
    </div>
  );
}
