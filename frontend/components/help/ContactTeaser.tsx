"use client";

import { ArrowDown } from "lucide-react";
import styles from "./ContactTeaser.module.css";

// Not an accordion like the other Help & Contact sections -- clicking it
// jumps to the real Contact section at the bottom of the page instead of
// expanding in place.
export function ContactTeaser({ label }: { label: string }) {
  function handleClick() {
    document.getElementById("contact-section")?.scrollIntoView({ behavior: "smooth", block: "start" });
  }

  return (
    <button type="button" className={styles.teaser} onClick={handleClick}>
      <span>{label}</span>
      <ArrowDown size={16} aria-hidden="true" />
    </button>
  );
}
