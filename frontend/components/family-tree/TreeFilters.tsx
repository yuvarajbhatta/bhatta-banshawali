"use client";

import { Search } from "lucide-react";
import { useTranslations } from "next-intl";
import type { LivingFilter } from "./familyTree.types";
import styles from "./TreeFilters.module.css";

interface TreeFiltersProps {
  search: string;
  onSearchChange: (value: string) => void;
  generation: number | "all";
  onGenerationChange: (value: number | "all") => void;
  generationOptions: number[];
  living: LivingFilter;
  onLivingChange: (value: LivingFilter) => void;
  onReset: () => void;
  visibleCount: number;
  totalCount: number;
}

export function TreeFilters({
  search,
  onSearchChange,
  generation,
  onGenerationChange,
  generationOptions,
  living,
  onLivingChange,
  onReset,
  visibleCount,
  totalCount,
}: TreeFiltersProps) {
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
      </div>

      <select
        className={styles.select}
        aria-label={t("generation")}
        value={generation === "all" ? "all" : String(generation)}
        onChange={(event) => onGenerationChange(event.target.value === "all" ? "all" : Number(event.target.value))}
      >
        <option value="all">{t("allGenerations")}</option>
        {generationOptions.map((gen) => (
          <option key={gen} value={gen}>
            {gen}
          </option>
        ))}
      </select>

      <select
        className={styles.select}
        aria-label={t("living")}
        value={living}
        onChange={(event) => onLivingChange(event.target.value as LivingFilter)}
      >
        <option value="all">{t("allPeople")}</option>
        <option value="living">{t("living")}</option>
        <option value="deceased">{t("deceased")}</option>
      </select>

      <button type="button" className={styles.resetButton} onClick={onReset}>
        {t("reset")}
      </button>

      <span className={styles.resultCount}>{t("resultCount", { count: visibleCount, total: totalCount })}</span>
    </div>
  );
}
