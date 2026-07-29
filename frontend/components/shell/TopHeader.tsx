"use client";

import { Menu } from "lucide-react";
import { useTranslations } from "next-intl";
import { HeaderSearch } from "./HeaderSearch";
import { LanguageSwitcher } from "@/components/LanguageSwitcher";
import { UserMenu } from "./UserMenu";
import styles from "./TopHeader.module.css";

interface TopHeaderProps {
  displayName: string;
  roleLabel: string;
  email: string | null;
  onOpenMobileNav: () => void;
}

export function TopHeader({ displayName, roleLabel, email, onOpenMobileNav }: TopHeaderProps) {
  const t = useTranslations("appShell.header");

  return (
    <header className={styles.header}>
      <button type="button" className={styles.menuButton} onClick={onOpenMobileNav} aria-label={t("openMenu")}>
        <Menu size={20} aria-hidden="true" />
      </button>

      <div className={styles.spacer} />

      <HeaderSearch />

      {/* Below 640px there isn't room for search + language + identity in
          one row (see AppShell.module.css .drawer) -- language switching
          and sign-out move into the mobile drawer instead, reachable via
          the hamburger button above, rather than letting this row
          overflow horizontally. */}
      <div className={styles.desktopOnly}>
        <LanguageSwitcher />
      </div>
      <div className={`${styles.userMenuDesktop} ${styles.desktopOnly}`}>
        <UserMenu displayName={displayName} roleLabel={roleLabel} email={email} placement="below" />
      </div>
    </header>
  );
}
