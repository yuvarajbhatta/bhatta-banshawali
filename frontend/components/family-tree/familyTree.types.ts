import type { PersonTreeNodeDto } from "@/lib/api";

export type TreePathHighlight = "root" | "selected" | null;

export interface MemberNodeData extends Record<string, unknown> {
  person: PersonTreeNodeDto;
  selected: boolean;
  pathHighlight: TreePathHighlight;
  onSelect?: (personId: number) => void;
}
