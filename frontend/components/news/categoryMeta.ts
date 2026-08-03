import { Heart, Megaphone, PartyPopper, Sparkles, HeartHandshake, type LucideIcon } from "lucide-react";
import type { AnnouncementCategory } from "@/lib/api";

export const CATEGORY_ICONS: Record<AnnouncementCategory, LucideIcon> = {
  APP_UPDATE: Sparkles,
  FAMILY_NEWS: Megaphone,
  CELEBRATION: PartyPopper,
  OBITUARY: Heart,
  HELP_REQUEST: HeartHandshake,
};
