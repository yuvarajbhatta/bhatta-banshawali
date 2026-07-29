"use client";

import type { ReactNode } from "react";
import { useTranslations } from "next-intl";
import { GitFork } from "lucide-react";
import { Link, usePathname } from "@/i18n/navigation";
import { APP_NAV_ITEMS } from "./navItems";
import styles from "./Sidebar.module.css";

interface SidebarProps {
  onNavigate?: () => void;
  footer?: ReactNode;
}

export function Sidebar({ onNavigate, footer }: SidebarProps) {
  const t = useTranslations("appShell.nav");
  const brandT = useTranslations("app");
  const pathname = usePathname();

  return (
    <nav className={styles.sidebar} aria-label={brandT("name")}>
      <Link href="/dashboard" className={styles.brand} onClick={onNavigate}>
        <span className={styles.brandMark} aria-hidden="true">
          <GitFork size={18} />
        </span>
        <span className={styles.brandName}>Bhatta Banshawali</span>
      </Link>

      <div className={styles.nav}>
        {APP_NAV_ITEMS.map((item) => {
          const isActive = pathname === item.href || pathname.startsWith(`${item.href}/`);
          const Icon = item.icon;
          return (
            <Link
              key={item.href}
              href={item.href}
              onClick={onNavigate}
              aria-current={isActive ? "page" : undefined}
              className={isActive ? `${styles.navLink} ${styles.navLinkActive}` : styles.navLink}
            >
              <Icon size={18} className={styles.navIcon} aria-hidden="true" />
              {t(item.labelKey)}
            </Link>
          );
        })}
      </div>

      {footer ? <div className={styles.footer}>{footer}</div> : null}
    </nav>
  );
}
