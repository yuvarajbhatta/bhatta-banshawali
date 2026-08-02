import { describe, expect, it } from "vitest";
import type { PersonTreeNodeDto } from "./api";
import { buildGraphIndex, findRelationshipPath, getAncestors, getDescendants, getImmediateFamily } from "./familyGraph";

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
// 5=self, 6=sibling, 7=self's child
// 9=unrelated person, no connection to anyone above
const FIXTURE: PersonTreeNodeDto[] = [
  person({ id: 1, spouseIds: [2], childIds: [3] }),
  person({ id: 2, spouseIds: [1], childIds: [3] }),
  person({ id: 3, fatherId: 1, motherId: 2, spouseIds: [4], childIds: [5, 6] }),
  person({ id: 4, spouseIds: [3], childIds: [5, 6] }),
  person({ id: 5, fatherId: 3, motherId: 4, childIds: [7] }),
  person({ id: 6, fatherId: 3, motherId: 4 }),
  person({ id: 7, fatherId: 5 }),
  person({ id: 9 }),
];

describe("getAncestors", () => {
  it("returns parents at distance 1 and grandparents at distance 2", () => {
    const index = buildGraphIndex(FIXTURE);
    const ancestors = getAncestors(index, 5);

    const byId = new Map(ancestors.map((a) => [a.person.id, a.distance]));
    expect(byId.get(3)).toBe(1);
    expect(byId.get(4)).toBe(1);
    expect(byId.get(1)).toBe(2);
    expect(byId.get(2)).toBe(2);
    expect(ancestors).toHaveLength(4);
  });

  it("returns nothing for a person with no recorded parents", () => {
    const index = buildGraphIndex(FIXTURE);
    expect(getAncestors(index, 1)).toEqual([]);
  });
});

describe("getDescendants", () => {
  it("returns children at distance 1 and grandchildren at distance 2", () => {
    const index = buildGraphIndex(FIXTURE);
    const descendants = getDescendants(index, 3);
    const byId = new Map(descendants.map((d) => [d.person.id, d.distance]));

    expect(byId.get(5)).toBe(1);
    expect(byId.get(6)).toBe(1);
    expect(byId.get(7)).toBe(2);
    expect(descendants).toHaveLength(3);
  });
});

describe("getImmediateFamily", () => {
  it("resolves father, mother, spouse, children, and siblings", () => {
    const index = buildGraphIndex(FIXTURE);
    const family = getImmediateFamily(index, 5);

    expect(family?.father?.id).toBe(3);
    expect(family?.mother?.id).toBe(4);
    expect(family?.children.map((c) => c.id)).toEqual([7]);
    expect(family?.siblings.map((s) => s.id)).toEqual([6]);
  });

  it("returns null for an id not present in the graph", () => {
    const index = buildGraphIndex(FIXTURE);
    expect(getImmediateFamily(index, 999)).toBeNull();
  });

  it("resolves grandparents through both parents, deduped and filtered", () => {
    const index = buildGraphIndex(FIXTURE);
    const family = getImmediateFamily(index, 5);

    // Father (3)'s parents are 1 and 2; mother (4) has none recorded.
    expect(family?.grandparents.map((g) => g.id).sort()).toEqual([1, 2]);
  });

  it("returns no grandparents when neither parent has parents recorded", () => {
    const index = buildGraphIndex(FIXTURE);
    const family = getImmediateFamily(index, 3);

    expect(family?.grandparents).toEqual([]);
  });
});

describe("findRelationshipPath", () => {
  it("returns a single-step path when from and to are the same person", () => {
    const index = buildGraphIndex(FIXTURE);
    const path = findRelationshipPath(index, 5, 5);
    expect(path).toEqual([{ person: FIXTURE.find((p) => p.id === 5), relationToPrevious: null }]);
  });

  it("finds the direct parent path", () => {
    const index = buildGraphIndex(FIXTURE);
    const path = findRelationshipPath(index, 5, 3);
    expect(path?.map((s) => [s.person.id, s.relationToPrevious])).toEqual([
      [5, null],
      [3, "father"],
    ]);
  });

  it("finds a multi-hop path through a grandparent", () => {
    const index = buildGraphIndex(FIXTURE);
    const path = findRelationshipPath(index, 5, 1);
    expect(path?.map((s) => s.person.id)).toEqual([5, 3, 1]);
    expect(path?.map((s) => s.relationToPrevious)).toEqual([null, "father", "father"]);
  });

  it("finds a path through a sibling via the shared parent", () => {
    const index = buildGraphIndex(FIXTURE);
    const path = findRelationshipPath(index, 5, 6);
    expect(path?.map((s) => s.person.id)).toEqual([5, 3, 6]);
  });

  it("returns null when there is no path between two people", () => {
    const index = buildGraphIndex(FIXTURE);
    expect(findRelationshipPath(index, 5, 9)).toBeNull();
  });

  it("returns null for an id that isn't in the graph", () => {
    const index = buildGraphIndex(FIXTURE);
    expect(findRelationshipPath(index, 5, 999)).toBeNull();
  });
});
