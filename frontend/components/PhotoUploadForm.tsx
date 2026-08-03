"use client";

import { useRef, useState, type FormEvent } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/Button";
import { PhotoUploadError, uploadPersonPhoto, type PersonPhotoDto } from "@/lib/api";
import styles from "./PhotoUploadForm.module.css";

interface PhotoUploadFormProps {
  personId: number;
  onUploaded: (photo: PersonPhotoDto) => void;
  onClose: () => void;
}

// Open/closed is owned by the parent (PhotoAlbumSection) now, triggered
// by PhotoGallery's add-tile -- this component is just the form itself.
//
// No client-side eligibility check for who can upload a photo of whom --
// same choice CorrectionForm already made for corrections: always show
// the trigger, let the server enforce (self/immediate family, or admin)
// and surface a clear error if it rejects. Simpler than replicating the
// backend's relationship walk here just to decide whether to show it.
export function PhotoUploadForm({ personId, onUploaded, onClose }: PhotoUploadFormProps) {
  const t = useTranslations("personDetailPage.photos");
  const [caption, setCaption] = useState("");
  const [file, setFile] = useState<File | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (!file) {
      return;
    }
    setError(null);
    setSubmitting(true);
    try {
      const photo = await uploadPersonPhoto(personId, file, caption);
      onUploaded(photo);
      setCaption("");
      setFile(null);
      if (fileInputRef.current) {
        fileInputRef.current.value = "";
      }
      onClose();
    } catch (err) {
      setError(err instanceof PhotoUploadError ? err.message : t("errorGeneric"));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form className={styles.form} onSubmit={handleSubmit}>
      {error ? <div className={styles.error}>{error}</div> : null}

      <label className={styles.label}>
        {t("fileLabel")}
        <input
          ref={fileInputRef}
          type="file"
          accept="image/*"
          onChange={(event) => setFile(event.target.files?.[0] ?? null)}
          required
        />
      </label>

      <label className={styles.label}>
        {t("captionLabel")}
        <input
          type="text"
          value={caption}
          onChange={(event) => setCaption(event.target.value)}
          placeholder={t("captionPlaceholder")}
          maxLength={500}
        />
      </label>

      <div className={styles.actions}>
        <Button type="submit" variant="primary" size="sm" disabled={submitting || !file}>
          {submitting ? t("uploading") : t("upload")}
        </Button>
        <button type="button" className={styles.cancel} onClick={onClose}>
          {t("cancel")}
        </button>
      </div>
    </form>
  );
}
