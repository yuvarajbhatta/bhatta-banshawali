import { getTranslations } from "next-intl/server";
import { Link } from "@/i18n/navigation";
import { Reveal } from "@/components/motion/Reveal";
import { ScrollReveal } from "@/components/motion/ScrollReveal";
import type { AdminSummaryDto, PublicStatsDto } from "@/lib/api";
import styles from "./AdminDashboard.module.css";

interface AdminDashboardProps {
  summary: AdminSummaryDto | null;
  stats: PublicStatsDto | null;
}

export async function AdminDashboard({ summary, stats }: AdminDashboardProps) {
  const t = await getTranslations("dashboardPage.admin");

  if (!summary) {
    return (
      <Reveal>
        <div className={styles.notice}>{t("notAdmin")}</div>
      </Reveal>
    );
  }

  const signupItems = summary.recentPendingSignups.map((s) => ({
    id: s.id,
    label: s.submittedFullName,
    meta: formatDate(s.submittedAt),
    href: `/admin/signups/${s.id}`,
  }));
  const correctionItems = summary.recentPendingCorrections.map((c) => ({
    id: c.id,
    label: `${c.personName} — ${c.field}`,
    meta: formatDate(c.submittedAt),
    href: "/admin/corrections",
  }));

  return (
    <div className={styles.dashboard}>
      <Reveal>
        <div className={styles.hero}>
          <p className={styles.eyebrow}>{t("welcome")}</p>
          <p className={styles.subtitle}>{t("subtitle")}</p>
        </div>
      </Reveal>

      <ScrollReveal className={styles.statGrid}>
        <StatCard value={summary.pendingSignupCount} label={t("pendingSignups")} />
        <StatCard value={summary.pendingCorrectionCount} label={t("pendingCorrections")} />
        {stats ? <StatCard value={stats.documentedFamilyMembers} label={t("familyMembers")} /> : null}
        {stats ? <StatCard value={stats.documentedGenerations} label={t("generationsRecorded")} /> : null}
      </ScrollReveal>

      <Reveal delay={0.1}>
        <div className={styles.queueGrid}>
          <QueueCard
            title={t("pendingSignups")}
            items={signupItems}
            reviewHref="/admin/signups"
            reviewLabel={t("reviewAll")}
            emptyLabel={t("noneWaiting")}
          />
          <QueueCard
            title={t("pendingCorrections")}
            items={correctionItems}
            reviewHref="/admin/corrections"
            reviewLabel={t("reviewAll")}
            emptyLabel={t("noneWaiting")}
          />
        </div>
      </Reveal>
    </div>
  );
}

function StatCard({ value, label }: { value: number; label: string }) {
  return (
    <div className={styles.statCard}>
      <span className={styles.statValue}>{value}</span>
      <span className={styles.statLabel}>{label}</span>
    </div>
  );
}

function QueueCard({
  title,
  items,
  reviewHref,
  reviewLabel,
  emptyLabel,
}: {
  title: string;
  items: { id: number; label: string; meta: string; href: string }[];
  reviewHref: "/admin/signups" | "/admin/corrections";
  reviewLabel: string;
  emptyLabel: string;
}) {
  return (
    <div className={styles.queueCard}>
      <div className={styles.queueHeader}>
        <h3>{title}</h3>
        <Link href={reviewHref} className={styles.reviewLink}>
          {reviewLabel}
        </Link>
      </div>
      {items.length === 0 ? (
        <p className={styles.empty}>{emptyLabel}</p>
      ) : (
        <ul className={styles.queueList}>
          {items.map((item) => (
            <li key={item.id}>
              <Link href={item.href}>{item.label}</Link>
              <span className={styles.meta}>{item.meta}</span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString(undefined, { year: "numeric", month: "short", day: "numeric" });
}
