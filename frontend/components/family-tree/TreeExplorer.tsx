"use client";

import { useMemo, useState } from "react";
import { useTranslations } from "next-intl";
import { ReactFlowProvider } from "@xyflow/react";
import type { PersonTreeNodeDto } from "@/lib/api";
import { buildGraphIndex, findRelationshipPath } from "@/lib/familyGraph";
import { TreeCanvas } from "./TreeCanvas";
import { TreeFilters } from "./TreeFilters";
import { TreeFocusControls } from "./TreeFocusControls";
import { MemberQuickView } from "./MemberQuickView";
import { ALL_GENERATIONS_CONFIRM_THRESHOLD, useTreeWindow } from "./useTreeWindow";
import { computeFocusSubgraphIds, DEFAULT_FOCUS_DEPTH, MAX_FOCUS_DEPTH } from "./useFocusDepthWindow";
import { computeHighlightedPath, type HighlightedPath } from "./treeHighlight";
import type { LivingFilter } from "./familyTree.types";
import styles from "./TreeExplorer.module.css";

const EMPTY_HIGHLIGHT: HighlightedPath = { nodeIds: new Set(), edgeIds: new Set() };

interface TreeExplorerProps {
  people: PersonTreeNodeDto[];
  initialFocusId: number | null;
  /** The logged-in member's own person id, if any -- powers "highlight path to me". Null for admins/unlinked accounts, who just don't get that toggle. */
  selfId: number | null;
}

export function TreeExplorer({ people, initialFocusId, selfId }: TreeExplorerProps) {
  const t = useTranslations("treePage");
  const [search, setSearch] = useState("");
  const [living, setLiving] = useState<LivingFilter>("all");
  const [selectedId, setSelectedId] = useState<number | null>(initialFocusId);
  const [focusId, setFocusId] = useState<number | null>(initialFocusId);
  const [ancestorDepth, setAncestorDepth] = useState(DEFAULT_FOCUS_DEPTH.ancestorDepth);
  const [descendantDepth, setDescendantDepth] = useState(DEFAULT_FOCUS_DEPTH.descendantDepth);
  const [highlightPath, setHighlightPath] = useState(false);

  // Bounds what actually reaches Dagre/React Flow to a generation window --
  // see useTreeWindow.ts and docs/08 Phase 5's performance-benchmark
  // follow-up. `people` itself already holds the whole fetched graph; this
  // only slices it, no extra network round-trip. Used when nobody's
  // focused; once someone is, computeFocusSubgraphIds below takes over.
  const treeWindow = useTreeWindow(people);

  const index = useMemo(() => buildGraphIndex(people), [people]);
  const peopleById = useMemo(() => new Map(people.map((person) => [person.id, person])), [people]);

  const generationOptions = useMemo(
    () =>
      Array.from(new Set(people.map((person) => person.generationNumber).filter((gen): gen is number => gen != null))).sort(
        (a, b) => a - b,
      ),
    [people],
  );

  const focusSubgraphIds = useMemo(() => {
    if (selectedId == null) {
      return null;
    }
    return computeFocusSubgraphIds(index, selectedId, { ancestorDepth, descendantDepth });
  }, [index, selectedId, ancestorDepth, descendantDepth]);

  const pathSteps = useMemo(() => {
    if (!highlightPath || selectedId == null || selfId == null || selectedId === selfId) {
      return null;
    }
    return findRelationshipPath(index, selectedId, selfId);
  }, [index, highlightPath, selectedId, selfId]);

  const highlight = useMemo(() => (pathSteps ? computeHighlightedPath(pathSteps) : EMPTY_HIGHLIGHT), [pathSteps]);

  const filteredPeople = useMemo(() => {
    const normalizedSearch = search.trim().toLowerCase();
    const basePeople = focusSubgraphIds ? people.filter((person) => focusSubgraphIds.has(person.id)) : treeWindow.windowedPeople;

    const filteredBase = basePeople.filter((person) => {
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

    if (!pathSteps) {
      return filteredBase;
    }

    // The highlighted path always renders in full, even if a step falls
    // outside the current search/living/depth filters -- a path that's
    // silently missing its middle is worse than one that ignores a filter.
    const seen = new Set(filteredBase.map((person) => person.id));
    const missingPathPeople = pathSteps.map((step) => step.person).filter((person) => !seen.has(person.id));
    return [...filteredBase, ...missingPathPeople];
  }, [focusSubgraphIds, treeWindow.windowedPeople, people, pathSteps, search, living]);

  function handleReset() {
    setSearch("");
    setLiving("all");
    treeWindow.resetToDefaultWindow();
  }

  function handleResetView() {
    handleReset();
    setSelectedId(null);
    setFocusId(null);
    setAncestorDepth(DEFAULT_FOCUS_DEPTH.ancestorDepth);
    setDescendantDepth(DEFAULT_FOCUS_DEPTH.descendantDepth);
    setHighlightPath(false);
  }

  function handleExpandAll() {
    if (selectedId == null) {
      return;
    }
    setAncestorDepth(MAX_FOCUS_DEPTH);
    setDescendantDepth(MAX_FOCUS_DEPTH);
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
        searchScopeLimited={Boolean(search.trim()) && (focusSubgraphIds != null || !treeWindow.isAllGenerations)}
      />

      <TreeFocusControls
        focused={selectedId != null}
        ancestorDepth={ancestorDepth}
        descendantDepth={descendantDepth}
        onAncestorDepthChange={setAncestorDepth}
        onDescendantDepthChange={setDescendantDepth}
        highlightPathAvailable={selfId != null}
        highlightPath={highlightPath}
        onHighlightPathChange={setHighlightPath}
        onExpandAll={handleExpandAll}
        onResetView={handleResetView}
      />

      <div className={styles.canvasArea}>
        {filteredPeople.length === 0 ? (
          <div className={styles.empty}>{t("empty")}</div>
        ) : (
          <ReactFlowProvider>
            <TreeCanvas
              people={filteredPeople}
              selectedId={selectedId}
              focusId={focusId}
              onSelect={handleSelect}
              highlight={highlight}
            />
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
