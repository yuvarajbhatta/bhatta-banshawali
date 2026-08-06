"use client";

import type { ReactNode } from "react";
import { useTranslations } from "next-intl";
import { Link, usePathname } from "@/i18n/navigation";
import { ADMIN_NAV_ITEMS, APP_NAV_ITEMS } from "./navItems";
import styles from "./Sidebar.module.css";

export interface AdminNavCounts {
  pendingSignupCount: number;
  pendingCorrectionCount: number;
  pendingAdminAccessRequestCount: number;
}

interface SidebarProps {
  onNavigate?: () => void;
  footer?: ReactNode;
  adminCounts?: AdminNavCounts | null;
  announcementUnreadCount?: number;
}

export function Sidebar({ onNavigate, footer, adminCounts, announcementUnreadCount }: SidebarProps) {
  const t = useTranslations("appShell.nav");
  const brandT = useTranslations("app");
  const pathname = usePathname();

  function isActive(href: string) {
    return pathname === href || pathname.startsWith(`${href}/`);
  }

  return (
    <nav className={styles.sidebar} aria-label={brandT("name")}>
      <Link href="/dashboard" className={styles.brand} onClick={onNavigate}>
        <span className={styles.brandMark} aria-hidden="true">
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img src="/api/v1/brand/icon" alt="" className={styles.brandMarkImage} />
        </span>
        <span className={styles.brandName}>Bhatta Banshawali</span>
      </Link>

      <div className={styles.nav}>
        <div className={styles.navGroup}>
          {APP_NAV_ITEMS.map((item) => {
            const Icon = item.icon;
            const badgeCount = item.showUnreadBadge ? announcementUnreadCount : undefined;
            return (
              <Link
                key={item.href}
                href={item.href}
                onClick={onNavigate}
                aria-current={isActive(item.href) ? "page" : undefined}
                className={isActive(item.href) ? `${styles.navLink} ${styles.navLinkActive}` : styles.navLink}
              >
                <Icon size={18} className={styles.navIcon} aria-hidden="true" />
                {t(item.labelKey)}
                {badgeCount != null && badgeCount > 0 ? <span className={styles.countBadge}>{badgeCount}</span> : null}
              </Link>
            );
          })}
        </div>

        {adminCounts ? (
          <div className={styles.navGroup}>
            <p className={styles.sectionLabel}>{t("administration")}</p>
            {ADMIN_NAV_ITEMS.map((item) => {
              const Icon = item.icon;
              const count = item.countKey ? adminCounts[item.countKey] : undefined;
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  onClick={onNavigate}
                  aria-current={isActive(item.href) ? "page" : undefined}
                  className={isActive(item.href) ? `${styles.navLink} ${styles.navLinkActive}` : styles.navLink}
                >
                  <Icon size={18} className={styles.navIcon} aria-hidden="true" />
                  {t(item.labelKey)}
                  {count != null && count > 0 ? <span className={styles.countBadge}>{count}</span> : null}
                </Link>
              );
            })}
          </div>
        ) : null}
      </div>

      {footer ? <div className={styles.footer}>{footer}</div> : null}
    </nav>
  );
}
