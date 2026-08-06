"use client";

import { useEffect } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/Button";
import { PageHeader } from "@/components/shell/PageHeader";
import styles from "./RouteLoading.module.css";

interface RouteErrorProps {
  title: string;
  error: Error & { digest?: string };
  reset: () => void;
}

// Shared route-level error boundary, the error.tsx counterpart to
// RouteLoading -- see that file's comment for why this exists.
export function RouteError({ title, error, reset }: RouteErrorProps) {
  const t = useTranslations("routeState");

  useEffect(() => {
    // No error-reporting service wired up yet -- console is the honest fallback.
    console.error(error);
  }, [error]);

  return (
    <>
      <PageHeader title={title} />
      <div className={styles.wrapper}>
        <div className={styles.content}>
          <span className={styles.label}>{t("error")}</span>
          <Button variant="secondary" onClick={reset}>
            {t("retry")}
          </Button>
        </div>
      </div>
    </>
  );
}
