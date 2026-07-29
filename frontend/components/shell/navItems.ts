import type { LucideIcon } from "lucide-react";
import {
  FileCheck,
  GitFork,
  History,
  Layers,
  LayoutDashboard,
  Link2,
  Sprout,
  ShieldCheck,
  UserCog,
  UserPlus,
  Users,
  Workflow,
} from "lucide-react";

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

export interface AdminNavItem {
  href:
    | "/admin/signups"
    | "/admin/corrections"
    | "/admin/persons"
    | "/admin/relationships"
    | "/admin/unlinked-accounts"
    | "/admin/lineage"
    | "/admin/generations"
    | "/admin/audit-log";
  labelKey:
    | "signupReview"
    | "correctionReview"
    | "people"
    | "relationships"
    | "unlinkedAccounts"
    | "lineageBuilder"
    | "generationsView"
    | "auditLog";
  icon: LucideIcon;
  countKey?: "pendingSignupCount" | "pendingCorrectionCount";
}

// Only shown to admins (docs/08 Phase 6) -- Sidebar checks isAdmin before
// rendering this section at all.
export const ADMIN_NAV_ITEMS: AdminNavItem[] = [
  { href: "/admin/signups", labelKey: "signupReview", icon: ShieldCheck, countKey: "pendingSignupCount" },
  { href: "/admin/corrections", labelKey: "correctionReview", icon: FileCheck, countKey: "pendingCorrectionCount" },
  { href: "/admin/unlinked-accounts", labelKey: "unlinkedAccounts", icon: UserPlus },
  { href: "/admin/persons", labelKey: "people", icon: UserCog },
  { href: "/admin/relationships", labelKey: "relationships", icon: Link2 },
  { href: "/admin/lineage", labelKey: "lineageBuilder", icon: Sprout },
  { href: "/admin/generations", labelKey: "generationsView", icon: Layers },
  { href: "/admin/audit-log", labelKey: "auditLog", icon: History },
];
