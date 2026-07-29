"use client";

import { useMemo, useState } from "react";
import { useTranslations } from "next-intl";
import type { PersonTreeNodeDto } from "@/lib/api";
import { buildGraphIndex } from "@/lib/familyGraph";
import { FamilyVisualTree } from "./FamilyVisualTree";
import { LineageList } from "./LineageList";
import { RelationshipPathFinder } from "./RelationshipPathFinder";
import styles from "./YourFamilyExplorer.module.css";

type Tab = "visualTree" | "ancestors" | "descendants" | "relationshipPath";

const TABS: Tab[] = ["visualTree", "ancestors", "descendants", "relationshipPath"];

interface YourFamilyExplorerProps {
  people: PersonTreeNodeDto[];
  selfId: number;
}

export function YourFamilyExplorer({ people, selfId }: YourFamilyExplorerProps) {
  const t = useTranslations("familyPage.tabs");
  const [activeTab, setActiveTab] = useState<Tab>("visualTree");
  const index = useMemo(() => buildGraphIndex(people), [people]);

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

      {activeTab === "visualTree" ? <FamilyVisualTree index={index} selfId={selfId} /> : null}
      {activeTab === "ancestors" ? <LineageList index={index} selfId={selfId} direction="ancestors" /> : null}
      {activeTab === "descendants" ? <LineageList index={index} selfId={selfId} direction="descendants" /> : null}
      {activeTab === "relationshipPath" ? <RelationshipPathFinder index={index} selfId={selfId} /> : null}
    </div>
  );
}
