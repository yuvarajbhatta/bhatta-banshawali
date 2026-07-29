"use client";

import { useEffect, useRef, useState } from "react";
import { useTranslations } from "next-intl";
import { ChevronUp } from "lucide-react";
import styles from "./UserMenu.module.css";

interface UserMenuProps {
  displayName: string;
  roleLabel: string;
  email: string | null;
  placement?: "above" | "below";
}

export function UserMenu({ displayName, roleLabel, email, placement = "above" }: UserMenuProps) {
  const t = useTranslations("appShell.header");
  const [open, setOpen] = useState(false);
  const wrapperRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) {
      return;
    }

    function handleClickOutside(event: MouseEvent) {
      if (wrapperRef.current && !wrapperRef.current.contains(event.target as Node)) {
        setOpen(false);
      }
    }

    function handleEscape(event: KeyboardEvent) {
      if (event.key === "Escape") {
        setOpen(false);
      }
    }

    document.addEventListener("mousedown", handleClickOutside);
    document.addEventListener("keydown", handleEscape);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
      document.removeEventListener("keydown", handleEscape);
    };
  }, [open]);

  const initials = displayName
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join("") || "?";

  return (
    <div className={styles.wrapper} ref={wrapperRef}>
      <button
        type="button"
        className={styles.trigger}
        onClick={() => setOpen((current) => !current)}
        aria-haspopup="menu"
        aria-expanded={open}
        aria-label={t("userMenu")}
      >
        <span className={styles.avatar} aria-hidden="true">
          {initials}
        </span>
        <span className={styles.identity}>
          <span className={styles.name}>{displayName}</span>
          <span className={styles.role}>{roleLabel}</span>
        </span>
        <ChevronUp size={16} className={open ? `${styles.chevron} ${styles.chevronOpen}` : styles.chevron} aria-hidden="true" />
      </button>

      {open ? (
        <div role="menu" className={placement === "below" ? `${styles.menu} ${styles.menuBelow}` : styles.menu}>
          <div className={styles.profileCard}>
            <span className={styles.profileName}>{displayName}</span>
            {email ? <span className={styles.profileEmail}>{email}</span> : null}
          </div>
        </div>
      ) : null}
    </div>
  );
}
