"use client";

import { useMemo, useState } from "react";
import { useTranslations } from "next-intl";
import { ReactFlowProvider } from "@xyflow/react";
import { TreeCanvas } from "@/components/family-tree/TreeCanvas";
import { MemberQuickView } from "@/components/family-tree/MemberQuickView";
import { buildFamilySubgraphIds, type FamilyGraphIndex } from "@/lib/familyGraph";
import styles from "./FamilyVisualTree.module.css";

interface FamilyVisualTreeProps {
  index: FamilyGraphIndex;
  selfId: number;
}

export function FamilyVisualTree({ index, selfId }: FamilyVisualTreeProps) {
  const t = useTranslations("familyPage.visualTree");
  const [selectedId, setSelectedId] = useState<number | null>(selfId);

  const subgraphPeople = useMemo(() => {
    const ids = buildFamilySubgraphIds(index, selfId);
    return Array.from(ids)
      .map((id) => index.byId.get(id))
      .filter((person): person is NonNullable<typeof person> => Boolean(person));
  }, [index, selfId]);

  const selectedPerson = selectedId != null ? (index.byId.get(selectedId) ?? null) : null;

  return (
    <div className={styles.wrapper}>
      <div className={styles.canvasArea}>
        {subgraphPeople.length === 0 ? (
          <div className={styles.empty}>{t("empty")}</div>
        ) : (
          <ReactFlowProvider>
            <TreeCanvas people={subgraphPeople} selectedId={selectedId} focusId={selfId} onSelect={setSelectedId} />
          </ReactFlowProvider>
        )}
      </div>

      {selectedPerson ? (
        <MemberQuickView
          person={selectedPerson}
          peopleById={index.byId}
          onClose={() => setSelectedId(null)}
          onFocusPerson={setSelectedId}
        />
      ) : null}
    </div>
  );
}
