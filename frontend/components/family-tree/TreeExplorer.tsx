"use client";

import { useCallback, useMemo, useState } from "react";
import { useTranslations } from "next-intl";
import { ReactFlowProvider } from "@xyflow/react";
import type { PersonTreeNodeDto } from "@/lib/api";
import { buildGraphIndex, findRelationshipPath } from "@/lib/familyGraph";
import { TreeCanvas } from "./TreeCanvas";
import { MemberQuickView } from "./MemberQuickView";
import { TreeFullscreenView } from "./TreeFullscreenView";
import { computeHighlightedPath, type TreeHighlights } from "./treeHighlight";
import styles from "./TreeExplorer.module.css";

interface TreeExplorerProps {
  people: PersonTreeNodeDto[];
  initialFocusId: number | null;
  /** The tree's designated founder/root person, if any -- the start of the always-on "path to you" highlight. */
  rootPersonId: number | null;
  /** The logged-in member's own person id, if any -- powers the "path to you" highlight. Null for admins/unlinked accounts, who just don't get it. */
  selfId: number | null;
}

export function TreeExplorer({ people, initialFocusId, rootPersonId, selfId }: TreeExplorerProps) {
  const t = useTranslations("treePage");
  const [selectedId, setSelectedId] = useState<number | null>(initialFocusId);
  const [focusId, setFocusId] = useState<number | null>(initialFocusId);
  const [isFullscreenOpen, setIsFullscreenOpen] = useState(false);

  // Search now lives entirely in the header (HeaderSearch shows tree-
  // specific wording/behavior on this page -- see its own comment), which
  // navigates here via ?focus=<id> rather than holding any local search
  // state. initialFocusId is a prop, not state, so it only takes effect on
  // mount by default -- this re-syncs selectedId/focusId whenever the
  // header search (or any other ?focus= link) changes it, without a
  // useEffect: adjusting state during render, conditioned on a value
  // actually having changed since the last render, is the pattern React
  // itself recommends for "reset state when a prop changes" (avoids an
  // extra commit + the setState-in-effect anti-pattern).
  const [lastSyncedFocusId, setLastSyncedFocusId] = useState(initialFocusId);
  if (initialFocusId !== lastSyncedFocusId) {
    setLastSyncedFocusId(initialFocusId);
    setSelectedId(initialFocusId);
    setFocusId(initialFocusId);
  }

  const index = useMemo(() => buildGraphIndex(people), [people]);
  const peopleById = useMemo(() => new Map(people.map((person) => [person.id, person])), [people]);

  // Always on (once the viewer has a linked person) so they can spot
  // themselves in the whole tree without having to search -- root ->
  // self, in red. Distinct from selectedPath below (amber), which
  // appears once someone's tapped or searched-and-matched.
  const rootPathSteps = useMemo(() => {
    if (rootPersonId == null || selfId == null || rootPersonId === selfId) {
      return null;
    }
    return findRelationshipPath(index, rootPersonId, selfId);
  }, [index, rootPersonId, selfId]);
  const rootPath = useMemo(() => computeHighlightedPath(rootPathSteps), [rootPathSteps]);

  // A focused/selected person gets the amber path highlight too, not just
  // panning/the quick-view popup -- covers both a node clicked directly on
  // the canvas and a person reached via the header search's ?focus= link.
  const pathTargetId = focusId ?? selectedId;

  const selectedPathSteps = useMemo(() => {
    if (pathTargetId == null || selfId == null || pathTargetId === selfId) {
      return null;
    }
    return findRelationshipPath(index, pathTargetId, selfId);
  }, [index, pathTargetId, selfId]);
  const selectedPath = useMemo(() => computeHighlightedPath(selectedPathSteps), [selectedPathSteps]);

  const highlights: TreeHighlights = useMemo(() => ({ rootPath, selectedPath }), [rootPath, selectedPath]);

  const handleResetView = useCallback(() => {
    setSelectedId(null);
    setFocusId(null);
  }, []);

  // Passed down to TreeCanvas (onSelect) / MemberQuickView (onFocusPerson),
  // which flow into every MemberNode's props -- a new identity each render
  // defeats TreeCanvas's node-building useMemo and MemberNode's React.memo,
  // re-rendering every node unnecessarily.
  const handleSelect = useCallback((personId: number) => {
    setSelectedId(personId);
  }, []);

  const handleFocusPerson = useCallback((personId: number) => {
    setSelectedId(personId);
    setFocusId(personId);
  }, []);

  const handleOpenFullscreen = useCallback(() => {
    setSelectedId(null);
    setIsFullscreenOpen(true);
  }, []);

  const selectedPerson = selectedId != null ? peopleById.get(selectedId) ?? null : null;

  return (
    <div className={styles.wrapper}>
      <div className={styles.canvasArea}>
        {people.length === 0 ? (
          <div className={styles.empty}>{t("empty")}</div>
        ) : (
          <ReactFlowProvider>
            <TreeCanvas
              people={people}
              selectedId={selectedId}
              focusId={focusId}
              onSelect={handleSelect}
              onReset={handleResetView}
              onOpenFullscreen={handleOpenFullscreen}
              highlights={highlights}
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
        <TreeFullscreenView
          people={people}
          peopleById={peopleById}
          rootPath={rootPath}
          onClose={() => setIsFullscreenOpen(false)}
        />
      ) : null}
    </div>
  );
}
