"use client";

import { useMemo, useState } from "react";
import { useTranslations } from "next-intl";
import type { PersonTreeNodeDto } from "@/lib/api";
import { buildGraphIndex, getImmediateFamily } from "@/lib/familyGraph";
import { CorrectionForm } from "@/components/CorrectionForm";
import { ImmediateFamily } from "./ImmediateFamily";
import { LineageTimeline } from "./LineageTimeline";
import { LineageList } from "./LineageList";
import { RelationshipPathFinder } from "./RelationshipPathFinder";
import styles from "./YourFamilyExplorer.module.css";

type Tab = "immediateFamily" | "lineage" | "descendants" | "relationshipPath";

const TABS: Tab[] = ["immediateFamily", "lineage", "descendants", "relationshipPath"];

interface YourFamilyExplorerProps {
  people: PersonTreeNodeDto[];
  selfId: number;
}

export function YourFamilyExplorer({ people, selfId }: YourFamilyExplorerProps) {
  const t = useTranslations("familyPage.tabs");
  const [activeTab, setActiveTab] = useState<Tab>("immediateFamily");
  const index = useMemo(() => buildGraphIndex(people), [people]);
  const immediateFamily = useMemo(() => getImmediateFamily(index, selfId), [index, selfId]);

  return (
    <div>
      <div className={styles.tabs} role="tablist">
        {TABS.map((tab) => (
          <button
            key={tab}
            type="button"
            role="tab"
            aria-selected={activeTab === tab}
            className={activeTab === tab ? `${styles.tab} ${styles.tabActive}` : styles.tab}
            onClick={() => setActiveTab(tab)}
          >
            {t(tab)}
          </button>
        ))}
      </div>

      {activeTab === "immediateFamily" && immediateFamily ? (
        <>
          <ImmediateFamily family={immediateFamily} />
          <div className={styles.correction}>
            <CorrectionForm personId={selfId} />
          </div>
        </>
      ) : null}
      {activeTab === "lineage" ? <LineageTimeline index={index} selfId={selfId} /> : null}
      {activeTab === "descendants" ? <LineageList index={index} selfId={selfId} /> : null}
      {activeTab === "relationshipPath" ? <RelationshipPathFinder index={index} selfId={selfId} /> : null}
    </div>
  );
}
