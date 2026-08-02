"use client";

import { memo, type KeyboardEvent } from "react";
import { useTranslations } from "next-intl";
import { Handle, Position, type NodeProps, type Node } from "@xyflow/react";
import type { MemberNodeData } from "./familyTree.types";
import styles from "./MemberNode.module.css";

type MemberNodeType = Node<MemberNodeData, "member">;

function MemberNodeComponent({ data }: NodeProps<MemberNodeType>) {
  const t = useTranslations("treePage.node");
  const { person, selected, highlighted, onSelect } = data;

  const name = person.englishFullName.trim() || t("unknownName");
  const isDeceased = Boolean(person.deathDate);
  const birthYear = person.birthDate ? new Date(person.birthDate).getFullYear() : null;
  const deathYear = person.deathDate ? new Date(person.deathDate).getFullYear() : null;
  const years = birthYear ? (isDeceased ? `${birthYear}–${deathYear}` : `b. ${birthYear}`) : t("birthYearUnknown");

  const initials = name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join("") || "?";

  const isFemale = person.gender?.toUpperCase() === "FEMALE";

  function handleKeyDown(event: KeyboardEvent<HTMLDivElement>) {
    if (event.key === "Enter" || event.key === " ") {
      event.preventDefault();
      onSelect?.(person.id);
    }
  }

  return (
    <>
      <Handle type="target" position={Position.Top} id="top-target" className={styles.handle} />
      <Handle type="target" position={Position.Left} id="left-target" className={styles.handle} />
      <Handle type="source" position={Position.Right} id="right-source" className={styles.handle} />
      <Handle type="source" position={Position.Bottom} id="bottom-source" className={styles.handle} />

      <div
        role="button"
        tabIndex={0}
        aria-pressed={selected}
        aria-label={`${name}, ${years}`}
        className={[
          styles.card,
          selected ? styles.cardSelected : "",
          highlighted ? styles.cardHighlighted : "",
          isDeceased ? styles.cardDeceased : "",
        ]
          .filter(Boolean)
          .join(" ")}
        onClick={() => onSelect?.(person.id)}
        onKeyDown={handleKeyDown}
      >
        <span className={isFemale ? `${styles.avatar} ${styles.avatarFemale}` : styles.avatar} aria-hidden="true">
          {initials}
        </span>
        <span className={styles.info}>
          <span className={styles.name}>{name}</span>
          <span className={styles.years}>{years}</span>
        </span>
      </div>
    </>
  );
}

export const MemberNode = memo(MemberNodeComponent);
