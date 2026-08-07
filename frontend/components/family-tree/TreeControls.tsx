"use client";

import { Maximize, Minus, Plus, RotateCcw } from "lucide-react";
import { useTranslations } from "next-intl";
import { useReactFlow } from "@xyflow/react";
import styles from "./TreeControls.module.css";

interface TreeControlsProps {
  onReset: () => void;
  onOpenFullscreen: () => void;
}

export function TreeControls({ onReset, onOpenFullscreen }: TreeControlsProps) {
  const t = useTranslations("treePage");
  const { zoomIn, zoomOut } = useReactFlow();

  return (
    <div className={styles.controls} role="group" aria-label={t("controls.groupLabel")}>
      <button type="button" className={styles.button} onClick={() => zoomIn({ duration: 200 })} aria-label={t("controls.zoomIn")}>
        <Plus size={18} aria-hidden="true" />
      </button>
      <button type="button" className={styles.button} onClick={() => zoomOut({ duration: 200 })} aria-label={t("controls.zoomOut")}>
        <Minus size={18} aria-hidden="true" />
      </button>
      <button type="button" className={styles.button} onClick={onReset} aria-label={t("controls.resetView")}>
        <RotateCcw size={16} aria-hidden="true" />
      </button>
      <button type="button" className={styles.button} onClick={onOpenFullscreen} aria-label={t("viewFullTreeCta")}>
        <Maximize size={16} aria-hidden="true" />
      </button>
    </div>
  );
}
