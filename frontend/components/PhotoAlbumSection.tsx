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

// Holds the photo list and the upload form's open/closed state as client
// state -- PhotoGallery (grid + its leading add-tile) and PhotoUploadForm
// below are purely presentational, this is the only piece that knows
// about both.
export function PhotoAlbumSection({ personId, initialPhotos }: PhotoAlbumSectionProps) {
  const t = useTranslations("personDetailPage.photos");
  const [photos, setPhotos] = useState(initialPhotos);
  const [uploadOpen, setUploadOpen] = useState(false);

  return (
    <div>
      <h3 className={styles.title}>{t("title")}</h3>
      {uploadOpen ? (
        <div className={styles.uploadArea}>
          <PhotoUploadForm
            personId={personId}
            onUploaded={(photo) => setPhotos((prev) => [photo, ...prev])}
            onClose={() => setUploadOpen(false)}
          />
        </div>
      ) : null}
      <PhotoGallery
        personId={personId}
        photos={photos}
        onAddClick={() => setUploadOpen(true)}
        onDeleted={(photoId) => setPhotos((prev) => prev.filter((photo) => photo.id !== photoId))}
      />
    </div>
  );
}
