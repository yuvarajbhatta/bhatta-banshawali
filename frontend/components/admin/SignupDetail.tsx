"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Link } from "@/i18n/navigation";
import { Button } from "@/components/Button";
import {
  approveSignup,
  rejectSignup,
  requestMoreInfoSignup,
  type AdminSignupDetailDto,
  type FatherCandidateDto,
  type MatchCandidateDto,
} from "@/lib/api";
import { Badge, matchConfidenceTone, verificationStatusTone } from "./Badge";
import styles from "./SignupDetail.module.css";

interface SignupDetailProps {
  initialDetail: AdminSignupDetailDto;
}

type Action = "approve" | "reject" | "request-more-info" | null;

// A single shared selection instead of two independently-managed IDs --
// structurally guarantees "at most one of linkedPersonId /
// createAsChildOfFatherId" rather than trusting two pieces of state to
// stay mutually exclusive.
type SelectedMatch = { type: "link"; personId: number } | { type: "createChild"; fatherId: number } | null;

export function SignupDetail({ initialDetail }: SignupDetailProps) {
  const t = useTranslations("adminSignupsPage");
  const [detail, setDetail] = useState(initialDetail);
  const [selectedMatch, setSelectedMatch] = useState<SelectedMatch>(null);
  // Only consulted when selectedMatch is a "createChild" candidate that
  // has a matchedMother -- defaults to on, so the corroborated match is
  // linked unless the admin explicitly unchecks it.
  const [linkMatchedMother, setLinkMatchedMother] = useState(true);
  const [decisionNote, setDecisionNote] = useState("");
  const [pendingAction, setPendingAction] = useState<Action>(null);
  const [error, setError] = useState<string | null>(null);

  async function handleAction(action: Exclude<Action, null>) {
    setPendingAction(action);
    setError(null);
    try {
      const body = {
        decisionNote: decisionNote.trim() || undefined,
        linkedPersonId: selectedMatch?.type === "link" ? selectedMatch.personId : undefined,
        createAsChildOfFatherId: selectedMatch?.type === "createChild" ? selectedMatch.fatherId : undefined,
        linkMatchedMother: selectedMatch?.type === "createChild" ? linkMatchedMother : undefined,
      };
      const updated =
        action === "approve"
          ? await approveSignup(detail.id, body)
          : action === "reject"
            ? await rejectSignup(detail.id, body)
            : await requestMoreInfoSignup(detail.id, body);
      setDetail(updated);
    } catch {
      setError(t("actionError"));
    } finally {
      setPendingAction(null);
    }
  }

  return (
    <div className={styles.profile}>
      <div className={styles.card}>
        <h2>{t("submittedInfo")}</h2>
        <dl className={styles.grid}>
          <dt>{t("submittedName")}</dt>
          <dd>{detail.submittedFullName}</dd>

          <dt>{t("fatherName")}</dt>
          <dd>{detail.submittedFatherName}</dd>

          <dt>{t("grandfatherName")}</dt>
          <dd>{detail.submittedGrandfatherName}</dd>

          <dt>{t("birthDate")}</dt>
          <dd>
            {detail.submittedDobAd ?? "—"}
            {detail.submittedDobBsYear != null
              ? ` (BS ${detail.submittedDobBsYear}-${detail.submittedDobBsMonth}-${detail.submittedDobBsDay})`
              : null}
          </dd>

          {detail.motherName ? (
            <>
              <dt>{t("motherName")}</dt>
              <dd>{detail.motherName}</dd>
            </>
          ) : null}
          {detail.placeOfBirth ? (
            <>
              <dt>{t("birthPlace")}</dt>
              <dd>{detail.placeOfBirth}</dd>
            </>
          ) : null}
          {detail.ancestralVillage ? (
            <>
              <dt>{t("ancestralVillage")}</dt>
              <dd>{detail.ancestralVillage}</dd>
            </>
          ) : null}
          {detail.familyBranch ? (
            <>
              <dt>{t("familyBranch")}</dt>
              <dd>{detail.familyBranch}</dd>
            </>
          ) : null}
          {detail.knownRelativeName ? (
            <>
              <dt>{t("knownRelative")}</dt>
              <dd>{detail.knownRelativeName}</dd>
            </>
          ) : null}
          {detail.invitationCode ? (
            <>
              <dt>{t("invitationCode")}</dt>
              <dd>{detail.invitationCode}</dd>
            </>
          ) : null}
          {detail.applicantNote ? (
            <>
              <dt>{t("applicantNote")}</dt>
              <dd>{detail.applicantNote}</dd>
            </>
          ) : null}
        </dl>
      </div>

      <div className={styles.card}>
        <h2>{t("matchEvidence")}</h2>
        <p>
          {t("confidence")}: <Badge tone={matchConfidenceTone(detail.matchConfidence)}>{detail.matchConfidence}</Badge>
        </p>
        <p className={styles.helpText}>{t("matchEvidenceHelp")}</p>

        {detail.candidates.length === 0 && detail.fatherCandidates.length === 0 ? (
          <p className={styles.helpText}>{t("noCandidates")}</p>
        ) : null}

        {detail.candidates.length > 0 ? (
          <table className={styles.candidateTable}>
            <thead>
              <tr>
                {detail.status === "PENDING" ? <th>{t("linkThisPerson")}</th> : null}
                <th>ID</th>
                <th>Name</th>
                <th>Generation</th>
              </tr>
            </thead>
            <tbody>
              {detail.candidates.map((candidate) => (
                <CandidateRows
                  key={candidate.person.id}
                  candidate={candidate}
                  columns={detail.status === "PENDING" ? 4 : 3}
                  radio={
                    detail.status === "PENDING"
                      ? {
                          checked: selectedMatch?.type === "link" && selectedMatch.personId === candidate.person.id,
                          onChange: () => setSelectedMatch({ type: "link", personId: candidate.person.id }),
                        }
                      : null
                  }
                />
              ))}
            </tbody>
          </table>
        ) : null}

        {detail.fatherCandidates.length > 0 ? (
          <>
            <h3>{t("createAsChildTitle")}</h3>
            <p className={styles.helpText}>{t("createAsChildHelp")}</p>
            <table className={styles.candidateTable}>
              <thead>
                <tr>
                  {detail.status === "PENDING" ? <th>{t("createThisChild")}</th> : null}
                  <th>ID</th>
                  <th>{t("fatherName")}</th>
                  <th>Generation</th>
                </tr>
              </thead>
              <tbody>
                {detail.fatherCandidates.map((candidate) => {
                  const checked = selectedMatch?.type === "createChild" && selectedMatch.fatherId === candidate.person.id;
                  return (
                    <CandidateRows
                      key={candidate.person.id}
                      candidate={candidate}
                      columns={detail.status === "PENDING" ? 4 : 3}
                      radio={
                        detail.status === "PENDING"
                          ? {
                              checked,
                              onChange: () => setSelectedMatch({ type: "createChild", fatherId: candidate.person.id }),
                            }
                          : null
                      }
                      motherLink={
                        detail.status === "PENDING" && checked && candidate.matchedMother
                          ? { checked: linkMatchedMother, onChange: setLinkMatchedMother }
                          : null
                      }
                    />
                  );
                })}
              </tbody>
            </table>
          </>
        ) : null}
      </div>

      <div className={styles.card}>
        <h2>{t("decision")}</h2>

        {detail.status === "PENDING" ? (
          <>
            {detail.candidates.length > 0 ? <p className={styles.helpText}>{t("linkHelp")}</p> : null}
            <label className={styles.label} htmlFor="decision-note">
              {t("decisionNote")}
            </label>
            <textarea
              id="decision-note"
              className={styles.textarea}
              rows={2}
              value={decisionNote}
              onChange={(event) => setDecisionNote(event.target.value)}
            />
            <div className={styles.decisionActions}>
              <Button variant="primary" onClick={() => handleAction("approve")} disabled={pendingAction !== null}>
                {t("approve")}
              </Button>
              <Button variant="secondary" onClick={() => handleAction("request-more-info")} disabled={pendingAction !== null}>
                {t("requestMoreInfo")}
              </Button>
              <Button variant="destructive" onClick={() => handleAction("reject")} disabled={pendingAction !== null}>
                {t("reject")}
              </Button>
            </div>
            {error ? <p className={styles.errorNotice}>{error}</p> : null}
          </>
        ) : (
          <>
            <p>
              <Badge tone={verificationStatusTone(detail.status)}>{detail.status}</Badge>{" "}
              {detail.reviewedByUsername ? (
                <span className={styles.reviewedMeta}>{t("reviewedBy", { username: detail.reviewedByUsername })}</span>
              ) : null}
            </p>
            {detail.decisionNote ? <p>{detail.decisionNote}</p> : null}
          </>
        )}
      </div>
    </div>
  );
}

