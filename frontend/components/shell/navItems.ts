import type { LucideIcon } from "lucide-react";
import { GitFork, LayoutDashboard, Users, Workflow } from "lucide-react";

export interface AppNavItem {
  href: "/dashboard" | "/family" | "/tree" | "/directory";
  labelKey: "dashboard" | "yourFamily" | "familyTree" | "members";
  icon: LucideIcon;
}

// The four authenticated sections that actually exist (docs/08 Phases 4-5)
// -- not the full brief's Photos/Events/Reports/etc. sidebar, which would
// be dead links (see docs/frontend-redesign-plan.md).
export const APP_NAV_ITEMS: AppNavItem[] = [
  { href: "/dashboard", labelKey: "dashboard", icon: LayoutDashboard },
  { href: "/family", labelKey: "yourFamily", icon: Workflow },
  { href: "/tree", labelKey: "familyTree", icon: GitFork },
  { href: "/directory", labelKey: "members", icon: Users },
];
