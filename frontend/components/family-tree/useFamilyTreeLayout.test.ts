import { describe, expect, it } from "vitest";
import type { PersonTreeNodeDto } from "@/lib/api";
import { layoutFamilyTree, NODE_HEIGHT, NODE_WIDTH } from "./useFamilyTreeLayout";

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

describe("layoutFamilyTree", () => {
  it("positions every person exactly once, sized to the node dimensions", () => {
    const people = [person({ id: 1 }), person({ id: 2, fatherId: 1, childIds: [] })];
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

  it("draws exactly one spouse edge per pair, not two", () => {
    const a = person({ id: 1, spouseIds: [2] });
    const b = person({ id: 2, spouseIds: [1] });
    const { edges } = layoutFamilyTree([a, b], null);

    const spouseEdges = edges.filter((e) => e.id.startsWith("sp-"));
    expect(spouseEdges).toHaveLength(1);
  });

  it("marks only the selected person's node as selected", () => {
    const people = [person({ id: 1 }), person({ id: 2 })];
    const { nodes } = layoutFamilyTree(people, 2);

    expect(nodes.find((n) => n.id === "1")?.data.selected).toBe(false);
    expect(nodes.find((n) => n.id === "2")?.data.selected).toBe(true);
  });

  it("handles an empty list without throwing", () => {
    const { nodes, edges } = layoutFamilyTree([], null);
    expect(nodes).toEqual([]);
    expect(edges).toEqual([]);
  });
});
