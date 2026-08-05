"use client";

import { useTranslations } from "next-intl";
import { FOCUS_DEPTH_OPTIONS } from "./useFocusDepthWindow";
import styles from "./TreeFocusControls.module.css";

interface TreeFocusControlsProps {
  focused: boolean;
  ancestorDepth: number;
  descendantDepth: number;
  onAncestorDepthChange: (depth: number) => void;
  onDescendantDepthChange: (depth: number) => void;
  highlightPathAvailable: boolean;
  highlightPath: boolean;
  onHighlightPathChange: (value: boolean) => void;
  onExpandAll: () => void;
  onResetView: () => void;
}

// The toolbar that appears once someone's focused in the tree (search
// result or node click) -- how far up/down to show from them, whether
// to trace the path back to the logged-in member, and a way to zoom
// back out. TreeFilters (search/generation-window/living) stays a
// separate bar since it governs the opposite mode: browsing without
// anyone focused.
export function TreeFocusControls({
  focused,
  ancestorDepth,
  descendantDepth,
  onAncestorDepthChange,
  onDescendantDepthChange,
  highlightPathAvailable,
  highlightPath,
  onHighlightPathChange,
  onExpandAll,
  onResetView,
}: TreeFocusControlsProps) {
  const t = useTranslations("treePage.focus");
  const controlsT = useTranslations("treePage.controls");

  return (
    <div className={styles.bar}>
      <label className={styles.depthField}>
        <span>{t("ancestors")}</span>
        <select
          className={styles.select}
          value={ancestorDepth}
          disabled={!focused}
          onChange={(event) => onAncestorDepthChange(Number(event.target.value))}
        >
          {FOCUS_DEPTH_OPTIONS.map((depth) => (
            <option key={depth} value={depth}>
              {depth}
            </option>
          ))}
        </select>
      </label>

      <label className={styles.depthField}>
        <span>{t("descendants")}</span>
        <select
          className={styles.select}
          value={descendantDepth}
          disabled={!focused}
          onChange={(event) => onDescendantDepthChange(Number(event.target.value))}
        >
          {FOCUS_DEPTH_OPTIONS.map((depth) => (
            <option key={depth} value={depth}>
              {depth}
            </option>
          ))}
        </select>
      </label>

      {highlightPathAvailable ? (
        <label className={focused ? styles.toggle : `${styles.toggle} ${styles.toggleDisabled}`}>
          <input
            type="checkbox"
            checked={highlightPath}
            disabled={!focused}
            onChange={(event) => onHighlightPathChange(event.target.checked)}
          />
          {t("highlightPath")}
          {!focused ? <span className={styles.toggleHint}>{t("highlightPathHint")}</span> : null}
        </label>
      ) : null}

      <button type="button" className={styles.button} onClick={onExpandAll} disabled={!focused}>
        {t("expandAll")}
      </button>

      <button type="button" className={styles.button} onClick={onResetView}>
        {controlsT("resetView")}
      </button>
    </div>
  );
}
