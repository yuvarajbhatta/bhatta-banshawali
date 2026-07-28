"use client";

import { useLocale } from "next-intl";
import { Link, usePathname } from "@/i18n/navigation";
import { routing } from "@/i18n/routing";
import styles from "./LanguageSwitcher.module.css";

const LOCALE_LABELS: Record<string, string> = {
  en: "English",
  ne: "नेपाली",
};

export function LanguageSwitcher() {
  const activeLocale = useLocale();
  const pathname = usePathname();

  return (
    <nav className={styles.switcher} aria-label="Language">
      {routing.locales.map((locale) => (
        <Link
          key={locale}
          href={pathname}
          locale={locale}
          className={locale === activeLocale ? `${styles.link} ${styles.active}` : styles.link}
          aria-current={locale === activeLocale ? "true" : undefined}
        >
          {LOCALE_LABELS[locale] ?? locale}
        </Link>
      ))}
    </nav>
  );
}
