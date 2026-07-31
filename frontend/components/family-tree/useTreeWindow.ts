import { useMemo, useState } from "react";
import type { PersonTreeNodeDto } from "@/lib/api";

// Phase 5 performance-benchmark follow-up (docs/08): /tree fetches the
// whole graph once (cheap -- ~200ms/417KB even at 2,000 people, per this
// session's benchmark), but handing all of it to Dagre/React Flow at once
// is what's slow. This hook bounds what actually reaches the layout/render
// pipeline to a generation window, purely by slicing the already-fetched
// array -- no extra network round-trip, since the data's already local.
//
// Split into pure functions + a thin useState/useMemo wrapper, same
// pattern as layoutFamilyTree/useFamilyTreeLayout, so the logic is
// directly unit-testable without a React rendering harness.
const DEFAULT_WINDOW_SIZE = 3; // generations beyond the minimum, inclusive
const EXPAND_STEP = 2;

/** Above this many people, switching to "all generations" requires an explicit confirm step. */
export const ALL_GENERATIONS_CONFIRM_THRESHOLD = 500;

export interface RangeState {
  minGeneration: number;
  maxGeneration: number;
  isAll: boolean;
}

export interface GenerationBounds {
  min: number;
  max: number;
}

export function computeGenerationBounds(people: PersonTreeNodeDto[]): GenerationBounds {
  const generationNumbers = people
    .map((person) => person.generationNumber)
    .filter((generation): generation is number => generation != null);
  if (generationNumbers.length === 0) {
    return { min: 0, max: 0 };
  }
  return { min: Math.min(...generationNumbers), max: Math.max(...generationNumbers) };
}

export function computeDefaultRange(bounds: GenerationBounds): RangeState {
  const defaultMax = Math.min(bounds.min + DEFAULT_WINDOW_SIZE, bounds.max);
  return {
    minGeneration: bounds.min,
    maxGeneration: defaultMax,
    // A family small enough that the default window already covers every
    // generation behaves exactly as "all generations" from the start --
    // no reason to make a small family jump through the windowing UI.
    isAll: defaultMax >= bounds.max,
  };
}

export function filterByWindow(people: PersonTreeNodeDto[], range: RangeState): PersonTreeNodeDto[] {
  if (range.isAll) {
    return people;
  }
  return people.filter(
    (person) =>
      person.generationNumber != null &&
      person.generationNumber >= range.minGeneration &&
      person.generationNumber <= range.maxGeneration,
  );
}

export function expandEarlier(range: RangeState, bounds: GenerationBounds): RangeState {
  return { ...range, minGeneration: Math.max(bounds.min, range.minGeneration - EXPAND_STEP), isAll: false };
}

export function expandLater(range: RangeState, bounds: GenerationBounds): RangeState {
  return { ...range, maxGeneration: Math.min(bounds.max, range.maxGeneration + EXPAND_STEP), isAll: false };
}

export function showAllGenerationsRange(bounds: GenerationBounds): RangeState {
  return { minGeneration: bounds.min, maxGeneration: bounds.max, isAll: true };
}

export interface UseTreeWindowResult {
  windowedPeople: PersonTreeNodeDto[];
  minGeneration: number;
  maxGeneration: number;
  overallMinGeneration: number;
  overallMaxGeneration: number;
  isAllGenerations: boolean;
  totalPeopleCount: number;
  setRange: (minGeneration: number, maxGeneration: number) => void;
  loadEarlier: () => void;
  loadLater: () => void;
  showAllGenerations: () => void;
  resetToDefaultWindow: () => void;
}

export function useTreeWindow(people: PersonTreeNodeDto[]): UseTreeWindowResult {
  const bounds = useMemo(() => computeGenerationBounds(people), [people]);
  const [range, setRange] = useState<RangeState>(() => computeDefaultRange(bounds));

  const windowedPeople = useMemo(() => filterByWindow(people, range), [people, range]);

  return {
    windowedPeople,
    minGeneration: range.minGeneration,
    maxGeneration: range.maxGeneration,
    overallMinGeneration: bounds.min,
    overallMaxGeneration: bounds.max,
    isAllGenerations: range.isAll,
    totalPeopleCount: people.length,
    setRange: (minGeneration, maxGeneration) => setRange({ minGeneration, maxGeneration, isAll: false }),
    loadEarlier: () => setRange((current) => expandEarlier(current, bounds)),
    loadLater: () => setRange((current) => expandLater(current, bounds)),
    showAllGenerations: () => setRange(showAllGenerationsRange(bounds)),
    resetToDefaultWindow: () => setRange(computeDefaultRange(bounds)),
  };
}
