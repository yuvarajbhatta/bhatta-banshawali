"use client";

import { useMemo, useState } from "react";
import { useTranslations } from "next-intl";
import { Maximize } from "lucide-react";
import { ReactFlowProvider } from "@xyflow/react";
import type { PersonTreeNodeDto } from "@/lib/api";
import { buildGraphIndex, findRelationshipPath } from "@/lib/familyGraph";
import { TreeCanvas } from "./TreeCanvas";
import { TreeFilters } from "./TreeFilters";
import { TreeFocusControls } from "./TreeFocusControls";
import { MemberQuickView } from "./MemberQuickView";
import { TreeFullscreenView } from "./TreeFullscreenView";
import { computeFocusSubgraphIds, DEFAULT_FOCUS_DEPTH, MAX_FOCUS_DEPTH } from "./useFocusDepthWindow";
import { computeHighlightedPath, type HighlightedPath } from "./treeHighlight";
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
  const [selectedId, setSelectedId] = useState<number | null>(initialFocusId);
  const [focusId, setFocusId] = useState<number | null>(initialFocusId);
  const [ancestorDepth, setAncestorDepth] = useState(DEFAULT_FOCUS_DEPTH.ancestorDepth);
  const [descendantDepth, setDescendantDepth] = useState(DEFAULT_FOCUS_DEPTH.descendantDepth);
  const [highlightPath, setHighlightPath] = useState(false);
  const [isFullscreenOpen, setIsFullscreenOpen] = useState(false);

  const index = useMemo(() => buildGraphIndex(people), [people]);
  const peopleById = useMemo(() => new Map(people.map((person) => [person.id, person])), [people]);

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

  // Search finds and centers the view on a match rather than hiding
  // everyone else -- the whole tree stays visible (see displayedPeople
  // below), this just tells TreeCanvas who to pan/zoom to.
  const searchMatchId = useMemo(() => {
    const normalizedSearch = search.trim().toLowerCase();
    if (!normalizedSearch) {
      return null;
    }
    const match = people.find((person) => {
      const haystack = `${person.englishFullName} ${person.nepaliFullName}`.toLowerCase();
      return haystack.includes(normalizedSearch);
    });
    return match ? match.id : null;
  }, [people, search]);

  const displayedPeople = useMemo(() => {
    // Nobody focused -- show the whole tree by default (search narrows
    // where the view centers, via searchMatchId, not what's rendered).
    if (!focusSubgraphIds) {
      return people;
    }

    const focusedPeople = people.filter((person) => focusSubgraphIds.has(person.id));
    if (!pathSteps) {
      return focusedPeople;
    }

    // The highlighted path always renders in full, even if a step falls
    // outside the current focus depth -- a path that's silently missing
    // its middle is worse than one that ignores the depth setting.
    const seen = new Set(focusedPeople.map((person) => person.id));
    const missingPathPeople = pathSteps.map((step) => step.person).filter((person) => !seen.has(person.id));
    return [...focusedPeople, ...missingPathPeople];
  }, [focusSubgraphIds, people, pathSteps]);

  function handleResetView() {
    setSearch("");
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
      <div className={styles.toolbarRow}>
        <div className={styles.searchColumn}>
          <TreeFilters search={search} onSearchChange={setSearch} />
        </div>
        <button
          type="button"
          className={styles.fullscreenButton}
          onClick={() => {
            setSelectedId(null);
            setIsFullscreenOpen(true);
          }}
          aria-label={t("viewFullTreeCta")}
        >
          <Maximize size={16} aria-hidden="true" />
          <span aria-hidden="true">{t("viewFullTreeCta")}</span>
        </button>
      </div>

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
        {displayedPeople.length === 0 ? (
          <div className={styles.empty}>{t("empty")}</div>
        ) : (
          <ReactFlowProvider>
            <TreeCanvas
              people={displayedPeople}
              selectedId={selectedId}
              focusId={searchMatchId ?? focusId}
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

      {isFullscreenOpen ? (
        <TreeFullscreenView people={people} peopleById={peopleById} onClose={() => setIsFullscreenOpen(false)} />
      ) : null}
    </div>
  );
}
