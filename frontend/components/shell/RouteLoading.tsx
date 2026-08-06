import { getTranslations } from "next-intl/server";
import { PageHeader } from "@/components/shell/PageHeader";
import styles from "./RouteLoading.module.css";

interface RouteLoadingProps {
  title: string;
  subtitle?: string;
}

// Shared route-level Suspense fallback (docs/... production-readiness
// review: most routes had no loading.tsx, so a slow fetch just showed a
// blank page instead of this). Routes with a bespoke skeleton -- e.g.
// tree/loading.tsx's tree-shaped placeholder -- keep their own instead of
// using this.
export async function RouteLoading({ title, subtitle }: RouteLoadingProps) {
  const t = await getTranslations("routeState");

  return (
    <>
      <PageHeader title={title} subtitle={subtitle} />
      <div className={styles.wrapper} role="status" aria-live="polite">
        <div className={styles.content}>
          <div className={styles.spinner} />
          <span className={styles.label}>{t("loading")}</span>
        </div>
      </div>
    </>
  );
}
