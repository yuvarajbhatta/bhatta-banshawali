"use client";

import { Fragment, useEffect, useRef, useState } from "react";
import { useTranslations } from "next-intl";
import { ArrowRight } from "lucide-react";
import { Link } from "@/i18n/navigation";
import { searchPersons, type PersonSummaryDto } from "@/lib/api";
import { findRelationshipPath, type FamilyGraphIndex, type RelationshipStep } from "@/lib/familyGraph";
import styles from "./RelationshipPathFinder.module.css";

interface RelationshipPathFinderProps {
  index: FamilyGraphIndex;
  selfId: number;
}

export function RelationshipPathFinder({ index, selfId }: RelationshipPathFinderProps) {
  const t = useTranslations("familyPage.relationshipPath");
  const relationT = useTranslations("relationshipType");
  const [keyword, setKeyword] = useState("");
  const [results, setResults] = useState<PersonSummaryDto[] | null>(null);
  const [open, setOpen] = useState(false);
  const [targetId, setTargetId] = useState<number | null>(null);
  const wrapperRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!keyword.trim()) {
      return;
    }
    let cancelled = false;
    const timeout = setTimeout(() => {
      searchPersons(keyword.trim())
        .then((people) => {
          if (!cancelled) {
            setResults(people.filter((p) => p.id !== selfId).slice(0, 8));
          }
        })
        .catch(() => {
          if (!cancelled) {
            setResults([]);
          }
        });
    }, 250);
    return () => {
      cancelled = true;
      clearTimeout(timeout);
    };
  }, [keyword, selfId]);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (wrapperRef.current && !wrapperRef.current.contains(event.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const path = targetId != null ? findRelationshipPath(index, selfId, targetId) : null;
  const showResults = open && keyword.trim().length > 0;

  return (
    <div>
      <div className={styles.searchWrapper} ref={wrapperRef}>
        <input
          type="search"
          className={styles.input}
          aria-label={t("searchLabel")}
          placeholder={t("searchPlaceholder")}
          value={keyword}
          onFocus={() => setOpen(true)}
          onChange={(event) => {
            setKeyword(event.target.value);
            setOpen(true);
          }}
        />
        {showResults ? (
          <ul className={styles.results}>
            {results === null ? null : results.length === 0 ? (
              <li className={styles.resultButton}>{t("noResults")}</li>
            ) : (
              results.map((person) => (
                <li key={person.id}>
                  <button
                    type="button"
                    className={styles.resultButton}
                    onClick={() => {
                      setTargetId(person.id);
                      setKeyword(person.englishFullName);
                      setOpen(false);
                    }}
                  >
                    {person.englishFullName}
                  </button>
                </li>
              ))
            )}
          </ul>
        ) : null}
      </div>

      {targetId == null ? (
        <p className={styles.prompt}>{t("prompt")}</p>
      ) : path == null ? (
        <p className={styles.notConnected}>{t("notConnected")}</p>
      ) : (
        <RelationshipPathView steps={path} selfId={selfId} youLabel={t("you")} relationLabel={(k) => relationT(k.toUpperCase())} />
      )}
    </div>
  );
}

function RelationshipPathView({
  steps,
  selfId,
  youLabel,
  relationLabel,
}: {
  steps: RelationshipStep[];
  selfId: number;
  youLabel: string;
  relationLabel: (relation: NonNullable<RelationshipStep["relationToPrevious"]>) => string;
}) {
  return (
    <div className={styles.path}>
      {steps.map((step, index) => {
        const name = step.person.id === selfId ? youLabel : step.person.englishFullName;
        const initials =
          name
            .split(/\s+/)
            .filter(Boolean)
            .slice(0, 2)
            .map((part) => part[0]?.toUpperCase())
            .join("") || "?";

        return (
          <Fragment key={step.person.id}>
            {index > 0 && step.relationToPrevious ? (
              <span className={styles.arrow}>
                <ArrowRight size={16} aria-hidden="true" />
                {relationLabel(step.relationToPrevious)}
              </span>
            ) : null}
            <Link href={`/directory/${step.person.id}`} className={styles.step}>
              <span className={styles.avatar} aria-hidden="true">
                {initials}
              </span>
              <span className={styles.stepName}>{name}</span>
            </Link>
          </Fragment>
        );
      })}
    </div>
  );
}
