import dagre from "@dagrejs/dagre";
import { useMemo } from "react";
import type { Edge, Node } from "@xyflow/react";
import type { PersonTreeNodeDto } from "@/lib/api";
import type { MemberNodeData, TreePathHighlight } from "./familyTree.types";
import { EMPTY_TREE_HIGHLIGHTS, type TreeHighlights } from "./treeHighlight";

export const NODE_WIDTH = 216;
export const NODE_HEIGHT = 88;

export interface FamilyTreeLayout {
  nodes: Node<MemberNodeData>[];
  edges: Edge[];
}

/**
 * Pure data transform: PersonTreeNodeDto[] -> Dagre-laid-out React Flow
 * nodes/edges (docs/frontend-redesign-plan.md "family-tree rendering
 * approach"). Kept dependency-free of React Flow's runtime state so it's
 * unit-testable on its own (see useFamilyTreeLayout.test.ts) -- the hook
 * below just memoizes it for TreeCanvas.
 *
 * Lineage-only: no spouse/marriage connections are drawn, and a person
 * recorded solely as someone's spouse (no parents, no children of their
 * own) has nothing left to connect to, so they're dropped from the
 * rendered tree rather than shown as a disconnected floating card.
 *
 * Only edges between two people both present in `people` are drawn, so
 * filtering the input list (search/generation/living filters) never
 * produces a dangling edge to a node that isn't rendered.
 */
export function layoutFamilyTree(
  people: PersonTreeNodeDto[],
  selectedId: number | null,
  highlights: TreeHighlights = EMPTY_TREE_HIGHLIGHTS,
): FamilyTreeLayout {
  const lineagePeople = people.filter(
    (person) => person.childIds.length > 0 || person.fatherId != null || person.motherId != null,
  );

  const graph = new dagre.graphlib.Graph();
  graph.setGraph({ rankdir: "TB", nodesep: 40, ranksep: 88 });
  graph.setDefaultEdgeLabel(() => ({}));

  const idSet = new Set(lineagePeople.map((person) => person.id));

  for (const person of lineagePeople) {
    graph.setNode(String(person.id), { width: NODE_WIDTH, height: NODE_HEIGHT });
  }

  const parentChildPairs: { parentId: number; childId: number }[] = [];
  for (const person of lineagePeople) {
    for (const childId of person.childIds) {
      if (idSet.has(childId)) {
        parentChildPairs.push({ parentId: person.id, childId });
      }
    }
  }
  for (const pair of parentChildPairs) {
    graph.setEdge(String(pair.parentId), String(pair.childId));
  }

  // Isolated people (no parent/child edge at all) still need a rank --
  // Dagre places unconnected nodes fine on its own, nothing extra needed
  // here.
  dagre.layout(graph);

  // Vertical position comes from the curated generationNumber, not
  // Dagre's own rank, whenever every person being laid out has one. In a
  // large historical dataset like this one, a child can have two
  // incoming parent-child edges (father's and mother's) whose own
  // ancestry chains reach different depths elsewhere in the graph --
  // Dagre's rank algorithm pushes the child down to match whichever
  // chain is deepest, landing them many rows below their real parents
  // on screen. generationNumber is this app's single source of truth
  // for "how far down the tree is this person" (see the admin
  // Generations screen), so it decides the row here instead.
  //
  // Handing generationNumber to Dagre directly as a pinned rank (via
  // ranker: "none") was tried and rejected: real data has occasional
  // generationNumber/edge inconsistencies that violate assumptions
  // Dagre's position/normalize pipeline depends on and crash the whole
  // layout. Keeping Dagre's own rank assignment (always internally
  // consistent with its edges, so it never crashes) for horizontal
  // ordering only, and overriding just the row afterwards, gets the
  // correctness without the risk.
  const hasCompleteGenerations =
    lineagePeople.length > 0 && lineagePeople.every((person) => person.generationNumber != null);

  const positionById = new Map<number, { x: number; y: number }>();
  if (hasCompleteGenerations) {
    const minGeneration = Math.min(...lineagePeople.map((person) => person.generationNumber as number));
    const rows = new Map<number, PersonTreeNodeDto[]>();
    for (const person of lineagePeople) {
      const row = (person.generationNumber as number) - minGeneration;
      const people2 = rows.get(row);
      if (people2) {
        people2.push(person);
      } else {
        rows.set(row, [person]);
      }
    }
    for (const [row, peopleInRow] of rows) {
      // Preserve Dagre's own crossing-minimized left-to-right order
      // within the row, just re-spaced evenly so people regrouped out
      // of Dagre's own (possibly different) rank don't overlap.
      peopleInRow.sort((a, b) => graph.node(String(a.id)).x - graph.node(String(b.id)).x);
      peopleInRow.forEach((person, index) => {
        positionById.set(person.id, {
          x: index * (NODE_WIDTH + 40) + NODE_WIDTH / 2,
          y: row * (NODE_HEIGHT + 88) + NODE_HEIGHT / 2,
        });
      });
    }
  } else {
    for (const person of lineagePeople) {
      const dagreNode = graph.node(String(person.id));
      positionById.set(person.id, { x: dagreNode.x, y: dagreNode.y });
    }
  }

  const nodes: Node<MemberNodeData>[] = lineagePeople.map((person) => {
    const position = positionById.get(person.id) ?? { x: 0, y: 0 };
    const pathHighlight: TreePathHighlight = highlights.selectedPath.nodeIds.has(person.id)
      ? "selected"
      : highlights.rootPath.nodeIds.has(person.id)
        ? "root"
        : null;
    return {
      id: String(person.id),
      type: "member",
      position: { x: position.x - NODE_WIDTH / 2, y: position.y - NODE_HEIGHT / 2 },
      data: { person, selected: person.id === selectedId, pathHighlight },
    };
  });

  const edges: Edge[] = parentChildPairs.map((pair) => {
    const id = `pc-${pair.parentId}-${pair.childId}`;
    const onSelectedPath = highlights.selectedPath.edgeIds.has(id);
    const onRootPath = highlights.rootPath.edgeIds.has(id);
    return {
      id,
      source: String(pair.parentId),
      target: String(pair.childId),
      sourceHandle: "bottom-source",
      targetHandle: "top-target",
      type: "smoothstep",
      zIndex: onSelectedPath || onRootPath ? 10 : 0,
      style: onSelectedPath
        ? { stroke: "var(--color-warning)", strokeWidth: 3 }
        : onRootPath
          ? { stroke: "var(--color-error)", strokeWidth: 3 }
          : { stroke: "var(--color-neutral-300)", strokeWidth: 1.5 },
    };
  });

  return { nodes, edges };
}

export function useFamilyTreeLayout(
  people: PersonTreeNodeDto[],
  selectedId: number | null,
  highlights: TreeHighlights = EMPTY_TREE_HIGHLIGHTS,
): FamilyTreeLayout {
  return useMemo(() => layoutFamilyTree(people, selectedId, highlights), [people, selectedId, highlights]);
}
