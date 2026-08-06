"use client";

import { useCallback, useMemo, useState } from "react";
import { useTranslations } from "next-intl";
import { Maximize, RotateCcw } from "lucide-react";
import { ReactFlowProvider } from "@xyflow/react";
import type { PersonTreeNodeDto } from "@/lib/api";
import { buildGraphIndex, findRelationshipPath } from "@/lib/familyGraph";
import { TreeCanvas } from "./TreeCanvas";
import { TreeFilters } from "./TreeFilters";
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
  const [search, setSearch] = useState("");
  const [selectedId, setSelectedId] = useState<number | null>(initialFocusId);
  const [focusId, setFocusId] = useState<number | null>(initialFocusId);
  const [isFullscreenOpen, setIsFullscreenOpen] = useState(false);

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

  // Search finds and centers the view on a match rather than hiding
  // everyone else -- the whole tree always stays visible, this just
  // tells TreeCanvas who to pan/zoom to.
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

  // A search match counts as "selected" for path-highlighting purposes
  // too, not just for panning -- otherwise the amber line only appears
  // after an extra click on the node the search just zoomed to.
  const pathTargetId = searchMatchId ?? selectedId;

  const selectedPathSteps = useMemo(() => {
    if (pathTargetId == null || selfId == null || pathTargetId === selfId) {
      return null;
    }
    return findRelationshipPath(index, pathTargetId, selfId);
  }, [index, pathTargetId, selfId]);
  const selectedPath = useMemo(() => computeHighlightedPath(selectedPathSteps), [selectedPathSteps]);

  const highlights: TreeHighlights = useMemo(() => ({ rootPath, selectedPath }), [rootPath, selectedPath]);

  function handleResetView() {
    setSearch("");
    setSelectedId(null);
    setFocusId(null);
  }

  // Passed down to TreeCanvas (onSelect) / MemberQuickView (onFocusPerson),
  // which flow into every MemberNode's props -- a new identity each render
  // (e.g. on every search keystroke, since `search` is state on this same
  // component) invalidates TreeCanvas's node-building useMemo and defeats
  // MemberNode's React.memo, re-rendering every node on every keystroke.
  const handleSelect = useCallback((personId: number) => {
    setSelectedId(personId);
  }, []);

  const handleFocusPerson = useCallback((personId: number) => {
    setSelectedId(personId);
    setFocusId(personId);
  }, []);

  const selectedPerson = selectedId != null ? peopleById.get(selectedId) ?? null : null;

  return (
    <div className={styles.wrapper}>
      <div className={styles.toolbarRow}>
        <div className={styles.searchColumn}>
          <TreeFilters search={search} onSearchChange={setSearch} />
        </div>
        <button type="button" className={styles.toolbarButton} onClick={handleResetView} aria-label={t("controls.resetView")}>
          <RotateCcw size={16} aria-hidden="true" />
          <span aria-hidden="true">{t("controls.resetView")}</span>
        </button>
        <button
          type="button"
          className={styles.toolbarButton}
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

      <div className={styles.canvasArea}>
        {people.length === 0 ? (
          <div className={styles.empty}>{t("empty")}</div>
        ) : (
          <ReactFlowProvider>
            <TreeCanvas
              people={people}
              selectedId={selectedId}
              focusId={searchMatchId ?? focusId}
              onSelect={handleSelect}
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
