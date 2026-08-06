"use client";

import { useEffect, useState } from "react";
import { useTranslations } from "next-intl";
import { Link, useRouter } from "@/i18n/navigation";
import { searchPersons, UnauthenticatedError, type PersonSummaryDto } from "@/lib/api";
import styles from "./PersonSearch.module.css";

export function PersonSearch() {
  const t = useTranslations("directoryPage");
  const router = useRouter();
  const [keyword, setKeyword] = useState("");
  const [results, setResults] = useState<PersonSummaryDto[] | null>(null);

  useEffect(() => {
    if (!keyword.trim()) {
      return;
    }
    let cancelled = false;
    const timeout = setTimeout(() => {
      searchPersons(keyword.trim())
        .then((people) => {
          if (!cancelled) {
            setResults(people);
          }
        })
        .catch((error: unknown) => {
          if (cancelled) {
            return;
          }
          // A session that expired mid-search must not render identically
          // to a genuine zero-result search -- that silently hid the real
          // problem (re-login needed) behind what looked like "no matches".
          if (error instanceof UnauthenticatedError) {
            router.push("/login");
            return;
          }
          setResults([]);
        });
    }, 300);

    return () => {
      cancelled = true;
      clearTimeout(timeout);
    };
  }, [keyword, router]);

  return (
    <div className={styles.container}>
      <p className={styles.subtitle}>{t("subtitle")}</p>
      <input
        type="search"
        className={styles.input}
        placeholder={t("searchPlaceholder")}
        value={keyword}
        onChange={(event) => setKeyword(event.target.value)}
      />

      {keyword.trim() && results !== null ? (
        results.length > 0 ? (
          <ul className={styles.results}>
            {results.map((person) => (
              <li key={person.id}>
                <Link href={`/directory/${person.id}`} className={styles.resultLink}>
                  <span>{person.englishFullName}</span>
                  {person.nepaliFullName ? <span className={styles.nepaliName}>{person.nepaliFullName}</span> : null}
                  {person.generationNumber != null ? (
                    <span className={styles.generation}>{t("generation", { number: person.generationNumber })}</span>
                  ) : null}
                </Link>
              </li>
            ))}
          </ul>
        ) : (
          <p className={styles.empty}>{t("noResults")}</p>
        )
      ) : !keyword.trim() ? (
        <p className={styles.empty}>{t("prompt")}</p>
      ) : null}
    </div>
  );
}
