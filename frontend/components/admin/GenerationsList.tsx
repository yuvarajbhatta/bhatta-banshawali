import { getTranslations } from "next-intl/server";
import { Link } from "@/i18n/navigation";
import type { AdminPersonDto } from "@/lib/api";
import styles from "./GenerationsList.module.css";

export async function GenerationsList({ people }: { people: AdminPersonDto[] }) {
  const t = await getTranslations("adminGenerationsPage");

  const byGeneration = new Map<number, AdminPersonDto[]>();
  for (const person of people) {
    if (person.generationNumber == null) continue;
    const bucket = byGeneration.get(person.generationNumber) ?? [];
    bucket.push(person);
    byGeneration.set(person.generationNumber, bucket);
  }

  const generations = Array.from(byGeneration.keys()).sort((a, b) => a - b);

  if (generations.length === 0) {
    return <p>{t("empty")}</p>;
  }

  return (
    <div>
      {generations.map((generation) => {
        const members = byGeneration.get(generation) ?? [];
        return (
          <div key={generation} className={styles.block}>
            <h2 className={styles.heading}>{t("label", { number: generation })}</h2>
            <div className={styles.grid}>
              {members.map((person) => (
                <Link key={person.id} href={`/directory/${person.id}`} className={styles.personLink}>
                  <span>{`${person.firstName} ${person.lastName}`.trim()}</span>
                  {person.gender ? <span className={styles.genderMark}>({person.gender})</span> : null}
                </Link>
              ))}
            </div>
          </div>
        );
      })}
    </div>
  );
}
