import { Link } from "@/i18n/navigation";
import styles from "./StatusFilterTabs.module.css";

interface StatusFilterTabsProps<T extends string> {
  statuses: readonly T[];
  selected: T;
  basePath: string;
  labelFor: (status: T) => string;
}

// A server component -- the status filter is plain navigation
// (?status=X, matching the Thymeleaf original's <a> links), not client
// state, so the list page can stay a server component too.
export function StatusFilterTabs<T extends string>({ statuses, selected, basePath, labelFor }: StatusFilterTabsProps<T>) {
  return (
    <nav className={styles.tabs}>
      {statuses.map((status) => (
        <Link
          key={status}
          href={`${basePath}?status=${status}`}
          className={status === selected ? `${styles.tab} ${styles.tabActive}` : styles.tab}
          aria-current={status === selected ? "page" : undefined}
        >
          {labelFor(status)}
        </Link>
      ))}
    </nav>
  );
}
