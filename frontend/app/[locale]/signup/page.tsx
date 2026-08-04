import { useTranslations } from "next-intl";
import { PageShell } from "@/components/PageShell";
import { SignupForm } from "@/components/SignupForm";
import styles from "./page.module.css";

export default function SignupPage() {
  const t = useTranslations("signupPage");

  return (
    <PageShell title={t("title")} titleClassName={styles.compactTitle}>
      <SignupForm />
    </PageShell>
  );
}
