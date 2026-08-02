"use client";

import { Fragment, useMemo, useState } from "react";
import { useTranslations } from "next-intl";
import { Link } from "@/i18n/navigation";
import type { PersonTreeNodeDto } from "@/lib/api";
import { getAncestors, type FamilyGraphIndex } from "@/lib/familyGraph";
import styles from "./LineageTimeline.module.css";

interface LineageTimelineProps {
  index: FamilyGraphIndex;
  selfId: number;
}

const COLLAPSE_THRESHOLD = 6;
const HEAD_SIZE = 2;
const TAIL_SIZE = 3;

type TimelineItem =
  | { kind: "ancestor"; distance: number; people: PersonTreeNodeDto[]; oldest: boolean }
  | { kind: "collapsed"; count: number }
  | { kind: "self"; person: PersonTreeNodeDto };

// A single connected vertical line, oldest ancestor at the top down to
// "You" at the bottom -- replaces the old arrow-joined single-line
// chain. Unlike that FATHER-only chain, getAncestors() walks both
// parents, so a generation can hold more than one recorded person
// (e.g. both grandfathers); each row groups everyone at that distance
// under one dot rather than giving every person their own row.
export function LineageTimeline({ index, selfId }: LineageTimelineProps) {
  const t = useTranslations("familyPage.lineage");
  const commonT = useTranslations("familyPage");
  const [expanded, setExpanded] = useState(false);

  const self = index.byId.get(selfId);

  const groups = useMemo(() => {
    const entries = getAncestors(index, selfId);
    const byDistance = new Map<number, PersonTreeNodeDto[]>();
    for (const entry of entries) {
      const bucket = byDistance.get(entry.distance) ?? [];
      bucket.push(entry.person);
      byDistance.set(entry.distance, bucket);
    }
    // Oldest (largest distance) first, so the list reads top-to-bottom
    // the same direction a family tree grows downward.
    return Array.from(byDistance.entries()).sort((a, b) => b[0] - a[0]);
  }, [index, selfId]);

  if (!self) {
    return null;
  }

  const items = buildTimelineItems(groups, expanded, self);

  return (
    <div>
      {groups.length === 0 ? <p className={styles.empty}>{t("empty")}</p> : null}
      <ol className={styles.list}>
      {items.map((item, i) => {
        const isLast = i === items.length - 1;
        if (item.kind === "collapsed") {
          return (
            <li key="collapsed" className={styles.row}>
              <Rail isLast={isLast} muted />
              <div className={styles.content}>
                <button type="button" className={styles.expandButton} onClick={() => setExpanded(true)}>
                  {t("showMore", { count: item.count })}
                </button>
              </div>
            </li>
          );
        }
        if (item.kind === "self") {
          return (
            <li key="self" className={styles.row}>
              <Rail isLast={isLast} self />
              <div className={styles.content}>
                <span className={styles.selfBadge}>{commonT("you")}</span>
                <span className={styles.name}>{item.person.englishFullName}</span>
              </div>
            </li>
          );
        }
        return (
          <li key={item.distance} className={styles.row}>
            <Rail isLast={isLast} />
            <div className={styles.content}>
              <div className={styles.meta}>
                <span className={styles.generationBadge}>{t("distanceLabel", { count: item.distance })}</span>
                {item.oldest ? <span className={styles.oldestTag}>{t("oldest")}</span> : null}
              </div>
              <div className={styles.names}>
                {item.people.map((person, personIndex) => (
                  <Fragment key={person.id}>
                    {personIndex > 0 ? <span className={styles.separator}>·</span> : null}
                    <Link href={`/directory/${person.id}`} className={styles.name}>
                      {person.englishFullName}
                    </Link>
                  </Fragment>
                ))}
              </div>
            </div>
          </li>
        );
      })}
      </ol>
    </div>
  );
}

function Rail({ isLast, muted, self }: { isLast: boolean; muted?: boolean; self?: boolean }) {
  return (
    <div className={styles.rail}>
      <span className={[styles.dot, muted ? styles.dotMuted : "", self ? styles.dotSelf : ""].filter(Boolean).join(" ")} />
      {!isLast ? <span className={styles.line} /> : null}
    </div>
  );
}

function buildTimelineItems(
  groups: [number, PersonTreeNodeDto[]][],
  expanded: boolean,
  self: PersonTreeNodeDto,
): TimelineItem[] {
  const ancestorItems: TimelineItem[] = groups.map(([distance, people], i) => ({
    kind: "ancestor",
    distance,
    people,
    oldest: i === 0,
  }));

  const shouldCollapse = !expanded && ancestorItems.length > COLLAPSE_THRESHOLD;
  const visible = shouldCollapse
    ? [
        ...ancestorItems.slice(0, HEAD_SIZE),
        {
          kind: "collapsed" as const,
          count: ancestorItems.length - HEAD_SIZE - TAIL_SIZE,
        },
        ...ancestorItems.slice(ancestorItems.length - TAIL_SIZE),
      ]
    : ancestorItems;

  return [...visible, { kind: "self", person: self }];
}
