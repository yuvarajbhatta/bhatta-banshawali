import type { LucideIcon } from "lucide-react";
import {
  AlertTriangle,
  Bell,
  Copy,
  FileCheck,
  FileText,
  GitFork,
  History,
  Info,
  Layers,
  LayoutDashboard,
  Link2,
  Sprout,
  ShieldCheck,
  ShieldPlus,
  UserCog,
  UserPlus,
  Users,
  Workflow,
} from "lucide-react";

export interface AppNavItem {
  href: "/dashboard" | "/family" | "/tree" | "/directory" | "/news" | "/about";
  labelKey: "dashboard" | "yourFamily" | "familyTree" | "members" | "news" | "about";
  icon: LucideIcon;
  // Only "/news" carries a badge (the unread announcement count) -- a
  // single boolean flag rather than a countKey lookup table like
  // ADMIN_NAV_ITEMS below, since there's just the one case.
  showUnreadBadge?: boolean;
}

// The six authenticated sections that actually exist (docs/08 Phases 4-5)
// -- not the full brief's Photos/Events/Reports/etc. sidebar, which would
// be dead links (see docs/frontend-redesign-plan.md). "/about" is outside
// the (app) route group (it's the same public, admin-managed-content page
// linked from the pre-login site) -- listed last since it's a step outside
// the app shell's own chrome, not another app/(dashboard) feature.
export const APP_NAV_ITEMS: AppNavItem[] = [
  { href: "/dashboard", labelKey: "dashboard", icon: LayoutDashboard },
  { href: "/family", labelKey: "yourFamily", icon: Workflow },
  { href: "/tree", labelKey: "familyTree", icon: GitFork },
  { href: "/directory", labelKey: "members", icon: Users },
  { href: "/news", labelKey: "news", icon: Bell, showUnreadBadge: true },
  { href: "/about", labelKey: "about", icon: Info },
];

export interface AdminNavItem {
  href:
    | "/admin/signups"
    | "/admin/corrections"
    | "/admin/persons"
    | "/admin/relationships"
    | "/admin/accounts"
    | "/admin/admin-access-requests"
    | "/admin/content"
    | "/admin/news"
    | "/admin/lineage"
    | "/admin/generations"
    | "/admin/audit-log"
    | "/admin/duplicates"
    | "/admin/data-quality";
  labelKey:
    | "signupReview"
    | "correctionReview"
    | "people"
    | "relationships"
    | "manageUserAccounts"
    | "adminAccessRequests"
    | "content"
    | "newsManager"
    | "lineageBuilder"
    | "generationsView"
    | "auditLog"
    | "duplicatePeople"
    | "dataQualityReports";
  icon: LucideIcon;
  countKey?: "pendingSignupCount" | "pendingCorrectionCount" | "pendingAdminAccessRequestCount";
}

// Only shown to admins (docs/08 Phase 6) -- Sidebar checks isAdmin before
// rendering this section at all.
export const ADMIN_NAV_ITEMS: AdminNavItem[] = [
  { href: "/admin/signups", labelKey: "signupReview", icon: ShieldCheck, countKey: "pendingSignupCount" },
  { href: "/admin/corrections", labelKey: "correctionReview", icon: FileCheck, countKey: "pendingCorrectionCount" },
  {
    href: "/admin/admin-access-requests",
    labelKey: "adminAccessRequests",
    icon: ShieldPlus,
    countKey: "pendingAdminAccessRequestCount",
  },
  { href: "/admin/accounts", labelKey: "manageUserAccounts", icon: UserPlus },
  { href: "/admin/persons", labelKey: "people", icon: UserCog },
  { href: "/admin/relationships", labelKey: "relationships", icon: Link2 },
  // No countKey on either -- AdminSummaryDto is computed on every admin
  // page load for the sidebar, and both of these reports scan the full
  // person/relationship tables (O(n^2) for duplicates, full-graph DFS for
  // data quality). Wiring a live count would mean running that on every
  // page view instead of only when the report is actually opened.
  { href: "/admin/duplicates", labelKey: "duplicatePeople", icon: Copy },
  { href: "/admin/data-quality", labelKey: "dataQualityReports", icon: AlertTriangle },
  { href: "/admin/content", labelKey: "content", icon: FileText },
  { href: "/admin/news", labelKey: "newsManager", icon: Bell },
  { href: "/admin/lineage", labelKey: "lineageBuilder", icon: Sprout },
  { href: "/admin/generations", labelKey: "generationsView", icon: Layers },
  { href: "/admin/audit-log", labelKey: "auditLog", icon: History },
];
