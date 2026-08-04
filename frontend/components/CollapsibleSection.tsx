"use client";

import { useState, type ReactNode } from "react";
import { ChevronDown } from "lucide-react";
import styles from "./CollapsibleSection.module.css";

interface CollapsibleSectionProps {
  title: string;
  defaultOpen?: boolean;
  children: ReactNode;
}

export function CollapsibleSection({ title, defaultOpen = false, children }: CollapsibleSectionProps) {
  const [open, setOpen] = useState(defaultOpen);

  return (
    <section className={styles.card}>
      <button type="button" className={styles.header} onClick={() => setOpen((current) => !current)} aria-expanded={open}>
        <h2 className={styles.title}>{title}</h2>
        <ChevronDown size={18} className={open ? styles.chevronOpen : styles.chevron} aria-hidden="true" />
      </button>
      {open ? <div className={styles.content}>{children}</div> : null}
    </section>
  );
}
