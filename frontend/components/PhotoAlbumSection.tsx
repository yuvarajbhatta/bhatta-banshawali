"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { PhotoGallery } from "./PhotoGallery";
import { PhotoUploadForm } from "./PhotoUploadForm";
import type { PersonPhotoDto } from "@/lib/api";
import styles from "./PhotoAlbumSection.module.css";

interface PhotoAlbumSectionProps {
  personId: number;
  initialPhotos: PersonPhotoDto[];
}

// Holds the photo list as client state so upload/delete update the
// gallery in place -- PhotoGallery and PhotoUploadForm below are purely
// presentational, this is the only piece that knows about both.
export function PhotoAlbumSection({ personId, initialPhotos }: PhotoAlbumSectionProps) {
  const t = useTranslations("personDetailPage.photos");
  const [photos, setPhotos] = useState(initialPhotos);

  return (
    <div>
      <h3 className={styles.title}>{t("title")}</h3>
      <div className={styles.uploadArea}>
        <PhotoUploadForm personId={personId} onUploaded={(photo) => setPhotos((prev) => [photo, ...prev])} />
      </div>
      <PhotoGallery
        personId={personId}
        photos={photos}
        onDeleted={(photoId) => setPhotos((prev) => prev.filter((photo) => photo.id !== photoId))}
      />
    </div>
  );
}
