"use client";

import { useState } from "react";
import { Plus } from "lucide-react";
import { useTranslations } from "next-intl";
import { deletePersonPhoto, personPhotoFileUrl, type PersonPhotoDto } from "@/lib/api";
import styles from "./PhotoGallery.module.css";

interface PhotoGalleryProps {
  personId: number;
  photos: PersonPhotoDto[];
  onDeleted: (photoId: number) => void;
  onAddClick: () => void;
}

export function PhotoGallery({ personId, photos, onDeleted, onAddClick }: PhotoGalleryProps) {
  const t = useTranslations("personDetailPage.photos");
  const [enlarged, setEnlarged] = useState<PersonPhotoDto | null>(null);
  const [deletingId, setDeletingId] = useState<number | null>(null);

  async function handleDelete(photoId: number) {
    if (!window.confirm(t("confirmDelete"))) {
      return;
    }
    setDeletingId(photoId);
    try {
      await deletePersonPhoto(personId, photoId);
      onDeleted(photoId);
      setEnlarged((current) => (current?.id === photoId ? null : current));
    } catch {
      // Best-effort UI action -- a failed delete just leaves the photo
      // where it was, nothing further to reconcile client-side.
    } finally {
      setDeletingId(null);
    }
  }

  return (
    <>
      <div className={styles.grid}>
        <button type="button" className={styles.addTile} onClick={onAddClick} aria-label={t("addPhoto")}>
          <Plus size={22} aria-hidden="true" />
        </button>

        {photos.map((photo) => (
          <figure key={photo.id} className={styles.item}>
            <button type="button" className={styles.thumbButton} onClick={() => setEnlarged(photo)}>
              {/* Authenticated, session-cookie-gated endpoint -- not eligible for next/image's remote loader/optimizer. */}
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img
                src={personPhotoFileUrl(personId, photo.id)}
                alt={photo.caption ?? ""}
                className={styles.thumb}
                loading="lazy"
              />
            </button>
            {photo.canDelete ? (
              <button
                type="button"
                className={styles.deleteButton}
                onClick={() => handleDelete(photo.id)}
                disabled={deletingId === photo.id}
                aria-label={t("delete")}
              >
                &times;
              </button>
            ) : null}
          </figure>
        ))}
      </div>

      {enlarged ? (
        <div className={styles.lightbox} onClick={() => setEnlarged(null)}>
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img
            src={personPhotoFileUrl(personId, enlarged.id)}
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
