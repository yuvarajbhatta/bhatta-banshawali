import { describe, expect, it } from "vitest";
import type { PersonTreeNodeDto } from "@/lib/api";
import { layoutFamilyTree, NODE_HEIGHT, NODE_WIDTH } from "./useFamilyTreeLayout";
import { EMPTY_HIGHLIGHTED_PATH, type TreeHighlights } from "./treeHighlight";

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

function highlights(overrides: Partial<TreeHighlights>): TreeHighlights {
  return { rootPath: EMPTY_HIGHLIGHTED_PATH, selectedPath: EMPTY_HIGHLIGHTED_PATH, ...overrides };
}

describe("layoutFamilyTree", () => {
  it("positions every person exactly once, sized to the node dimensions", () => {
    const people = [person({ id: 1, childIds: [2] }), person({ id: 2, fatherId: 1 })];
    const { nodes } = layoutFamilyTree(people, null);

    expect(nodes).toHaveLength(2);
    expect(new Set(nodes.map((n) => n.id))).toEqual(new Set(["1", "2"]));
    for (const node of nodes) {
      expect(node.type).toBe("member");
      expect(node.width ?? NODE_WIDTH).toBeGreaterThan(0);
      expect(node.height ?? NODE_HEIGHT).toBeGreaterThan(0);
    }
  });

  it("places children below their parent (Dagre top-to-bottom rank)", () => {
    const parent = person({ id: 1, childIds: [2] });
    const child = person({ id: 2, fatherId: 1 });
    const { nodes } = layoutFamilyTree([parent, child], null);

    const parentNode = nodes.find((n) => n.id === "1")!;
    const childNode = nodes.find((n) => n.id === "2")!;
    expect(childNode.position.y).toBeGreaterThan(parentNode.position.y);
  });

  it("draws a parent-child edge for every present child reference", () => {
    const parent = person({ id: 1, childIds: [2, 3] });
    const childA = person({ id: 2, fatherId: 1 });
    const childB = person({ id: 3, fatherId: 1 });
    const { edges } = layoutFamilyTree([parent, childA, childB], null);

    const parentChildEdges = edges.filter((e) => e.id.startsWith("pc-"));
    expect(parentChildEdges).toHaveLength(2);
    expect(parentChildEdges.map((e) => e.target).sort()).toEqual(["2", "3"]);
  });

  it("never draws an edge to a person outside the filtered list (no dangling edges)", () => {
    // Person 1's childIds references person 2, but 2 isn't in the input
    // (e.g. filtered out by generation/search) -- must not appear as an edge.
    const parent = person({ id: 1, childIds: [2] });
    const { edges, nodes } = layoutFamilyTree([parent], null);

    expect(nodes).toHaveLength(1);
    expect(edges).toHaveLength(0);
  });

  it("never draws a spouse/marriage line, even when spouseIds are populated", () => {
    const a = person({ id: 1, spouseIds: [2], childIds: [3] });
    const b = person({ id: 2, spouseIds: [1], childIds: [3] });
    const c = person({ id: 3, fatherId: 1, motherId: 2 });
    const { edges } = layoutFamilyTree([a, b, c], null);

    expect(edges.some((e) => e.id.startsWith("sp-"))).toBe(false);
  });

  it("drops a person who has no lineage role of their own (only ever a spouse) from the rendered tree", () => {
    // b has no father/mother and no children -- their only connection to
    // a is the marriage, which no longer gets a line, so they'd otherwise
    // render as a disconnected floating card with no edges at all.
    const a = person({ id: 1, spouseIds: [2], childIds: [] });
    const b = person({ id: 2, spouseIds: [1] });
    const { nodes } = layoutFamilyTree([a, b], null);

    expect(nodes.map((n) => n.id)).toEqual([]);
  });

  it("keeps a spouse who also has their own parent/child role", () => {
    // b is a's spouse, but also c's mother -- that parent-child edge is
    // reason enough to keep her, independent of the marriage.
    const a = person({ id: 1, spouseIds: [2], childIds: [3] });
    const b = person({ id: 2, spouseIds: [1], childIds: [3] });
    const c = person({ id: 3, fatherId: 1, motherId: 2 });
    const { nodes } = layoutFamilyTree([a, b, c], null);

    expect(new Set(nodes.map((n) => n.id))).toEqual(new Set(["1", "2", "3"]));
  });

  it("marks only the selected person's node as selected", () => {
    const people = [person({ id: 1, childIds: [2] }), person({ id: 2, fatherId: 1 })];
    const { nodes } = layoutFamilyTree(people, 2);

    expect(nodes.find((n) => n.id === "1")?.data.selected).toBe(false);
    expect(nodes.find((n) => n.id === "2")?.data.selected).toBe(true);
  });

  it("handles an empty list without throwing", () => {
    const { nodes, edges } = layoutFamilyTree([], null);
    expect(nodes).toEqual([]);
    expect(edges).toEqual([]);
  });

  it("defaults every node to no path highlight when none is passed", () => {
    const people = [person({ id: 1, childIds: [2] }), person({ id: 2, fatherId: 1 })];
    const { nodes } = layoutFamilyTree(people, null);

    expect(nodes.every((n) => n.data.pathHighlight === null)).toBe(true);
  });

  it("marks nodes and edges on the selected-person path (amber)", () => {
    const parent = person({ id: 1, childIds: [2] });
    const child = person({ id: 2, fatherId: 1 });
    const { nodes, edges } = layoutFamilyTree(
      [parent, child],
      null,
      highlights({ selectedPath: { nodeIds: new Set([1, 2]), edgeIds: new Set(["pc-1-2"]) } }),
    );

    expect(nodes.find((n) => n.id === "1")?.data.pathHighlight).toBe("selected");
    expect(nodes.find((n) => n.id === "2")?.data.pathHighlight).toBe("selected");

    const edge = edges.find((e) => e.id === "pc-1-2");
    expect(edge?.style).toMatchObject({ stroke: "var(--color-warning)", strokeWidth: 3 });
    expect(edge?.zIndex).toBe(10);
  });

  it("marks nodes and edges on the root path (red) when there's no selected-path override", () => {
    const parent = person({ id: 1, childIds: [2] });
    const child = person({ id: 2, fatherId: 1 });
    const { nodes, edges } = layoutFamilyTree(
      [parent, child],
      null,
      highlights({ rootPath: { nodeIds: new Set([1, 2]), edgeIds: new Set(["pc-1-2"]) } }),
    );

    expect(nodes.find((n) => n.id === "1")?.data.pathHighlight).toBe("root");
    expect(nodes.find((n) => n.id === "2")?.data.pathHighlight).toBe("root");

    const edge = edges.find((e) => e.id === "pc-1-2");
    expect(edge?.style).toMatchObject({ stroke: "var(--color-error)", strokeWidth: 3 });
    expect(edge?.zIndex).toBe(10);
  });

  it("lets the selected-person path win over the root path on a shared node/edge", () => {
    const parent = person({ id: 1, childIds: [2] });
    const child = person({ id: 2, fatherId: 1 });
    const shared = { nodeIds: new Set([1, 2]), edgeIds: new Set(["pc-1-2"]) };
    const { nodes, edges } = layoutFamilyTree(
      [parent, child],
      null,
      highlights({ rootPath: shared, selectedPath: shared }),
    );

    expect(nodes.find((n) => n.id === "2")?.data.pathHighlight).toBe("selected");
    const edge = edges.find((e) => e.id === "pc-1-2");
    expect(edge?.style).toMatchObject({ stroke: "var(--color-warning)" });
  });

  it("leaves edges outside every highlight set with their normal style", () => {
    const parent = person({ id: 1, childIds: [2] });
    const child = person({ id: 2, fatherId: 1 });
    const { edges } = layoutFamilyTree([parent, child], null);

    const edge = edges.find((e) => e.id === "pc-1-2");
    expect(edge?.style).toMatchObject({ stroke: "var(--color-neutral-300)", strokeWidth: 1.5 });
    expect(edge?.zIndex).toBe(0);
  });
});
