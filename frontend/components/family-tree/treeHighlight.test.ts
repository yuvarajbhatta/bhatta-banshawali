import { describe, expect, it } from "vitest";
import type { PersonTreeNodeDto } from "@/lib/api";
import { buildGraphIndex, findRelationshipPath } from "@/lib/familyGraph";
import { computeHighlightedPath } from "./treeHighlight";
import { layoutFamilyTree } from "./useFamilyTreeLayout";

function person(overrides: Partial<PersonTreeNodeDto> & { id: number }): PersonTreeNodeDto {
  return {
    englishFullName: `Person ${overrides.id}`,
    nepaliFullName: "",
    gender: null,
    generationNumber: null,
    birthDate: null,
    deathDate: null,
    fatherId: null,
    motherId: null,
    spouseIds: [],
    childIds: [],
    ...overrides,
  };
}

// 1=grandfather, 2=grandmother (spouses, parents of 3)
// 3=father, 4=mother (spouses, parents of 5 and 6)
// 5=self, 6=sibling
const FIXTURE: PersonTreeNodeDto[] = [
  person({ id: 1, spouseIds: [2], childIds: [3] }),
  person({ id: 2, spouseIds: [1], childIds: [3] }),
  person({ id: 3, fatherId: 1, motherId: 2, spouseIds: [4], childIds: [5, 6] }),
  person({ id: 4, spouseIds: [3], childIds: [5, 6] }),
  person({ id: 5, fatherId: 3, motherId: 4 }),
  person({ id: 6, fatherId: 3, motherId: 4 }),
];

describe("computeHighlightedPath", () => {
  it("returns empty sets for a null path", () => {
    expect(computeHighlightedPath(null)).toEqual({ nodeIds: new Set(), edgeIds: new Set() });
  });

  it("marks every person on the path as a highlighted node", () => {
    const index = buildGraphIndex(FIXTURE);
    const steps = findRelationshipPath(index, 5, 1); // self -> father -> grandfather
    const { nodeIds } = computeHighlightedPath(steps);
    expect(nodeIds).toEqual(new Set([5, 3, 1]));
  });

  it("produces parent-child edge ids matching layoutFamilyTree's own format", () => {
    const index = buildGraphIndex(FIXTURE);
    const steps = findRelationshipPath(index, 5, 1);
    const { edgeIds } = computeHighlightedPath(steps);

    const { edges } = layoutFamilyTree(FIXTURE, null);
    const realEdgeIds = new Set(edges.map((e) => e.id));

    for (const id of edgeIds) {
      expect(realEdgeIds.has(id)).toBe(true);
    }
    expect(edgeIds).toEqual(new Set(["pc-3-5", "pc-1-3"]));
  });

  it("produces a sibling path through the shared parent", () => {
    const index = buildGraphIndex(FIXTURE);
    const steps = findRelationshipPath(index, 5, 6);
    const { edgeIds, nodeIds } = computeHighlightedPath(steps);

    expect(nodeIds).toEqual(new Set([5, 3, 6]));
    expect(edgeIds).toEqual(new Set(["pc-3-5", "pc-3-6"]));
  });

  it("produces a spouse edge id matching layoutFamilyTree's own format for a path crossing a marriage", () => {
    // motherId deliberately unset on 12, so the only path to 11 is via
    // the father-then-spouse hop, not a direct mother edge.
    const spouseFixture: PersonTreeNodeDto[] = [
      person({ id: 10, spouseIds: [11], childIds: [12] }),
      person({ id: 11, spouseIds: [10] }),
      person({ id: 12, fatherId: 10 }),
    ];
    const index = buildGraphIndex(spouseFixture);
    const steps = findRelationshipPath(index, 12, 11);
    const { edgeIds } = computeHighlightedPath(steps);

    const { edges } = layoutFamilyTree(spouseFixture, null);
    const realEdgeIds = new Set(edges.map((e) => e.id));

    for (const id of edgeIds) {
      expect(realEdgeIds.has(id)).toBe(true);
    }
    expect(edgeIds).toEqual(new Set(["pc-10-12", "sp-10-11"]));
  });
});
