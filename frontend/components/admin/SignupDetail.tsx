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
} from "@/lib/api";
import { Badge, matchConfidenceTone, verificationStatusTone } from "./Badge";
import styles from "./SignupDetail.module.css";

interface SignupDetailProps {
  initialDetail: AdminSignupDetailDto;
}

type Action = "approve" | "reject" | "request-more-info" | null;

export function SignupDetail({ initialDetail }: SignupDetailProps) {
  const t = useTranslations("adminSignupsPage");
  const [detail, setDetail] = useState(initialDetail);
  const [selectedCandidateId, setSelectedCandidateId] = useState<number | null>(null);
  const [decisionNote, setDecisionNote] = useState("");
  const [pendingAction, setPendingAction] = useState<Action>(null);
  const [error, setError] = useState<string | null>(null);

  async function handleAction(action: Exclude<Action, null>) {
    setPendingAction(action);
    setError(null);
    try {
      const body = { decisionNote: decisionNote.trim() || undefined, linkedPersonId: selectedCandidateId ?? undefined };
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

        {detail.candidates.length === 0 ? (
          <p className={styles.helpText}>{t("noCandidates")}</p>
        ) : (
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
                <tr key={candidate.id}>
                  {detail.status === "PENDING" ? (
                    <td>
                      <input
                        type="radio"
                        name="linkedPersonId"
                        checked={selectedCandidateId === candidate.id}
                        onChange={() => setSelectedCandidateId(candidate.id)}
                        aria-label={candidate.englishFullName}
                      />
                    </td>
                  ) : null}
                  <td>{candidate.id}</td>
                  <td>
                    <Link href={`/directory/${candidate.id}`} className={styles.candidateLink}>
                      {candidate.englishFullName}
                    </Link>
                  </td>
                  <td>{candidate.generationNumber ?? "—"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
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
