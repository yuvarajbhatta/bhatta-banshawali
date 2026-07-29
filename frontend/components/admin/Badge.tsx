import type { ReactNode } from "react";
import styles from "./Badge.module.css";

type BadgeTone = "neutral" | "positive" | "warning" | "danger";

export function Badge({ tone, children }: { tone: BadgeTone; children: ReactNode }) {
  return <span className={`${styles.badge} ${styles[tone]}`}>{children}</span>;
}

export function matchConfidenceTone(confidence: "HIGH" | "MEDIUM" | "LOW"): BadgeTone {
  if (confidence === "HIGH") return "positive";
  if (confidence === "MEDIUM") return "warning";
  return "neutral";
}

export function verificationStatusTone(status: "PENDING" | "APPROVED" | "REJECTED" | "NEEDS_MORE_INFO"): BadgeTone {
  if (status === "APPROVED") return "positive";
  if (status === "REJECTED") return "danger";
  if (status === "NEEDS_MORE_INFO") return "warning";
  return "neutral";
}

export function correctionStatusTone(status: "PENDING" | "APPROVED" | "REJECTED"): BadgeTone {
  if (status === "APPROVED") return "positive";
  if (status === "REJECTED") return "danger";
  return "neutral";
}
