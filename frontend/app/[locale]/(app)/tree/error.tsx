"use client";

import { useEffect } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/Button";
import { PageHeader } from "@/components/shell/PageHeader";
import styles from "./loading.module.css";

export default function FamilyTreeError({ error, reset }: { error: Error & { digest?: string }; reset: () => void }) {
  const t = useTranslations("treePage");

  useEffect(() => {
    // No error-reporting service wired up yet -- console is the honest fallback.
    console.error(error);
  }, [error]);

  return (
    <>
      <PageHeader title={t("title")} />
      <div className={styles.wrapper}>
        <div className={styles.skeleton}>
          <span className={styles.label}>{t("error")}</span>
          <Button variant="secondary" onClick={reset}>
            {t("retry")}
          </Button>
        </div>
      </div>
    </>
  );
}
