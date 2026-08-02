import type { RelationshipStep } from "@/lib/familyGraph";

export interface HighlightedPath {
  nodeIds: Set<number>;
  edgeIds: Set<string>;
}

/**
 * Turns findRelationshipPath's step list into the node/edge id sets
 * TreeCanvas needs to draw it -- edge ids must match the "pc-{parent}-
 * {child}" / "sp-{sorted pair}" format useFamilyTreeLayout.ts already
 * generates, so this has to mirror that format exactly rather than
 * inventing its own.
 */
export function computeHighlightedPath(steps: RelationshipStep[] | null): HighlightedPath {
  const nodeIds = new Set<number>();
  const edgeIds = new Set<string>();
  if (!steps) {
    return { nodeIds, edgeIds };
  }

  for (const step of steps) {
    nodeIds.add(step.person.id);
  }

  for (let i = 1; i < steps.length; i++) {
    const previous = steps[i - 1];
    const current = steps[i];
    if (!previous || !current) {
      continue;
    }
    const previousId = previous.person.id;
    const currentId = current.person.id;
    const relation = current.relationToPrevious;

    if (relation === "father" || relation === "mother") {
      // relationToPrevious describes how we got from previous to
      // current, so "father"/"mother" means current is previous's parent.
      edgeIds.add(`pc-${currentId}-${previousId}`);
    } else if (relation === "child") {
      edgeIds.add(`pc-${previousId}-${currentId}`);
    } else if (relation === "spouse") {
      const [a, b] = [previousId, currentId].sort((x, y) => x - y);
      edgeIds.add(`sp-${a}-${b}`);
    }
  }

  return { nodeIds, edgeIds };
}
