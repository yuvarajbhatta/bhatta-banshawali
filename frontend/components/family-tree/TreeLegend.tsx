"use client";

import { useTranslations } from "next-intl";
import styles from "./TreeLegend.module.css";

export function TreeLegend() {
  const t = useTranslations("treePage.legend");

  return (
    <div className={styles.legend}>
      <span className={styles.title}>{t("title")}</span>
      <span className={styles.row}>
        <span className={`${styles.swatch} ${styles.swatchParentChild}`} aria-hidden="true" />
        {t("parentChild")}
      </span>
      <span className={styles.row}>
        <span className={`${styles.swatch} ${styles.swatchSpouse}`} aria-hidden="true" />
        {t("spouse")}
      </span>
    </div>
  );
}
