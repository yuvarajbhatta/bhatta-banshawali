"use client";

import { useEffect, useRef, useState } from "react";
import { useTranslations } from "next-intl";
import { X } from "lucide-react";
import { ReactFlowProvider } from "@xyflow/react";
import type { PersonTreeNodeDto } from "@/lib/api";
import { TreeCanvas } from "./TreeCanvas";
import { MemberQuickView } from "./MemberQuickView";
import { EMPTY_HIGHLIGHTED_PATH, type HighlightedPath } from "./treeHighlight";
import styles from "./TreeFullscreenView.module.css";

interface TreeFullscreenViewProps {
  people: PersonTreeNodeDto[];
  peopleById: Map<number, PersonTreeNodeDto>;
  /** Root -> viewer's own person, in red -- same always-on highlight as the compact view. */
  rootPath?: HighlightedPath;
  onClose: () => void;
}

export function TreeFullscreenView({ people, peopleById, rootPath = EMPTY_HIGHLIGHTED_PATH, onClose }: TreeFullscreenViewProps) {
  const t = useTranslations("treePage.fullscreen");
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const overlayRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    overlayRef.current?.querySelector<HTMLElement>("button, a")?.focus();

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        onClose();
      }
    }
    document.addEventListener("keydown", handleKeyDown);

    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";

    return () => {
      document.removeEventListener("keydown", handleKeyDown);
      document.body.style.overflow = previousOverflow;
    };
  }, [onClose]);

  const selectedPerson = selectedId != null ? peopleById.get(selectedId) ?? null : null;

  return (
    <div className={styles.overlay} ref={overlayRef} role="dialog" aria-modal="true" aria-label={t("title")}>
      <div className={styles.header}>
        <span className={styles.title}>{t("title")}</span>
        <button type="button" className={styles.closeButton} onClick={onClose} aria-label={t("close")}>
          <X size={20} aria-hidden="true" />
        </button>
      </div>
      <div className={styles.canvasArea}>
        <ReactFlowProvider>
          <TreeCanvas
            people={people}
            selectedId={selectedId}
            focusId={null}
            onSelect={setSelectedId}
            highlights={{ rootPath, selectedPath: EMPTY_HIGHLIGHTED_PATH }}
          />
        </ReactFlowProvider>
      </div>
      {selectedPerson ? (
        <MemberQuickView
          person={selectedPerson}
          peopleById={peopleById}
          onClose={() => setSelectedId(null)}
          onFocusPerson={setSelectedId}
        />
      ) : null}
    </div>
  );
}
