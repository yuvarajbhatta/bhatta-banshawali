import { describe, expect, it } from "vitest";
import type { PersonTreeNodeDto } from "@/lib/api";
import { buildGraphIndex } from "@/lib/familyGraph";
import { computeFocusSubgraphIds } from "./useFocusDepthWindow";

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
// 3=father, 4=mother (spouses, parents of 5)
// 5=self, spouse 8; 5 and 8's child is 7
// 9=unrelated person, no connection to anyone above
const FIXTURE: PersonTreeNodeDto[] = [
  person({ id: 1, spouseIds: [2], childIds: [3] }),
  person({ id: 2, spouseIds: [1], childIds: [3] }),
  person({ id: 3, fatherId: 1, motherId: 2, spouseIds: [4], childIds: [5] }),
  person({ id: 4, spouseIds: [3], childIds: [5] }),
  person({ id: 5, fatherId: 3, motherId: 4, spouseIds: [8], childIds: [7] }),
  person({ id: 7, fatherId: 5, motherId: 8 }),
  person({ id: 8, spouseIds: [5], childIds: [7] }),
  person({ id: 9 }),
];

describe("computeFocusSubgraphIds", () => {
  it("includes self, spouse, and ancestors/descendants within depth, each with their own spouses", () => {
    const index = buildGraphIndex(FIXTURE);
    const ids = computeFocusSubgraphIds(index, 5, { ancestorDepth: 2, descendantDepth: 1 });

    expect(ids).toEqual(new Set([5, 8, 3, 4, 1, 2, 7]));
  });

  it("excludes generations beyond the requested depth", () => {
    const index = buildGraphIndex(FIXTURE);
    const ids = computeFocusSubgraphIds(index, 5, { ancestorDepth: 1, descendantDepth: 0 });

    expect(ids.has(1)).toBe(false); // grandparent, out of range
    expect(ids.has(7)).toBe(false); // child (descendant), out of range
    expect(ids.has(3)).toBe(true); // parent, in range
    expect(ids.has(8)).toBe(true); // self's spouse, always included
  });

  it("excludes an unrelated person entirely", () => {
    const index = buildGraphIndex(FIXTURE);
    const ids = computeFocusSubgraphIds(index, 5, { ancestorDepth: 5, descendantDepth: 5 });

    expect(ids.has(9)).toBe(false);
  });

  it("returns just the focus id when that person isn't in the graph", () => {
    const index = buildGraphIndex(FIXTURE);
    const ids = computeFocusSubgraphIds(index, 999, { ancestorDepth: 3, descendantDepth: 3 });

    expect(ids).toEqual(new Set([999]));
  });
});
