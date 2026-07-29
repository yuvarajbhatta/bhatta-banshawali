"use client";

import { useEffect, useRef } from "react";
import { useTranslations } from "next-intl";
import { X } from "lucide-react";
import { Link } from "@/i18n/navigation";
import type { PersonTreeNodeDto } from "@/lib/api";
import styles from "./MemberQuickView.module.css";

interface MemberQuickViewProps {
  person: PersonTreeNodeDto;
  peopleById: Map<number, PersonTreeNodeDto>;
  onClose: () => void;
  onFocusPerson: (personId: number) => void;
}

export function MemberQuickView({ person, peopleById, onClose, onFocusPerson }: MemberQuickViewProps) {
  const t = useTranslations("treePage.quickView");
  const drawerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    drawerRef.current?.querySelector<HTMLElement>("button, a")?.focus();

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        onClose();
      }
    }
    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [onClose, person.id]);

  const name = person.englishFullName.trim() || t("focusPerson");
  const initials = name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join("") || "?";

  const isDeceased = Boolean(person.deathDate);
  const birthYear = person.birthDate ? new Date(person.birthDate).getFullYear() : null;
  const deathYear = person.deathDate ? new Date(person.deathDate).getFullYear() : null;

  const father = person.fatherId ? peopleById.get(person.fatherId) : null;
  const mother = person.motherId ? peopleById.get(person.motherId) : null;
  const spouses = person.spouseIds.map((id) => peopleById.get(id)).filter((p): p is PersonTreeNodeDto => Boolean(p));
  const children = person.childIds.map((id) => peopleById.get(id)).filter((p): p is PersonTreeNodeDto => Boolean(p));

  function relatedButton(related: PersonTreeNodeDto) {
    return (
      <button key={related.id} type="button" className={styles.personLink} onClick={() => onFocusPerson(related.id)}>
        {related.englishFullName}
      </button>
    );
  }

  return (
    <>
      <div className={styles.backdrop} onClick={onClose} />
      <div className={styles.drawer} ref={drawerRef} role="dialog" aria-modal="true" aria-label={t("title")}>
        <div className={styles.header}>
          <span className={styles.avatar} aria-hidden="true">
            {initials}
          </span>
          <div className={styles.identity}>
            <p className={styles.name}>{name}</p>
            {person.nepaliFullName ? <p className={styles.nepaliName}>{person.nepaliFullName}</p> : null}
            {person.generationNumber != null ? (
              <span className={styles.badge}>{t("generation", { number: person.generationNumber })}</span>
            ) : null}
          </div>
          <button type="button" className={styles.closeButton} onClick={onClose} aria-label={t("close")}>
            <X size={18} aria-hidden="true" />
          </button>
        </div>

        <div className={styles.body}>
          <div className={styles.section}>
            <h3>{t("title")}</h3>
            <p>
              {birthYear ? t("born", { year: birthYear }) : isDeceased ? null : t("living")}
              {isDeceased ? (birthYear ? " · " : "") + t("died", { year: deathYear ?? "—" }) : null}
            </p>
          </div>

          <div className={styles.section}>
            <h3>{t("father")}</h3>
            <p>{father ? relatedButton(father) : t("none")}</p>
          </div>

          <div className={styles.section}>
            <h3>{t("mother")}</h3>
            <p>{mother ? relatedButton(mother) : t("none")}</p>
          </div>

          <div className={styles.section}>
            <h3>{t("spouses")}</h3>
            <p>{spouses.length > 0 ? spouses.map((spouse, index) => <span key={spouse.id}>{index > 0 ? ", " : ""}{relatedButton(spouse)}</span>) : t("none")}</p>
          </div>

          <div className={styles.section}>
            <h3>{t("children")}</h3>
            <p>{children.length > 0 ? children.map((child, index) => <span key={child.id}>{index > 0 ? ", " : ""}{relatedButton(child)}</span>) : t("none")}</p>
          </div>
        </div>

        <div className={styles.footer}>
          <Link href={`/directory/${person.id}`} className={styles.personLink}>
            {t("viewFullProfile")}
          </Link>
        </div>
      </div>
    </>
  );
}
