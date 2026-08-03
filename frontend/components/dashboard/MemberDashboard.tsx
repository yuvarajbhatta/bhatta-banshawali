import { getTranslations } from "next-intl/server";
import { Link } from "@/i18n/navigation";
import { Button } from "@/components/Button";
import { Reveal } from "@/components/motion/Reveal";
import type { MemberProfileDto, PersonSummaryDto, PersonTreeNodeDto } from "@/lib/api";
import { buildGraphIndex, getAncestors, getDescendants } from "@/lib/familyGraph";
import styles from "./MemberDashboard.module.css";

interface MemberDashboardProps {
  profile: MemberProfileDto;
  people: PersonTreeNodeDto[];
}

export async function MemberDashboard({ profile, people }: MemberDashboardProps) {
  const t = await getTranslations("dashboardPage.member");

  if (!profile.linked || !profile.person || !profile.family) {
    return (
      <Reveal>
        <div className={styles.notice}>{t("unlinked")}</div>
      </Reveal>
    );
  }

  const { person, family, gotra, memberSince, pendingCorrectionCount } = profile;
  const index = buildGraphIndex(people);
  const ancestorCount = getAncestors(index, person.id).length;
  const descendantCount = getDescendants(index, person.id).length;
  const birthYear = person.birthDate ? new Date(person.birthDate).getFullYear() : null;
  const memberSinceYear = new Date(memberSince).getFullYear();

  return (
    <div className={styles.dashboard}>
      <Reveal>
        <div className={styles.hero}>
          <div>
            <p className={styles.eyebrow}>{t("welcomeBack")}</p>
            <h2 className={styles.name}>{person.englishFullName}</h2>
            {person.nepaliFullName ? <p className={styles.nepaliName}>{person.nepaliFullName}</p> : null}
            {person.generationNumber != null ? (
              <span className={styles.badge}>{t("generation", { number: person.generationNumber })}</span>
            ) : null}
            <dl className={styles.factGrid}>
              <FactRow label={t("father")} person={family.father} notRecordedLabel={t("notRecorded")} />
              <FactRow label={t("mother")} person={family.mother} notRecordedLabel={t("notRecorded")} />
              <div className={styles.fact}>
                <dt>{t("born")}</dt>
                <dd>{birthYear ?? t("notRecorded")}</dd>
              </div>
              {gotra ? (
                <div className={styles.fact}>
                  <dt>{t("gotra")}</dt>
                  <dd>{gotra}</dd>
                </div>
              ) : null}
              <div className={styles.fact}>
                <dt>{t("memberSince")}</dt>
                <dd>{memberSinceYear}</dd>
              </div>
            </dl>
          </div>
          <Link href={`/directory/${person.id}`}>
            <Button variant="secondary">{t("viewFullProfile")}</Button>
          </Link>
        </div>
      </Reveal>

      <Reveal delay={0.1}>
        <div className={styles.statsRow}>
          <StatTile value={ancestorCount} label={t("knownAncestors")} />
          <StatTile value={descendantCount} label={t("directDescendants")} />
          <StatTile value={pendingCorrectionCount} label={t("pendingCorrections")} />
        </div>
      </Reveal>

      <Reveal delay={0.2} className={styles.actions}>
        <Link href="/family">
          <Button variant="primary">{t("viewYourFamily")}</Button>
        </Link>
        <Link href={`/tree?focus=${person.id}`}>
          <Button variant="secondary">{t("viewFullTree")}</Button>
        </Link>
        <Link href="/directory">
          <Button variant="secondary">{t("browseDirectory")}</Button>
        </Link>
      </Reveal>
    </div>
  );
}

function FactRow({
  label,
  person,
  notRecordedLabel,
}: {
  label: string;
  person: PersonSummaryDto | null;
  notRecordedLabel: string;
}) {
  return (
    <div className={styles.fact}>
      <dt>{label}</dt>
      <dd>
        {person ? (
          <Link href={`/directory/${person.id}`} className={styles.factLink}>
            {person.englishFullName}
          </Link>
        ) : (
          notRecordedLabel
        )}
      </dd>
    </div>
  );
}

function StatTile({ value, label }: { value: number; label: string }) {
  return (
    <div className={styles.statTile}>
      <span className={styles.statValue}>{value}</span>
      <span className={styles.statLabel}>{label}</span>
    </div>
  );
}
