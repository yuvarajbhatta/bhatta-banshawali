"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/Button";
import { Badge, matchConfidenceTone } from "./Badge";
import {
  AdminActionError,
  mergeAdminDuplicate,
  type DuplicateCandidateDto,
  type DuplicatePersonSnapshotDto,
} from "@/lib/api";
import styles from "./DuplicateCandidatesManager.module.css";
import queueStyles from "./QueueTable.module.css";

function pairKey(a: DuplicatePersonSnapshotDto, b: DuplicatePersonSnapshotDto): string {
  return `${a.id}-${b.id}`;
}

// Session-only dismissal (no persistence table for this in v1 -- see
// docs/07-migration-plan.md/roadmap notes): a dismissed pair reappears on
// reload. That's a deliberate scope cut, not an oversight.
export function DuplicateCandidatesManager({ initialItems }: { initialItems: DuplicateCandidateDto[] }) {
  const t = useTranslations("adminDuplicatesPage");
  const [items, setItems] = useState(initialItems);
  const [dismissed, setDismissed] = useState<Set<string>>(new Set());
  const [expandedKey, setExpandedKey] = useState<string | null>(null);
  const [survivorByKey, setSurvivorByKey] = useState<Record<string, number>>({});
  const [mergingKey, setMergingKey] = useState<string | null>(null);
  const [errorByKey, setErrorByKey] = useState<Record<string, string>>({});

  const visibleItems = items.filter((item) => !dismissed.has(pairKey(item.personA, item.personB)));

  function defaultSurvivorId(candidate: DuplicateCandidateDto): number {
    // Default to whichever record has more data -- the admin can override.
    return candidate.personA.populatedFieldCount >= candidate.personB.populatedFieldCount
      ? candidate.personA.id
      : candidate.personB.id;
  }

  function survivorIdFor(candidate: DuplicateCandidateDto): number {
    return survivorByKey[pairKey(candidate.personA, candidate.personB)] ?? defaultSurvivorId(candidate);
  }

  function dismiss(key: string) {
    setDismissed((current) => new Set(current).add(key));
  }

  async function handleMerge(candidate: DuplicateCandidateDto) {
    const key = pairKey(candidate.personA, candidate.personB);
    const survivorId = survivorIdFor(candidate);
    const loserId = survivorId === candidate.personA.id ? candidate.personB.id : candidate.personA.id;

    setMergingKey(key);
    setErrorByKey((current) => ({ ...current, [key]: "" }));
    try {
      await mergeAdminDuplicate(survivorId, loserId);
      // Also drop any other candidate pair that referenced the now-deleted
      // loser, so a stale pair doesn't linger and 404 if clicked.
      setItems((current) => current.filter((item) => item.personA.id !== loserId && item.personB.id !== loserId));
    } catch (error) {
      setErrorByKey((current) => ({
        ...current,
        [key]: error instanceof AdminActionError ? error.message : t("errors.generic"),
      }));
    } finally {
      setMergingKey(null);
    }
  }

  if (visibleItems.length === 0) {
    return <div className={queueStyles.empty}>{t("empty")}</div>;
  }

  return (
    <div className={styles.list}>
      {visibleItems.map((candidate) => {
        const key = pairKey(candidate.personA, candidate.personB);
        const expanded = expandedKey === key;
        const survivorId = survivorIdFor(candidate);
        const survivor = survivorId === candidate.personA.id ? candidate.personA : candidate.personB;
        const loser = survivorId === candidate.personA.id ? candidate.personB : candidate.personA;

        return (
          <div key={key} className={styles.card}>
            <div className={styles.headerRow}>
              <div>
                <span className={styles.names}>
                  {candidate.personA.englishFullName} <span aria-hidden="true">/</span>{" "}
                  {candidate.personB.englishFullName}
                </span>
                <Badge tone={matchConfidenceTone(candidate.confidence)}>{candidate.confidence}</Badge>
              </div>
              <div className={styles.actions}>
                <button
                  type="button"
                  className={queueStyles.reviewLink}
                  onClick={() => setExpandedKey(expanded ? null : key)}
                >
                  {expanded ? t("hideDetails") : t("compareAndMerge")}
                </button>
                <button type="button" className={queueStyles.reviewLink} onClick={() => dismiss(key)}>
                  {t("dismiss")}
                </button>
              </div>
            </div>

            <ul className={styles.reasons}>
              {candidate.reasons.map((reason, index) => (
                <li key={index}>{reason}</li>
              ))}
            </ul>

            {expanded ? (
              <div className={styles.comparePanel}>
                <table className={styles.compareTable}>
                  <thead>
                    <tr>
                      <th />
                      <th>{candidate.personA.englishFullName}</th>
                      <th>{candidate.personB.englishFullName}</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr>
                      <th scope="row">{t("fields.survivor")}</th>
                      <td>
                        <label>
                          <input
                            type="radio"
                            name={`survivor-${key}`}
                            checked={survivorId === candidate.personA.id}
                            onChange={() =>
                              setSurvivorByKey((current) => ({ ...current, [key]: candidate.personA.id }))
                            }
                          />{" "}
                          {t("keepThisOne")}
                        </label>
                      </td>
                      <td>
                        <label>
                          <input
                            type="radio"
                            name={`survivor-${key}`}
                            checked={survivorId === candidate.personB.id}
                            onChange={() =>
                              setSurvivorByKey((current) => ({ ...current, [key]: candidate.personB.id }))
                            }
                          />{" "}
                          {t("keepThisOne")}
                        </label>
                      </td>
                    </tr>
                    <tr>
                      <th scope="row">{t("fields.nepaliName")}</th>
                      <td>{candidate.personA.nepaliFullName || "—"}</td>
                      <td>{candidate.personB.nepaliFullName || "—"}</td>
                    </tr>
                    <tr>
                      <th scope="row">{t("fields.gender")}</th>
                      <td>{candidate.personA.gender || "—"}</td>
                      <td>{candidate.personB.gender || "—"}</td>
                    </tr>
                    <tr>
                      <th scope="row">{t("fields.birthDate")}</th>
                      <td>{candidate.personA.birthDate || "—"}</td>
                      <td>{candidate.personB.birthDate || "—"}</td>
                    </tr>
                    <tr>
                      <th scope="row">{t("fields.deathDate")}</th>
                      <td>{candidate.personA.deathDate || "—"}</td>
                      <td>{candidate.personB.deathDate || "—"}</td>
                    </tr>
                    <tr>
                      <th scope="row">{t("fields.generation")}</th>
                      <td>{candidate.personA.generationNumber ?? "—"}</td>
                      <td>{candidate.personB.generationNumber ?? "—"}</td>
                    </tr>
                    <tr>
                      <th scope="row">{t("fields.populatedFields")}</th>
                      <td>{candidate.personA.populatedFieldCount}</td>
                      <td>{candidate.personB.populatedFieldCount}</td>
                    </tr>
                  </tbody>
                </table>

                <div className={styles.mergeAction}>
                  <Button variant="destructive" disabled={mergingKey === key} onClick={() => handleMerge(candidate)}>
                    {t("confirmMerge", { loserName: loser.englishFullName, survivorName: survivor.englishFullName })}
                  </Button>
                  {errorByKey[key] ? <span className={styles.error}>{errorByKey[key]}</span> : null}
                </div>
              </div>
            ) : null}
          </div>
        );
      })}
    </div>
  );
}
