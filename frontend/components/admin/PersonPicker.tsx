"use client";

import { useEffect, useRef, useState } from "react";
import { searchPersons, type PersonSummaryDto } from "@/lib/api";
import styles from "./PersonPicker.module.css";

interface PersonPickerProps {
  label: string;
  placeholder: string;
  clearLabel: string;
  selected: { id: number; name: string } | null;
  onChange: (person: { id: number; name: string } | null) => void;
}

// A typeahead search over the real member directory (searchPersons,
// the same endpoint /directory uses) instead of a giant <select> with
// hundreds of options -- the Thymeleaf original loaded every Person
// into one dropdown, which doesn't scale past a few hundred people.
export function PersonPicker({ label, placeholder, clearLabel, selected, onChange }: PersonPickerProps) {
  const [keyword, setKeyword] = useState("");
  const [results, setResults] = useState<PersonSummaryDto[]>([]);
  const [open, setOpen] = useState(false);
  const wrapperRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!keyword.trim()) {
      // Nothing to reset: the dropdown's own visibility check below
      // already requires a non-empty keyword, so stale results never render.
      return;
    }
    let cancelled = false;
    const timeout = setTimeout(() => {
      searchPersons(keyword.trim())
        .then((people) => {
          if (!cancelled) setResults(people.slice(0, 8));
        })
        .catch(() => {
          if (!cancelled) setResults([]);
        });
    }, 250);
    return () => {
      cancelled = true;
      clearTimeout(timeout);
    };
  }, [keyword]);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (wrapperRef.current && !wrapperRef.current.contains(event.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  if (selected) {
    return (
      <div className={styles.selected} role="group" aria-label={label}>
        <span>{selected.name}</span>
        <button type="button" className={styles.clearButton} onClick={() => onChange(null)}>
          {clearLabel}
        </button>
      </div>
    );
  }

  return (
    <div className={styles.wrapper} ref={wrapperRef}>
      <input
        type="search"
        className={styles.input}
        aria-label={label}
        placeholder={placeholder}
        value={keyword}
        onFocus={() => setOpen(true)}
        onChange={(event) => {
          setKeyword(event.target.value);
          setOpen(true);
        }}
      />
      {open && keyword.trim().length > 0 && results.length > 0 ? (
        <ul className={styles.results}>
          {results.map((person) => (
            <li key={person.id}>
              <button
                type="button"
                className={styles.resultButton}
                onClick={() => {
                  onChange({ id: person.id, name: person.englishFullName });
                  setKeyword("");
                  setOpen(false);
                }}
              >
                {person.englishFullName}
              </button>
            </li>
          ))}
        </ul>
      ) : null}
    </div>
  );
}