interface CandidateRowsProps {
  candidate: MatchCandidateDto | FatherCandidateDto;
  columns: number;
  radio: { checked: boolean; onChange: () => void } | null;
  // Only ever set for a father candidate that has a matchedMother and is
  // currently the selected radio -- lets the admin decline linking her
  // even though the father candidate itself is confirmed.
  motherLink?: { checked: boolean; onChange: (checked: boolean) => void } | null;
}

// A real family tree with a shared surname and a small pool of recurring
// first names can produce several identically-named candidates -- a bare
// name and ID is useless for telling them apart. The ancestor-chain row
// beneath each candidate is what actually lets the admin recognize the
// correct lineage, the same way they'd recognize their own family by
// name rather than by an arbitrary database ID.
function CandidateRows({ candidate, columns, radio, motherLink }: CandidateRowsProps) {
  const t = useTranslations("adminSignupsPage");
  const { person, ancestorChain } = candidate;
  const matchedMother = "matchedMother" in candidate ? candidate.matchedMother : null;

  return (
    <>
      <tr>
        {radio ? (
          <td>
            <input type="radio" name="matchSelection" checked={radio.checked} onChange={radio.onChange} aria-label={person.englishFullName} />
          </td>
        ) : null}
        <td>{person.id}</td>
        <td>
          <Link href={`/directory/${person.id}`} className={styles.candidateLink}>
            {person.englishFullName}
          </Link>
        </td>
        <td>{person.generationNumber ?? "—"}</td>
      </tr>
      <tr>
        <td colSpan={columns} className={styles.ancestorChainCell}>
          <span className={styles.ancestorChainLabel}>{t("ancestorChain")}: </span>
          {ancestorChain.length > 1
            ? ancestorChain.map((ancestor, index) => (
                <span key={ancestor.id}>
                  {index > 0 ? " → " : ""}
                  <Link href={`/directory/${ancestor.id}`} className={styles.candidateLink}>
                    {ancestor.englishFullName}
                  </Link>
                </span>
              ))
            : t("ancestorChainUnknown")}
        </td>
      </tr>
      {matchedMother ? (
        <tr>
          <td colSpan={columns} className={styles.ancestorChainCell}>
            {motherLink ? (
              <label>
                <input
                  type="checkbox"
                  checked={motherLink.checked}
                  onChange={(event) => motherLink.onChange(event.target.checked)}
                />{" "}
                {t("linkMatchedMother", { name: matchedMother.englishFullName })}
              </label>
            ) : (
              t("matchedMotherNote", { name: matchedMother.englishFullName })
            )}
          </td>
        </tr>
      ) : null}
    </>
  );
}
