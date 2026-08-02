import { getTranslations } from "next-intl/server";
import { Link } from "@/i18n/navigation";
import { Button } from "@/components/Button";
import { Reveal } from "@/components/motion/Reveal";
import { AncestorChain } from "@/components/dashboard/AncestorChain";
import { ImmediateFamily } from "@/components/dashboard/ImmediateFamily";
import type { MemberProfileDto } from "@/lib/api";
import styles from "./MemberDashboard.module.css";

export async function MemberDashboard({ profile }: { profile: MemberProfileDto }) {
  const t = await getTranslations("dashboardPage.member");

  if (!profile.linked || !profile.person || !profile.family) {
    return (
      <Reveal>
        <div className={styles.notice}>{t("unlinked")}</div>
      </Reveal>
    );
  }

  const { person, family, ancestorChain } = profile;

  return (
    <div className={styles.dashboard}>
      <Reveal>
        <div className={styles.hero}>
          <div>
            <p className={styles.eyebrow}>{t("welcomeBack")}</p>
            <h2 className={styles.name}>{person.englishFullName}</h2>
            {person.nepaliFullName ? <p className={styles.nepaliName}>{person.nepaliFullName}</p> : null}
            {person.generationNumber != null ? (
              <span className={styles.badge}>{t("generation", { number: person.generationNumber })}</span>
            ) : null}
          </div>
          <Link href={`/directory/${person.id}`}>
            <Button variant="secondary">{t("viewFullProfile")}</Button>
          </Link>
        </div>
      </Reveal>

      <Reveal delay={0.1}>
        <div className={styles.familyPanel}>
          <div className={styles.familyHeader}>
            <h3>{t("familyTitle")}</h3>
            <p className={styles.familyHint}>{t("familyHint")}</p>
          </div>
          <ImmediateFamily family={family} />
          <AncestorChain chain={ancestorChain} />
          <div className={styles.viewTreeAction}>
            <Link href="/family">
              <Button variant="primary">{t("viewYourFamily")}</Button>
            </Link>
            <Link href={`/tree?focus=${person.id}`}>
              <Button variant="secondary">{t("viewFullTree")}</Button>
            </Link>
          </div>
        </div>
      </Reveal>

      <Reveal delay={0.2} className={styles.actions}>
        <Link href="/directory">
          <Button variant="primary">{t("browseDirectory")}</Button>
        </Link>
      </Reveal>
    </div>
  );
}
