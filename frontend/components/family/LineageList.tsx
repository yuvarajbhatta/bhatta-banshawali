"use client";

import { useMemo } from "react";
import { useTranslations } from "next-intl";
import { Link } from "@/i18n/navigation";
import type { PersonTreeNodeDto } from "@/lib/api";
import { getAncestors, getDescendants, type FamilyGraphIndex, type LineageEntry } from "@/lib/familyGraph";
import styles from "./LineageList.module.css";

interface LineageListProps {
  index: FamilyGraphIndex;
  selfId: number;
  direction: "ancestors" | "descendants";
}

export function LineageList({ index, selfId, direction }: LineageListProps) {
  const t = useTranslations(`familyPage.${direction}`);

  const entries = useMemo(
    () => (direction === "ancestors" ? getAncestors(index, selfId) : getDescendants(index, selfId)),
    [index, selfId, direction],
  );
  const groups = useMemo(() => groupByDistance(entries), [entries]);

  if (entries.length === 0) {
    return <p className={styles.empty}>{t("empty")}</p>;
  }

  return (
    <div>
      {groups.map(([distance, people]) => (
        <div key={distance} className={styles.group}>
          <h3 className={styles.groupTitle}>{t("distanceLabel", { count: distance })}</h3>
          <div className={styles.rows}>
            {people.map((person) => (
              <LineageRow key={person.id} person={person} />
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}

function groupByDistance(entries: LineageEntry[]): [number, PersonTreeNodeDto[]][] {
  const byDistance = new Map<number, PersonTreeNodeDto[]>();
  for (const entry of entries) {
    const bucket = byDistance.get(entry.distance) ?? [];
    bucket.push(entry.person);
    byDistance.set(entry.distance, bucket);
  }
  return Array.from(byDistance.entries()).sort((a, b) => a[0] - b[0]);
}

function LineageRow({ person }: { person: PersonTreeNodeDto }) {
  const name = person.englishFullName.trim() || "—";
  const initials =
    name
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0]?.toUpperCase())
      .join("") || "?";
  const birthYear = person.birthDate ? new Date(person.birthDate).getFullYear() : null;
  const deathYear = person.deathDate ? new Date(person.deathDate).getFullYear() : null;
  const years = birthYear ? (deathYear ? `${birthYear}–${deathYear}` : `b. ${birthYear}`) : null;

  return (
    <Link href={`/directory/${person.id}`} className={styles.row}>
      <span className={styles.avatar} aria-hidden="true">
        {initials}
      </span>
      <span className={styles.info}>
        <span className={styles.name}>{name}</span>
        {years ? <span className={styles.years}>{years}</span> : null}
      </span>
    </Link>
  );
}
