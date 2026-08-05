import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { ReactFlowProvider, type NodeProps, type Node } from "@xyflow/react";
import { MemberNode } from "./MemberNode";
import type { MemberNodeData } from "./familyTree.types";
import type { PersonTreeNodeDto } from "@/lib/api";

vi.mock("next-intl", () => ({
  useTranslations: () => (key: string) => key,
}));

const PERSON: PersonTreeNodeDto = {
  id: 1,
  englishFullName: "Ram Bhatta",
  nepaliFullName: "राम भट्ट",
  gender: "MALE",
  generationNumber: 2,
  birthDate: "1950-01-01",
  deathDate: "2020-01-01",
  fatherId: null,
  motherId: null,
  spouseIds: [],
  childIds: [],
};

type MemberNodeType = Node<MemberNodeData, "member">;

function renderNode(data: MemberNodeData) {
  const props = {
    id: "1",
    type: "member",
    data,
    selected: false,
    dragging: false,
    isConnectable: true,
    zIndex: 0,
    xPos: 0,
    yPos: 0,
    dragHandle: undefined,
    positionAbsoluteX: 0,
    positionAbsoluteY: 0,
  } as unknown as NodeProps<MemberNodeType>;

  return render(
    <ReactFlowProvider>
      <MemberNode {...props} />
    </ReactFlowProvider>,
  );
}

describe("MemberNode", () => {
  it("renders only the person's name, no birth/death years", () => {
    renderNode({ person: PERSON, selected: false, highlighted: false });

    expect(screen.getByText("Ram Bhatta")).toBeInTheDocument();
    expect(screen.queryByText(/1950/)).not.toBeInTheDocument();
    expect(screen.queryByText(/2020/)).not.toBeInTheDocument();
  });

  it("uses the name alone as the accessible label", () => {
    renderNode({ person: PERSON, selected: false, highlighted: false });

    expect(screen.getByRole("button", { name: "Ram Bhatta" })).toBeInTheDocument();
  });
});
