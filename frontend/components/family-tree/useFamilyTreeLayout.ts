import dagre from "@dagrejs/dagre";
import { useMemo } from "react";
import type { Edge, Node } from "@xyflow/react";
import type { PersonTreeNodeDto } from "@/lib/api";
import type { MemberNodeData } from "./familyTree.types";
import type { HighlightedPath } from "./treeHighlight";

export const NODE_WIDTH = 216;
export const NODE_HEIGHT = 88;

export interface FamilyTreeLayout {
  nodes: Node<MemberNodeData>[];
  edges: Edge[];
}

const EMPTY_HIGHLIGHT: HighlightedPath = { nodeIds: new Set(), edgeIds: new Set() };

/**
 * Pure data transform: PersonTreeNodeDto[] -> Dagre-laid-out React Flow
 * nodes/edges (docs/frontend-redesign-plan.md "family-tree rendering
 * approach"). Kept dependency-free of React Flow's runtime state so it's
 * unit-testable on its own (see useFamilyTreeLayout.test.ts) -- the hook
 * below just memoizes it for TreeCanvas.
 *
 * Only edges between two people both present in `people` are drawn, so
 * filtering the input list (search/generation/living filters) never
 * produces a dangling edge to a node that isn't rendered.
 */
export function layoutFamilyTree(
  people: PersonTreeNodeDto[],
  selectedId: number | null,
  highlight: HighlightedPath = EMPTY_HIGHLIGHT,
): FamilyTreeLayout {
  const graph = new dagre.graphlib.Graph();
  graph.setGraph({ rankdir: "TB", nodesep: 40, ranksep: 88 });
  graph.setDefaultEdgeLabel(() => ({}));

  const idSet = new Set(people.map((person) => person.id));

  for (const person of people) {
    graph.setNode(String(person.id), { width: NODE_WIDTH, height: NODE_HEIGHT });
  }

  const parentChildPairs: { parentId: number; childId: number }[] = [];
  for (const person of people) {
    for (const childId of person.childIds) {
      if (idSet.has(childId)) {
        parentChildPairs.push({ parentId: person.id, childId });
      }
    }
  }
  for (const pair of parentChildPairs) {
    graph.setEdge(String(pair.parentId), String(pair.childId));
  }

  // Isolated people (no parent/child/spouse edge at all, e.g. a lone
  // search match) still need a rank -- Dagre places unconnected nodes
  // fine on its own, nothing extra needed here.
  dagre.layout(graph);

  const positionById = new Map<number, { x: number; y: number }>();
  for (const person of people) {
    const dagreNode = graph.node(String(person.id));
    positionById.set(person.id, { x: dagreNode.x, y: dagreNode.y });
  }

  const nodes: Node<MemberNodeData>[] = people.map((person) => {
    const position = positionById.get(person.id) ?? { x: 0, y: 0 };
    return {
      id: String(person.id),
      type: "member",
      position: { x: position.x - NODE_WIDTH / 2, y: position.y - NODE_HEIGHT / 2 },
      data: { person, selected: person.id === selectedId, highlighted: highlight.nodeIds.has(person.id) },
    };
  });

  const edges: Edge[] = parentChildPairs.map((pair) => {
    const id = `pc-${pair.parentId}-${pair.childId}`;
    const onPath = highlight.edgeIds.has(id);
    return {
      id,
      source: String(pair.parentId),
      target: String(pair.childId),
      sourceHandle: "bottom-source",
      targetHandle: "top-target",
      type: "smoothstep",
      zIndex: onPath ? 10 : 0,
      style: onPath
        ? { stroke: "var(--color-warning)", strokeWidth: 3 }
        : { stroke: "var(--color-neutral-300)", strokeWidth: 1.5 },
    };
  });

  const seenSpousePairs = new Set<string>();
  for (const person of people) {
    for (const spouseId of person.spouseIds) {
      if (!idSet.has(spouseId) || spouseId === person.id) {
        continue;
      }
      const key = [person.id, spouseId].sort((a, b) => a - b).join("-");
      if (seenSpousePairs.has(key)) {
        continue;
      }
      seenSpousePairs.add(key);

      const spouseNodePosition = positionById.get(spouseId);
      const personPosition = positionById.get(person.id);
      const personIsLeft = !personPosition || !spouseNodePosition || personPosition.x <= spouseNodePosition.x;
      const [leftId, rightId] = personIsLeft ? [person.id, spouseId] : [spouseId, person.id];

      const spouseEdgeId = `sp-${key}`;
      const onPath = highlight.edgeIds.has(spouseEdgeId);
      edges.push({
        id: spouseEdgeId,
        source: String(leftId),
        target: String(rightId),
        sourceHandle: "right-source",
        targetHandle: "left-target",
        type: "straight",
        zIndex: onPath ? 10 : 0,
        style: onPath
          ? { stroke: "var(--color-warning)", strokeWidth: 3 }
          : { stroke: "var(--color-neutral-300)", strokeWidth: 1.5 },
      });
    }
  }

  return { nodes, edges };
}

export function useFamilyTreeLayout(
  people: PersonTreeNodeDto[],
  selectedId: number | null,
  highlight: HighlightedPath = EMPTY_HIGHLIGHT,
): FamilyTreeLayout {
  return useMemo(() => layoutFamilyTree(people, selectedId, highlight), [people, selectedId, highlight]);
}
