import type { PersonTreeNodeDto } from "./api";

/**
 * Pure graph algorithms over the same flat PersonTreeNodeDto[] payload
 * GET /api/v1/family-tree already returns (docs/frontend-redesign-plan.md).
 * The "Your Family" page (docs/06-ui-ux-specification.md) needs ancestors,
 * descendants, immediate family, a relationship-path calculator, and a
 * bounded subgraph for its visual tree tab -- all of that is derivable
 * client-side from data already fetched for /tree, so none of this needs
 * a new backend endpoint. Kept dependency-free of React so it's directly
 * unit-testable (see familyGraph.test.ts).
 */

export interface FamilyGraphIndex {
  byId: Map<number, PersonTreeNodeDto>;
}

export function buildGraphIndex(nodes: PersonTreeNodeDto[]): FamilyGraphIndex {
  return { byId: new Map(nodes.map((node) => [node.id, node])) };
}

export interface LineageEntry {
  person: PersonTreeNodeDto;
  /** 1 = parent/child, 2 = grandparent/grandchild, etc. */
  distance: number;
}

export function getAncestors(index: FamilyGraphIndex, selfId: number): LineageEntry[] {
  return walkLineage(index, selfId, (person) =>
    [person.fatherId, person.motherId].filter((id): id is number => id != null),
  );
}

export function getDescendants(index: FamilyGraphIndex, selfId: number): LineageEntry[] {
  return walkLineage(index, selfId, (person) => person.childIds);
}

function walkLineage(
  index: FamilyGraphIndex,
  selfId: number,
  neighborsOf: (person: PersonTreeNodeDto) => number[],
): LineageEntry[] {
  const result: LineageEntry[] = [];
  const visited = new Set<number>([selfId]);
  let frontier = [selfId];
  let distance = 0;

  while (frontier.length > 0) {
    distance += 1;
    const next: number[] = [];
    for (const id of frontier) {
      const person = index.byId.get(id);
      if (!person) {
        continue;
      }
      for (const neighborId of neighborsOf(person)) {
        if (visited.has(neighborId)) {
          continue;
        }
        const neighbor = index.byId.get(neighborId);
        if (!neighbor) {
          continue;
        }
        visited.add(neighborId);
        result.push({ person: neighbor, distance });
        next.push(neighborId);
      }
    }
    frontier = next;
  }

  return result;
}

export interface ImmediateFamily {
  self: PersonTreeNodeDto;
  father: PersonTreeNodeDto | null;
  mother: PersonTreeNodeDto | null;
  spouses: PersonTreeNodeDto[];
  children: PersonTreeNodeDto[];
  siblings: PersonTreeNodeDto[];
  grandparents: PersonTreeNodeDto[];
}

export function getImmediateFamily(index: FamilyGraphIndex, selfId: number): ImmediateFamily | null {
  const self = index.byId.get(selfId);
  if (!self) {
    return null;
  }

  const father = self.fatherId != null ? (index.byId.get(self.fatherId) ?? null) : null;
  const mother = self.motherId != null ? (index.byId.get(self.motherId) ?? null) : null;
  const spouses = resolveAll(index, self.spouseIds);
  const children = resolveAll(index, self.childIds);

  const siblingIds = new Set<number>();
  if (father) {
    for (const id of father.childIds) {
      if (id !== selfId) siblingIds.add(id);
    }
  }
  if (mother) {
    for (const id of mother.childIds) {
      if (id !== selfId) siblingIds.add(id);
    }
  }

  const grandparentIds = [father?.fatherId, father?.motherId, mother?.fatherId, mother?.motherId].filter(
    (id): id is number => id != null,
  );

  return {
    self,
    father,
    mother,
    spouses,
    children,
    siblings: resolveAll(index, Array.from(siblingIds)),
    grandparents: resolveAll(index, grandparentIds),
  };
}

function resolveAll(index: FamilyGraphIndex, ids: number[]): PersonTreeNodeDto[] {
  return ids.map((id) => index.byId.get(id)).filter((person): person is PersonTreeNodeDto => Boolean(person));
}

export type RelationKind = "father" | "mother" | "child" | "spouse";

export interface RelationshipStep {
  person: PersonTreeNodeDto;
  /** How this step relates to the previous one in the path. Null for the starting person. */
  relationToPrevious: RelationKind | null;
}

/**
 * Shortest path between two people over the undirected combination of
 * FATHER/MOTHER/CHILD/SPOUSE edges (BFS, unweighted). Each step is
 * labeled with the single relationship type connecting it to the
 * previous step -- e.g. "You -> Father -> Father" for a paternal
 * grandfather -- rather than synthesizing a composite term like
 * "grandfather", which the data model has no basis for once step/adopted
 * relationships or ambiguous multi-path lineages are considered.
 */
export function findRelationshipPath(index: FamilyGraphIndex, fromId: number, toId: number): RelationshipStep[] | null {
  const start = index.byId.get(fromId);
  if (!start || !index.byId.has(toId)) {
    return null;
  }
  if (fromId === toId) {
    return [{ person: start, relationToPrevious: null }];
  }

  const cameFrom = new Map<number, { from: number; relation: RelationKind }>();
  const visited = new Set<number>([fromId]);
  const queue: number[] = [fromId];

  while (queue.length > 0) {
    const currentId = queue.shift() as number;
    if (currentId === toId) {
      break;
    }
    const current = index.byId.get(currentId);
    if (!current) {
      continue;
    }

    const neighbors: { id: number; relation: RelationKind }[] = [];
    if (current.fatherId != null) neighbors.push({ id: current.fatherId, relation: "father" });
    if (current.motherId != null) neighbors.push({ id: current.motherId, relation: "mother" });
    for (const childId of current.childIds) neighbors.push({ id: childId, relation: "child" });
    for (const spouseId of current.spouseIds) neighbors.push({ id: spouseId, relation: "spouse" });

    for (const neighbor of neighbors) {
      if (visited.has(neighbor.id) || !index.byId.has(neighbor.id)) {
        continue;
      }
      visited.add(neighbor.id);
      cameFrom.set(neighbor.id, { from: currentId, relation: neighbor.relation });
      queue.push(neighbor.id);
    }
  }

  if (!visited.has(toId)) {
    return null;
  }

  const reversed: { id: number; relation: RelationKind | null }[] = [];
  let cursor = toId;
  while (cursor !== fromId) {
    const step = cameFrom.get(cursor);
    if (!step) {
      return null;
    }
    reversed.push({ id: cursor, relation: step.relation });
    cursor = step.from;
  }
  reversed.push({ id: fromId, relation: null });
  reversed.reverse();

  const steps = reversed
    .map(({ id, relation }) => {
      const person = index.byId.get(id);
      return person ? { person, relationToPrevious: relation } : null;
    })
    .filter((step): step is RelationshipStep => step !== null);

  return steps.length === reversed.length ? steps : null;
}
