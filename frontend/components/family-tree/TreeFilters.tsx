"use client";

import { useState } from "react";
import { Search } from "lucide-react";
import { useTranslations } from "next-intl";
import type { LivingFilter } from "./familyTree.types";
import styles from "./TreeFilters.module.css";

interface TreeFiltersProps {
  search: string;
  onSearchChange: (value: string) => void;
  generationOptions: number[];
  living: LivingFilter;
  onLivingChange: (value: LivingFilter) => void;
  onReset: () => void;
  visibleCount: number;
  totalCount: number;
  minGeneration: number;
  maxGeneration: number;
  onRangeChange: (minGeneration: number, maxGeneration: number) => void;
  canLoadEarlier: boolean;
  canLoadLater: boolean;
  onLoadEarlier: () => void;
  onLoadLater: () => void;
  isAllGenerations: boolean;
  onShowAllGenerations: () => void;
  allGenerationsNeedsConfirm: boolean;
  searchScopeLimited: boolean;
}

export function TreeFilters({
  search,
  onSearchChange,
  generationOptions,
  living,
  onLivingChange,
  onReset,
  visibleCount,
  totalCount,
  minGeneration,
  maxGeneration,
  onRangeChange,
  canLoadEarlier,
  canLoadLater,
  onLoadEarlier,
  onLoadLater,
  isAllGenerations,
  onShowAllGenerations,
  allGenerationsNeedsConfirm,
  searchScopeLimited,
}: TreeFiltersProps) {
  const t = useTranslations("treePage.filters");
  const [confirmingAll, setConfirmingAll] = useState(false);

  // Reset the pending confirm whenever the window changes for any other
  // reason (range edited, expanded, or "all" already showing) -- adjusted
  // during render (React's recommended pattern for this), not in an effect,
  // to avoid an extra cascading render.
  const windowKey = `${minGeneration}-${maxGeneration}-${isAllGenerations}`;
  const [lastWindowKey, setLastWindowKey] = useState(windowKey);
  if (windowKey !== lastWindowKey) {
    setLastWindowKey(windowKey);
    setConfirmingAll(false);
  }

  function handleAllGenerationsClick() {
    if (isAllGenerations) {
      return;
    }
    if (allGenerationsNeedsConfirm && !confirmingAll) {
      setConfirmingAll(true);
      return;
    }
    setConfirmingAll(false);
    onShowAllGenerations();
  }

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

      <div className={styles.rangeGroup}>
        <span>{t("generation")}</span>
        <select
          className={styles.select}
          aria-label={t("generationFrom")}
          value={minGeneration}
          disabled={isAllGenerations}
          onChange={(event) => onRangeChange(Number(event.target.value), maxGeneration)}
        >
          {generationOptions.map((gen) => (
            <option key={gen} value={gen}>
              {gen}
            </option>
          ))}
        </select>
        <span aria-hidden="true">–</span>
        <select
          className={styles.select}
          aria-label={t("generationTo")}
          value={maxGeneration}
          disabled={isAllGenerations}
          onChange={(event) => onRangeChange(minGeneration, Number(event.target.value))}
        >
          {generationOptions.map((gen) => (
            <option key={gen} value={gen}>
              {gen}
            </option>
          ))}
        </select>
      </div>

      <button type="button" className={styles.resetButton} onClick={onLoadEarlier} disabled={!canLoadEarlier || isAllGenerations}>
        {t("loadEarlier")}
      </button>
      <button type="button" className={styles.resetButton} onClick={onLoadLater} disabled={!canLoadLater || isAllGenerations}>
        {t("loadLater")}
      </button>
      <button type="button" className={styles.resetButton} onClick={handleAllGenerationsClick} disabled={isAllGenerations}>
        {confirmingAll ? t("confirmAllGenerations", { count: totalCount }) : t("allGenerations")}
      </button>

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

      {searchScopeLimited ? <div className={styles.hintRow}>{t("searchScopeHint")}</div> : null}
    </div>
  );
}
