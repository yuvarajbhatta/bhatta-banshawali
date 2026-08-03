"use client";

import { useRef, useState } from "react";
import { useTranslations } from "next-intl";
import { Plus } from "lucide-react";
import { Button } from "@/components/Button";
import {
  announcementPhotoFileUrl,
  deleteAnnouncementPhoto,
  uploadAnnouncementPhoto,
  AdminActionError,
  type AnnouncementPhotoDto,
} from "@/lib/api";
import styles from "./AnnouncementPhotoManager.module.css";

interface AnnouncementPhotoManagerProps {
  postId: number;
  photos: AnnouncementPhotoDto[];
  onPhotosChange: (photos: AnnouncementPhotoDto[]) => void;
}

export function AnnouncementPhotoManager({ postId, photos, onPhotosChange }: AnnouncementPhotoManagerProps) {
  const t = useTranslations("adminNewsPage.photos");
  const [caption, setCaption] = useState("");
  const [file, setFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  async function handleUpload() {
    if (!file) return;
    setUploading(true);
    setError(null);
    try {
      const photo = await uploadAnnouncementPhoto(postId, file, caption);
      onPhotosChange([photo, ...photos]);
      setCaption("");
      setFile(null);
      if (fileInputRef.current) {
        fileInputRef.current.value = "";
      }
    } catch (err) {
      setError(err instanceof AdminActionError ? err.message : t("uploadError"));
    } finally {
      setUploading(false);
    }
  }

  async function handleDelete(photoId: number) {
    setDeletingId(photoId);
    try {
      await deleteAnnouncementPhoto(postId, photoId);
      onPhotosChange(photos.filter((photo) => photo.id !== photoId));
    } catch {
      // Best-effort UI action, same as PhotoGallery -- a failed delete
      // just leaves the photo where it was.
    } finally {
      setDeletingId(null);
    }
  }

  return (
    <div className={styles.wrapper}>
      <div className={styles.grid}>
        {photos.map((photo) => (
          <figure key={photo.id} className={styles.item}>
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img
              src={announcementPhotoFileUrl(postId, photo.id)}
              alt={photo.caption ?? ""}
              className={styles.thumb}
            />
            <button
              type="button"
              className={styles.deleteButton}
              onClick={() => handleDelete(photo.id)}
              disabled={deletingId === photo.id}
              aria-label={t("delete")}
            >
              &times;
            </button>
          </figure>
        ))}
      </div>

      <div className={styles.uploadRow}>
        <input
          ref={fileInputRef}
          type="file"
          accept="image/*"
          onChange={(event) => setFile(event.target.files?.[0] ?? null)}
        />
        <input
          type="text"
          value={caption}
          onChange={(event) => setCaption(event.target.value)}
          placeholder={t("captionPlaceholder")}
          maxLength={500}
        />
        <Button variant="secondary" size="sm" onClick={handleUpload} disabled={!file || uploading}>
          <Plus size={14} aria-hidden="true" /> {uploading ? t("uploading") : t("addPhoto")}
        </Button>
      </div>
      {error ? <span className={styles.error}>{error}</span> : null}
    </div>
  );
}
