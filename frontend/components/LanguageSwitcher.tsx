"use client";

import { useLocale } from "next-intl";
import { useSearchParams } from "next/navigation";
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
  // usePathname() (next-intl) never includes the query string -- switching
  // locale with just that as the href used to drop it, breaking deep links
  // like /admin/signups?status=pending or /tree?focus=<id>.
  const searchParams = useSearchParams();
  const query = Object.fromEntries(searchParams.entries());

  return (
    <nav className={styles.switcher} aria-label="Language">
      {routing.locales.map((locale) => (
        <Link
          key={locale}
          href={{ pathname, query }}
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
