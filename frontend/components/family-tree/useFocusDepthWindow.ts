import { getAncestors, getDescendants, type FamilyGraphIndex } from "@/lib/familyGraph";

// The neighborhood shown when a person is focused in the tree (selected
// via click) -- self, spouses, and ancestors/descendants up to the given
// depth (each with their own spouses so couples stay together). When
// nobody's focused, TreeExplorer shows the whole tree instead of this
// subgraph.
export interface FocusDepth {
  ancestorDepth: number;
  descendantDepth: number;
}

export const DEFAULT_FOCUS_DEPTH: FocusDepth = { ancestorDepth: 3, descendantDepth: 3 };
export const MAX_FOCUS_DEPTH = 20;
export const FOCUS_DEPTH_OPTIONS = [1, 2, 3, 4, 5, 6, 8, 10];

export function computeFocusSubgraphIds(index: FamilyGraphIndex, focusId: number, depth: FocusDepth): Set<number> {
  const ids = new Set<number>([focusId]);
  const self = index.byId.get(focusId);
  if (!self) {
    return ids;
  }

  for (const spouseId of self.spouseIds) {
    ids.add(spouseId);
  }

  for (const ancestor of getAncestors(index, focusId)) {
    if (ancestor.distance > depth.ancestorDepth) {
      continue;
    }
    ids.add(ancestor.person.id);
    for (const spouseId of ancestor.person.spouseIds) {
      ids.add(spouseId);
    }
  }

  for (const descendant of getDescendants(index, focusId)) {
    if (descendant.distance > depth.descendantDepth) {
      continue;
    }
    ids.add(descendant.person.id);
    for (const spouseId of descendant.person.spouseIds) {
      ids.add(spouseId);
    }
  }

  return ids;
}
