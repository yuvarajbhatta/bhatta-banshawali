import { useLocale, useTranslations } from "next-intl";
import { Pin } from "lucide-react";
import type { AnnouncementDto } from "@/lib/api";
import { localizeAnnouncement } from "@/lib/localize-announcement";
import { Paragraphs } from "@/components/Paragraphs";
import { CATEGORY_ICONS } from "./categoryMeta";
import { AnnouncementPhotoStrip } from "./AnnouncementPhotoStrip";
import styles from "./AnnouncementCard.module.css";

export function AnnouncementCard({ announcement }: { announcement: AnnouncementDto }) {
  const locale = useLocale();
  const t = useTranslations("newsPage");
  const { title, body } = localizeAnnouncement(announcement, locale);
  const Icon = CATEGORY_ICONS[announcement.category];

  return (
    <article className={styles.card}>
      <div className={styles.header}>
        <span className={styles.categoryBadge}>
          <Icon size={14} aria-hidden="true" />
          {t(`categories.${announcement.category}`)}
        </span>
        {announcement.pinned ? (
          <span className={styles.pinned} title={t("pinned")}>
            <Pin size={14} aria-hidden="true" />
          </span>
        ) : null}
      </div>
      <h2 className={styles.title}>{title}</h2>
      <p className={styles.date}>{formatDate(announcement.publishedAt, locale)}</p>
      <div className={styles.body}>
        <Paragraphs text={body} />
      </div>
      {announcement.photos.length > 0 ? (
        <AnnouncementPhotoStrip postId={announcement.id} photos={announcement.photos} />
      ) : null}
    </article>
  );
}

function formatDate(iso: string, locale: string): string {
  return new Date(iso).toLocaleDateString(locale === "ne" ? "ne" : "en-US", {
    year: "numeric",
    month: "long",
    day: "numeric",
  });
}
