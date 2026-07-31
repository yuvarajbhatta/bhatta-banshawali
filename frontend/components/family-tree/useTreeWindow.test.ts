import { describe, expect, it } from "vitest";
import type { PersonTreeNodeDto } from "@/lib/api";
import {
  computeDefaultRange,
  computeGenerationBounds,
  expandEarlier,
  expandLater,
  filterByWindow,
  showAllGenerationsRange,
} from "./useTreeWindow";

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

describe("computeGenerationBounds", () => {
  it("returns the min and max generation across all people", () => {
    const people = [person({ id: 1, generationNumber: 3 }), person({ id: 2, generationNumber: 1 }), person({ id: 3, generationNumber: 5 })];
    expect(computeGenerationBounds(people)).toEqual({ min: 1, max: 5 });
  });

  it("ignores people with no generation number", () => {
    const people = [person({ id: 1, generationNumber: null }), person({ id: 2, generationNumber: 2 })];
    expect(computeGenerationBounds(people)).toEqual({ min: 2, max: 2 });
  });

  it("returns 0/0 when nobody has a generation number", () => {
    expect(computeGenerationBounds([person({ id: 1, generationNumber: null })])).toEqual({ min: 0, max: 0 });
  });
});

describe("computeDefaultRange", () => {
  it("defaults to a 4-generation window starting at the minimum", () => {
    const range = computeDefaultRange({ min: 1, max: 20 });
    expect(range).toEqual({ minGeneration: 1, maxGeneration: 4, isAll: false });
  });

  it("marks isAll true when the default window already covers every generation", () => {
    const range = computeDefaultRange({ min: 1, max: 3 });
    expect(range).toEqual({ minGeneration: 1, maxGeneration: 3, isAll: true });
  });
});

describe("filterByWindow", () => {
  const people = [
    person({ id: 1, generationNumber: 1 }),
    person({ id: 2, generationNumber: 2 }),
    person({ id: 3, generationNumber: 3 }),
  ];

  it("returns only people within the range, inclusive", () => {
    const result = filterByWindow(people, { minGeneration: 2, maxGeneration: 3, isAll: false });
    expect(result.map((p) => p.id)).toEqual([2, 3]);
  });

  it("returns everyone when isAll is true, ignoring the range bounds", () => {
    const result = filterByWindow(people, { minGeneration: 1, maxGeneration: 1, isAll: true });
    expect(result).toHaveLength(3);
  });

  it("excludes people with no generation number even inside a wide range", () => {
    const withUnknown = [...people, person({ id: 4, generationNumber: null })];
    const result = filterByWindow(withUnknown, { minGeneration: 0, maxGeneration: 10, isAll: false });
    expect(result.map((p) => p.id)).toEqual([1, 2, 3]);
  });
});

describe("expandEarlier / expandLater", () => {
  const bounds = { min: 1, max: 10 };

  it("expandEarlier decrements minGeneration by 2, clamped to the overall minimum", () => {
    expect(expandEarlier({ minGeneration: 5, maxGeneration: 6, isAll: false }, bounds).minGeneration).toBe(3);
    expect(expandEarlier({ minGeneration: 2, maxGeneration: 6, isAll: false }, bounds).minGeneration).toBe(1);
  });

  it("expandLater increments maxGeneration by 2, clamped to the overall maximum", () => {
    expect(expandLater({ minGeneration: 5, maxGeneration: 6, isAll: false }, bounds).maxGeneration).toBe(8);
    expect(expandLater({ minGeneration: 5, maxGeneration: 9, isAll: false }, bounds).maxGeneration).toBe(10);
  });

  it("both clear isAll", () => {
    const allRange = { minGeneration: 1, maxGeneration: 10, isAll: true };
    expect(expandEarlier(allRange, bounds).isAll).toBe(false);
    expect(expandLater(allRange, bounds).isAll).toBe(false);
  });
});

describe("showAllGenerationsRange", () => {
  it("spans the full bounds and sets isAll", () => {
    expect(showAllGenerationsRange({ min: 2, max: 9 })).toEqual({ minGeneration: 2, maxGeneration: 9, isAll: true });
  });
});
