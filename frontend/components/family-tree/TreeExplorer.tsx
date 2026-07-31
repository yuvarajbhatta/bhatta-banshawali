"use client";

import { useMemo, useState } from "react";
import { useTranslations } from "next-intl";
import { ReactFlowProvider } from "@xyflow/react";
import type { PersonTreeNodeDto } from "@/lib/api";
import { TreeCanvas } from "./TreeCanvas";
import { TreeFilters } from "./TreeFilters";
import { MemberQuickView } from "./MemberQuickView";
import { ALL_GENERATIONS_CONFIRM_THRESHOLD, useTreeWindow } from "./useTreeWindow";
import type { LivingFilter } from "./familyTree.types";
import styles from "./TreeExplorer.module.css";

interface TreeExplorerProps {
  people: PersonTreeNodeDto[];
  initialFocusId: number | null;
}

export function TreeExplorer({ people, initialFocusId }: TreeExplorerProps) {
  const t = useTranslations("treePage");
  const [search, setSearch] = useState("");
  const [living, setLiving] = useState<LivingFilter>("all");
  const [selectedId, setSelectedId] = useState<number | null>(initialFocusId);
  const [focusId, setFocusId] = useState<number | null>(initialFocusId);

  // Bounds what actually reaches Dagre/React Flow to a generation window --
  // see useTreeWindow.ts and docs/08 Phase 5's performance-benchmark
  // follow-up. `people` itself already holds the whole fetched graph; this
  // only slices it, no extra network round-trip.
  const treeWindow = useTreeWindow(people);

  const peopleById = useMemo(() => new Map(people.map((person) => [person.id, person])), [people]);

  const generationOptions = useMemo(
    () =>
      Array.from(new Set(people.map((person) => person.generationNumber).filter((gen): gen is number => gen != null))).sort(
        (a, b) => a - b,
      ),
    [people],
  );

  const filteredPeople = useMemo(() => {
    const normalizedSearch = search.trim().toLowerCase();
    return treeWindow.windowedPeople.filter((person) => {
      if (normalizedSearch) {
        const haystack = `${person.englishFullName} ${person.nepaliFullName}`.toLowerCase();
        if (!haystack.includes(normalizedSearch)) {
          return false;
        }
      }
      if (living === "living" && person.deathDate) {
        return false;
      }
      if (living === "deceased" && !person.deathDate) {
        return false;
      }
      return true;
    });
  }, [treeWindow.windowedPeople, search, living]);

  function handleReset() {
    setSearch("");
    setLiving("all");
    treeWindow.resetToDefaultWindow();
  }

  function handleSelect(personId: number) {
    setSelectedId(personId);
  }

  function handleFocusPerson(personId: number) {
    setSelectedId(personId);
    setFocusId(personId);
  }

  const selectedPerson = selectedId != null ? peopleById.get(selectedId) ?? null : null;

  return (
    <div className={styles.wrapper}>
      <TreeFilters
        search={search}
        onSearchChange={setSearch}
        generationOptions={generationOptions}
        living={living}
        onLivingChange={setLiving}
        onReset={handleReset}
        visibleCount={filteredPeople.length}
        totalCount={people.length}
        minGeneration={treeWindow.minGeneration}
        maxGeneration={treeWindow.maxGeneration}
        onRangeChange={treeWindow.setRange}
        canLoadEarlier={treeWindow.minGeneration > treeWindow.overallMinGeneration}
        canLoadLater={treeWindow.maxGeneration < treeWindow.overallMaxGeneration}
        onLoadEarlier={treeWindow.loadEarlier}
        onLoadLater={treeWindow.loadLater}
        isAllGenerations={treeWindow.isAllGenerations}
        onShowAllGenerations={treeWindow.showAllGenerations}
        allGenerationsNeedsConfirm={treeWindow.totalPeopleCount > ALL_GENERATIONS_CONFIRM_THRESHOLD}
        searchScopeLimited={Boolean(search.trim()) && !treeWindow.isAllGenerations}
      />

      <div className={styles.canvasArea}>
        {filteredPeople.length === 0 ? (
          <div className={styles.empty}>{t("empty")}</div>
        ) : (
          <ReactFlowProvider>
            <TreeCanvas people={filteredPeople} selectedId={selectedId} focusId={focusId} onSelect={handleSelect} />
          </ReactFlowProvider>
        )}
      </div>

      {selectedPerson ? (
        <MemberQuickView
          person={selectedPerson}
          peopleById={peopleById}
          onClose={() => setSelectedId(null)}
          onFocusPerson={handleFocusPerson}
        />
      ) : null}
    </div>
  );
}
