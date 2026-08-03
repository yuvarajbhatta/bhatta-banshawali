"use client";

import { useState } from "react";
import { announcementPhotoFileUrl, type AnnouncementPhotoDto } from "@/lib/api";
import styles from "./AnnouncementPhotoStrip.module.css";

interface AnnouncementPhotoStripProps {
  postId: number;
  photos: AnnouncementPhotoDto[];
}

// Read-only, unlike PhotoGallery (person albums) -- no add/delete tile,
// News & Alerts photos are admin-authored, not member-uploaded.
export function AnnouncementPhotoStrip({ postId, photos }: AnnouncementPhotoStripProps) {
  const [enlarged, setEnlarged] = useState<AnnouncementPhotoDto | null>(null);

  return (
    <>
      <div className={styles.strip}>
        {photos.map((photo) => (
          <button key={photo.id} type="button" className={styles.thumbButton} onClick={() => setEnlarged(photo)}>
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img
              src={announcementPhotoFileUrl(postId, photo.id)}
              alt={photo.caption ?? ""}
              className={styles.thumb}
            />
          </button>
        ))}
      </div>

      {enlarged ? (
        <div className={styles.lightbox} onClick={() => setEnlarged(null)}>
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img
            src={announcementPhotoFileUrl(postId, enlarged.id)}
            alt={enlarged.caption ?? ""}
            className={styles.lightboxImage}
            onClick={(event) => event.stopPropagation()}
          />
          {enlarged.caption ? <p className={styles.caption}>{enlarged.caption}</p> : null}
        </div>
      ) : null}
    </>
  );
}
