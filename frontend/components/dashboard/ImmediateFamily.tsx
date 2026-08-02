import { useTranslations } from "next-intl";
import { Link } from "@/i18n/navigation";
import type { FamilySnapshotDto, PersonSummaryDto } from "@/lib/api";
import styles from "./ImmediateFamily.module.css";

interface ImmediateFamilyProps {
  family: FamilySnapshotDto;
}

// Father/mother/spouses/children straight from the member's own record --
// the registered relatives a member actually asked to see on their
// dashboard, as opposed to AncestorChain's deeper paternal lineage.
export function ImmediateFamily({ family }: ImmediateFamilyProps) {
  const t = useTranslations("dashboardPage.member");

  return (
    <div className={styles.grid}>
      <FamilyMemberCard label={t("father")} people={family.father ? [family.father] : []} emptyLabel={t("notRecorded")} />
      <FamilyMemberCard label={t("mother")} people={family.mother ? [family.mother] : []} emptyLabel={t("notRecorded")} />
      {family.spouses.length > 0 ? <FamilyMemberCard label={t("spouse")} people={family.spouses} /> : null}
      {family.children.length > 0 ? <FamilyMemberCard label={t("children")} people={family.children} /> : null}
    </div>
  );
}

function FamilyMemberCard({
  label,
  people,
  emptyLabel,
}: {
  label: string;
  people: PersonSummaryDto[];
  emptyLabel?: string;
}) {
  return (
    <div className={styles.card}>
      <span className={styles.label}>{label}</span>
      {people.length > 0 ? (
        <div className={styles.names}>
          {people.map((person) => (
            <Link key={person.id} href={`/directory/${person.id}`} className={styles.name}>
              {person.englishFullName}
            </Link>
          ))}
        </div>
      ) : (
        <span className={styles.empty}>{emptyLabel}</span>
      )}
    </div>
  );
}
