import type { ReactNode } from "react";
import styles from "./PageHeader.module.css";

interface PageHeaderProps {
  title: string;
  subtitle?: string;
  titleClassName?: string;
  actions?: ReactNode;
}

export function PageHeader({ title, subtitle, titleClassName, actions }: PageHeaderProps) {
  const titleClasses = titleClassName ? `${styles.title} ${titleClassName}` : styles.title;
  return (
    <div className={styles.header}>
      <h1 className={titleClasses}>{title}</h1>
      {subtitle ? <p className={styles.subtitle}>{subtitle}</p> : null}
      {actions ? <div className={styles.actions}>{actions}</div> : null}
    </div>
  );
}
