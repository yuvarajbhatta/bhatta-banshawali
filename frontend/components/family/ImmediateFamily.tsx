import { useTranslations } from "next-intl";
import { Link } from "@/i18n/navigation";
import type { PersonTreeNodeDto } from "@/lib/api";
import type { ImmediateFamily as ImmediateFamilyData } from "@/lib/familyGraph";
import styles from "./ImmediateFamily.module.css";

interface ImmediateFamilyProps {
  family: ImmediateFamilyData;
}

// Parents/spouse/children/siblings/grandparents in one glance -- the
// close-relatives view "Your Family" owns, as opposed to the deeper
// generation-by-generation view LineageTimeline covers.
export function ImmediateFamily({ family }: ImmediateFamilyProps) {
  const t = useTranslations("familyPage.immediateFamily");

  return (
    <div className={styles.wrapper}>
      <div className={styles.grid}>
        <FamilyMemberCard label={t("father")} people={family.father ? [family.father] : []} emptyLabel={t("notRecorded")} />
        <FamilyMemberCard label={t("mother")} people={family.mother ? [family.mother] : []} emptyLabel={t("notRecorded")} />
        {family.grandparents.length > 0 ? <FamilyMemberCard label={t("grandparents")} people={family.grandparents} /> : null}
        {family.siblings.length > 0 ? <FamilyMemberCard label={t("siblings")} people={family.siblings} /> : null}
        {family.spouses.length > 0 ? <FamilyMemberCard label={t("spouse")} people={family.spouses} /> : null}
        {family.children.length > 0 ? <FamilyMemberCard label={t("children")} people={family.children} /> : null}
      </div>
    </div>
  );
}

function FamilyMemberCard({
  label,
  people,
  emptyLabel,
}: {
  label: string;
  people: PersonTreeNodeDto[];
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
