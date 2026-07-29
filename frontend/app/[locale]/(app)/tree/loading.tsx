import { getTranslations } from "next-intl/server";
import { PageHeader } from "@/components/shell/PageHeader";
import styles from "./loading.module.css";

// Route-level Suspense fallback while the server component fetches
// GET /api/v1/family-tree -- a tree-shaped skeleton rather than a blank
// screen or generic spinner (docs/frontend-redesign-plan.md / brief
// section 10).
export default async function FamilyTreeLoading() {
  const t = await getTranslations("treePage");

  return (
    <>
      <PageHeader title={t("title")} subtitle={t("subtitle")} />
      <div className={styles.wrapper} role="status" aria-live="polite">
        <div className={styles.skeleton}>
          <div className={styles.node} />
          <div className={styles.row}>
            <div className={styles.node} />
            <div className={styles.node} />
          </div>
          <span className={styles.label}>{t("loading")}</span>
        </div>
      </div>
    </>
  );
}
