import { useTranslations } from "next-intl";
import { Link } from "@/i18n/navigation";
import type { PersonSummaryDto } from "@/lib/api";
import styles from "./AncestorChain.module.css";

interface AncestorChainProps {
  chain: PersonSummaryDto[];
}

// The same FATHER-line lineage view an admin sees when linking a signup
// (components/admin/SignupDetail.tsx's CandidateRows), reused here so a
// member can recognize their own ancestry the same way -- a plain
// left-to-right chain rather than the rotating 3D constellation this
// replaced.
export function AncestorChain({ chain }: AncestorChainProps) {
  const t = useTranslations("dashboardPage.member");

  if (chain.length <= 1) {
    return <p className={styles.empty}>{t("ancestorChainUnknown")}</p>;
  }

  return (
    <p className={styles.chain}>
      {chain.map((person, index) => (
        <span key={person.id}>
          {index > 0 ? <span className={styles.arrow}> → </span> : null}
          <Link href={`/directory/${person.id}`} className={styles.link}>
            {person.englishFullName}
          </Link>
        </span>
      ))}
    </p>
  );
}
