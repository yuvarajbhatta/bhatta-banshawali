import type { AnnouncementDto } from "./api";

// Same rule as localizeArticle: Nepali only when the viewer is on the
// Nepali locale AND a translation actually exists, English otherwise.
export function localizeAnnouncement(announcement: AnnouncementDto, locale: string): { title: string; body: string } {
  if (locale === "ne" && announcement.titleNe && announcement.bodyNe) {
    return { title: announcement.titleNe, body: announcement.bodyNe };
  }
  return { title: announcement.titleEn, body: announcement.bodyEn };
}
