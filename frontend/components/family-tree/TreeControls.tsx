"use client";

import { Maximize, Minus, Plus } from "lucide-react";
import { useTranslations } from "next-intl";
import { useReactFlow } from "@xyflow/react";
import styles from "./TreeControls.module.css";

export function TreeControls() {
  const t = useTranslations("treePage.controls");
  const { zoomIn, zoomOut, fitView } = useReactFlow();

  return (
    <div className={styles.controls} role="group" aria-label={t("fitView")}>
      <button type="button" className={styles.button} onClick={() => zoomIn({ duration: 200 })} aria-label={t("zoomIn")}>
        <Plus size={18} aria-hidden="true" />
      </button>
      <button type="button" className={styles.button} onClick={() => zoomOut({ duration: 200 })} aria-label={t("zoomOut")}>
        <Minus size={18} aria-hidden="true" />
      </button>
      <button
        type="button"
        className={styles.button}
        onClick={() => fitView({ padding: 0.2, duration: 300 })}
        aria-label={t("fitView")}
      >
        <Maximize size={16} aria-hidden="true" />
      </button>
    </div>
  );
}
