import type { LucideIcon } from "lucide-react";
import { GitFork, LayoutDashboard, Users } from "lucide-react";

export interface AppNavItem {
  href: "/dashboard" | "/tree" | "/directory";
  labelKey: "dashboard" | "familyTree" | "members";
  icon: LucideIcon;
}

// The three authenticated sections that actually exist (docs/08 Phase 4
// + this delivery's Phase 5 tree) -- not the full brief's Photos/Events/
// Reports/etc. sidebar, which would be dead links (see
// docs/frontend-redesign-plan.md).
export const APP_NAV_ITEMS: AppNavItem[] = [
  { href: "/dashboard", labelKey: "dashboard", icon: LayoutDashboard },
  { href: "/tree", labelKey: "familyTree", icon: GitFork },
  { href: "/directory", labelKey: "members", icon: Users },
];
