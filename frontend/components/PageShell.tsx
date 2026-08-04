import type { ReactNode } from "react";
import { useTranslations } from "next-intl";
import { Link } from "@/i18n/navigation";
import { LanguageSwitcher } from "./LanguageSwitcher";
import styles from "./PageShell.module.css";

interface PageShellProps {
  title: string;
  titleClassName?: string;
  children: ReactNode;
}

export function PageShell({ title, titleClassName, children }: PageShellProps) {
  const t = useTranslations("nav");
  const titleClasses = titleClassName ? `${styles.title} ${titleClassName}` : styles.title;

  return (
    <main className={styles.page}>
      <header className={styles.header}>
        <LanguageSwitcher />
        <Link href="/" className={styles.backLink}>
          {t("backHome")}
        </Link>
      </header>
      <h1 className={titleClasses}>{title}</h1>
      <div className={styles.body}>{children}</div>
    </main>
  );
}
