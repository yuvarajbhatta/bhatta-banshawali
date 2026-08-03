"use client";

import { useEffect } from "react";
import { useTranslations } from "next-intl";
import { markAnnouncementsSeen, type AnnouncementDto } from "@/lib/api";
import { AnnouncementCard } from "./AnnouncementCard";
import styles from "./NewsFeed.module.css";

interface NewsFeedProps {
  announcements: AnnouncementDto[];
}

export function NewsFeed({ announcements }: NewsFeedProps) {
  const t = useTranslations("newsPage");

  // Fire-and-forget: clears the sidebar's unread badge for next page load.
  // Not awaited/surfaced to the user -- a failed mark-seen just leaves the
  // badge as it was, nothing to recover from here.
  useEffect(() => {
    markAnnouncementsSeen().catch(() => {});
  }, []);

  if (announcements.length === 0) {
    return <p className={styles.empty}>{t("empty")}</p>;
  }

  return (
    <div className={styles.feed}>
      {announcements.map((announcement) => (
        <AnnouncementCard key={announcement.id} announcement={announcement} />
      ))}
    </div>
  );
}
