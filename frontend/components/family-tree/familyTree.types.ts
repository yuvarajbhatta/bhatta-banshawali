import type { PersonTreeNodeDto } from "@/lib/api";

export interface MemberNodeData extends Record<string, unknown> {
  person: PersonTreeNodeDto;
  selected: boolean;
  highlighted: boolean;
  onSelect?: (personId: number) => void;
}

export type LivingFilter = "all" | "living" | "deceased";
