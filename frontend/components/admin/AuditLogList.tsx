import { FileCheck, Link2, ShieldCheck, UserCog } from "lucide-react";
import type { LucideIcon } from "lucide-react";
import type { AuditLogEntryDto } from "@/lib/api";
import styles from "./AuditLogList.module.css";

const ICON_BY_ENTITY_TYPE: Record<string, LucideIcon> = {
  PERSON: UserCog,
  RELATIONSHIP: Link2,
  VERIFICATION_REQUEST: ShieldCheck,
  CORRECTION_REQUEST: FileCheck,
};

export function AuditLogList({ entries }: { entries: AuditLogEntryDto[] }) {
  return (
    <div className={styles.list}>
      {entries.map((entry) => {
        const Icon = ICON_BY_ENTITY_TYPE[entry.entityType] ?? UserCog;
        return (
          <div key={entry.id} className={styles.entry}>
            <span className={styles.icon} aria-hidden="true">
              <Icon size={16} />
            </span>
            <div className={styles.body}>
              <p className={styles.summary}>{entry.summary}</p>
              <p className={styles.meta}>
                {entry.actorUsername} · {formatDateTime(entry.createdAt)}
              </p>
            </div>
          </div>
        );
      })}
    </div>
  );
}

function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString(undefined, {
    year: "numeric",
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}
