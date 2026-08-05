import { useMemo } from "react";
import type { Edge, Node } from "@xyflow/react";
import type { PersonTreeNodeDto } from "@/lib/api";
import type { MemberNodeData, TreePathHighlight } from "./familyTree.types";
import { EMPTY_TREE_HIGHLIGHTS, type TreeHighlights } from "./treeHighlight";

export const NODE_WIDTH = 216;
export const NODE_HEIGHT = 88;
const SIBLING_GAP = 40;
const ROW_HEIGHT = NODE_HEIGHT + 88;
const ROOT_TREE_GAP = SIBLING_GAP * 4;

export interface FamilyTreeLayout {
  nodes: Node<MemberNodeData>[];
  edges: Edge[];
}

/**
 * Pure data transform: PersonTreeNodeDto[] -> React Flow nodes/edges,
 * laid out as a classic patrilineal genealogy chart -- the way a
 * Banshawali is traditionally drawn, not a generic directed-graph
 * layout. Kept dependency-free of React Flow's runtime state so it's
 * unit-testable on its own (see useFamilyTreeLayout.test.ts) -- the
 * hook below just memoizes it for TreeCanvas.
 *
 * Father-line only: each child has exactly one structural parent (their
 * father), so every subtree has a single, well-defined center to fan
 * out from -- a generic DAG layout (this used to run on Dagre) can't
 * guarantee that once a child has two incoming edges (father's and
 * mother's) that disagree on where they belong. Mothers/spouses still
 * show in a person's own detail panel, just not as a tree branch (see
 * the "no wife needed" redesign this followed).
 *
 * Each subtree's width is computed bottom-up (post-order), then each
 * parent is centered over the span of their own children (pre-order) --
 * the standard tidy-tree construction. Rows come from generationNumber,
 * the app's own curated "how far down the tree is this person" field
 * (see the admin Generations screen), falling back to tree depth from
 * each root when generationNumber isn't available for everyone (e.g.
 * test fixtures).
 */
export function layoutFamilyTree(
  people: PersonTreeNodeDto[],
  selectedId: number | null,
  highlights: TreeHighlights = EMPTY_TREE_HIGHLIGHTS,
): FamilyTreeLayout {
  const peopleById = new Map(people.map((person) => [person.id, person]));

  function isFather(parentId: number, childId: number): boolean {
    return peopleById.get(childId)?.fatherId === parentId;
  }

  const childrenOf = new Map<number, number[]>();
  for (const person of people) {
    for (const childId of person.childIds) {
      if (!peopleById.has(childId) || !isFather(person.id, childId)) {
        continue;
      }
      const list = childrenOf.get(person.id);
      if (list) {
        list.push(childId);
      } else {
        childrenOf.set(person.id, [childId]);
      }
    }
  }

  const lineagePeople = people.filter(
    (person) => person.fatherId != null || (childrenOf.get(person.id)?.length ?? 0) > 0,
  );
  const lineageIds = new Set(lineagePeople.map((person) => person.id));
  const roots = lineagePeople.filter((person) => person.fatherId == null || !lineageIds.has(person.fatherId));

  function childrenIn(personId: number): number[] {
    return childrenOf.get(personId) ?? [];
  }

  const widthById = new Map<number, number>();
  function subtreeWidth(personId: number): number {
    const cached = widthById.get(personId);
    if (cached != null) {
      return cached;
    }
    const children = childrenIn(personId);
    const width =
      children.length === 0
        ? NODE_WIDTH
        : children.reduce((sum, id) => sum + subtreeWidth(id), 0) + SIBLING_GAP * (children.length - 1);
    widthById.set(personId, width);
    return width;
  }

  const hasCompleteGenerations =
    lineagePeople.length > 0 && lineagePeople.every((person) => person.generationNumber != null);
  const minGeneration = hasCompleteGenerations
    ? Math.min(...lineagePeople.map((person) => person.generationNumber as number))
    : 0;

  const xById = new Map<number, number>();
  const rowById = new Map<number, number>();
  function assignPositions(personId: number, leftEdge: number, depth: number): void {
    const person = peopleById.get(personId);
    rowById.set(personId, hasCompleteGenerations ? (person?.generationNumber as number) - minGeneration : depth);

    const children = childrenIn(personId);
    if (children.length === 0) {
      xById.set(personId, leftEdge + NODE_WIDTH / 2);
      return;
    }
    let cursor = leftEdge;
    for (const childId of children) {
      assignPositions(childId, cursor, depth + 1);
      cursor += subtreeWidth(childId) + SIBLING_GAP;
    }
    const firstChildX = xById.get(children[0] as number) as number;
    const lastChildX = xById.get(children[children.length - 1] as number) as number;
    xById.set(personId, (firstChildX + lastChildX) / 2);
  }

  let cursor = 0;
  for (const root of roots) {
    assignPositions(root.id, cursor, 0);
    cursor += subtreeWidth(root.id) + ROOT_TREE_GAP;
  }

  const nodes: Node<MemberNodeData>[] = lineagePeople.map((person) => {
    const x = xById.get(person.id) ?? 0;
    const row = rowById.get(person.id) ?? 0;
    const pathHighlight: TreePathHighlight = highlights.selectedPath.nodeIds.has(person.id)
      ? "selected"
      : highlights.rootPath.nodeIds.has(person.id)
        ? "root"
        : null;
    return {
      id: String(person.id),
      type: "member",
      position: { x: x - NODE_WIDTH / 2, y: row * ROW_HEIGHT },
      data: { person, selected: person.id === selectedId, pathHighlight },
    };
  });

  const edges: Edge[] = [];
  for (const [parentId, childIds] of childrenOf) {
    for (const childId of childIds) {
      const id = `pc-${parentId}-${childId}`;
      const onSelectedPath = highlights.selectedPath.edgeIds.has(id);
      const onRootPath = highlights.rootPath.edgeIds.has(id);
      edges.push({
        id,
        source: String(parentId),
        target: String(childId),
        sourceHandle: "bottom-source",
        targetHandle: "top-target",
        type: "smoothstep",
        zIndex: onSelectedPath || onRootPath ? 10 : 0,
        style: onSelectedPath
          ? { stroke: "var(--color-warning)", strokeWidth: 3 }
          : onRootPath
            ? { stroke: "var(--color-error)", strokeWidth: 3 }
            : { stroke: "var(--color-neutral-300)", strokeWidth: 1.5 },
      });
    }
  }

  return { nodes, edges };
}

export function useFamilyTreeLayout(
  people: PersonTreeNodeDto[],
  selectedId: number | null,
  highlights: TreeHighlights = EMPTY_TREE_HIGHLIGHTS,
): FamilyTreeLayout {
  return useMemo(() => layoutFamilyTree(people, selectedId, highlights), [people, selectedId, highlights]);
}
