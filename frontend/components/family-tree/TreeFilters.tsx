"use client";

import { Search, X } from "lucide-react";
import { useTranslations } from "next-intl";
import styles from "./TreeFilters.module.css";

interface TreeFiltersProps {
  search: string;
  onSearchChange: (value: string) => void;
}

export function TreeFilters({ search, onSearchChange }: TreeFiltersProps) {
  const t = useTranslations("treePage.filters");

  return (
    <div className={styles.bar}>
      <div className={styles.searchWrapper}>
        <Search size={16} className={styles.searchIcon} aria-hidden="true" />
        <input
          type="search"
          className={styles.searchInput}
          placeholder={t("searchPlaceholder")}
          aria-label={t("search")}
          value={search}
          onChange={(event) => onSearchChange(event.target.value)}
        />
        {search ? (
          <button type="button" className={styles.clearButton} onClick={() => onSearchChange("")} aria-label={t("clearSearch")}>
            <X size={16} aria-hidden="true" />
          </button>
        ) : null}
      </div>
    </div>
  );
}
