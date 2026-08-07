"use client";

import { useEffect, useRef, useState, type KeyboardEvent } from "react";
import { useTranslations } from "next-intl";
import { Search } from "lucide-react";
import { useRouter, usePathname } from "@/i18n/navigation";
import { searchPersons, UnauthenticatedError, type PersonSummaryDto } from "@/lib/api";
import styles from "./HeaderSearch.module.css";

/**
 * The header's global member search (brief section 5) against the real
 * GET /api/v1/persons?keyword= endpoint -- the same one PersonSearch
 * uses on /directory, just as a compact debounced dropdown instead of a
 * full-page result list. Only searches members: there's no Photos/
 * Events/Documents index to group results with (docs/frontend-redesign-plan.md).
 *
 * Doubles as the tree page's own search while on /tree -- that page used
 * to have a second, redundant search box (TreeFilters, now removed) right
 * above the canvas. On /tree this one shows tree-specific wording and, on
 * picking a result, sets ?focus= instead of navigating to the directory
 * profile -- TreeExplorer reacts to that query param to pan/select the
 * matched node (see its initialFocusId effect).
 */
export function HeaderSearch() {
  const t = useTranslations("appShell.header");
  const directoryT = useTranslations("directoryPage");
  const router = useRouter();
  const pathname = usePathname();
  const isTreePage = pathname === "/tree";
  const [keyword, setKeyword] = useState("");
  const [results, setResults] = useState<PersonSummaryDto[] | null>(null);
  const [open, setOpen] = useState(false);
  const [activeIndex, setActiveIndex] = useState(-1);
  const wrapperRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!keyword.trim()) {
      // Nothing to reset here: `showResults` below already gates the
      // dropdown on a non-empty keyword, so stale results from a
      // previous query never render while the input is empty.
      return;
    }
    let cancelled = false;
    const timeout = setTimeout(() => {
      searchPersons(keyword.trim())
        .then((people) => {
          if (!cancelled) {
            setResults(people.slice(0, 8));
            setActiveIndex(-1);
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
    }, 250);

    return () => {
      cancelled = true;
      clearTimeout(timeout);
    };
  }, [keyword, router]);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (wrapperRef.current && !wrapperRef.current.contains(event.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const showResults = open && keyword.trim().length > 0;

  function goToPerson(person: PersonSummaryDto) {
    setOpen(false);
    setKeyword("");
    if (isTreePage) {
      router.push(`/tree?focus=${person.id}`);
    } else {
      router.push(`/directory/${person.id}`);
    }
  }

  function handleKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (!results || results.length === 0) {
      return;
    }
    if (event.key === "ArrowDown") {
      event.preventDefault();
      setActiveIndex((current) => Math.min(current + 1, results.length - 1));
    } else if (event.key === "ArrowUp") {
      event.preventDefault();
      setActiveIndex((current) => Math.max(current - 1, 0));
    } else if (event.key === "Enter" && activeIndex >= 0) {
      const selected = results[activeIndex];
      if (selected) {
        event.preventDefault();
        goToPerson(selected);
      }
    } else if (event.key === "Escape") {
      setOpen(false);
    }
  }

  return (
    <div className={styles.wrapper} ref={wrapperRef}>
      <div className={styles.inputWrapper}>
        <Search size={16} aria-hidden="true" />
        <input
          type="search"
          role="combobox"
          aria-expanded={showResults}
          aria-controls="header-search-results"
          aria-label={t("searchLabel")}
          className={styles.input}
          placeholder={isTreePage ? t("searchPlaceholderTree") : t("searchPlaceholder")}
          value={keyword}
          onFocus={() => setOpen(true)}
          onChange={(event) => {
            setKeyword(event.target.value);
            setOpen(true);
          }}
          onKeyDown={handleKeyDown}
        />
      </div>

      {showResults ? (
        <ul id="header-search-results" role="listbox" className={styles.results}>
          {results === null ? null : results.length === 0 ? (
            <li className={styles.empty}>{directoryT("noResults")}</li>
          ) : (
            results.map((person, index) => (
              <li key={person.id} role="option" aria-selected={index === activeIndex}>
                <button
                  type="button"
                  className={index === activeIndex ? `${styles.resultLink} ${styles.resultActive}` : styles.resultLink}
                  onMouseEnter={() => setActiveIndex(index)}
                  onClick={() => goToPerson(person)}
                >
                  <span className={styles.resultName}>{person.englishFullName}</span>
                  {person.generationNumber != null ? (
                    <span className={styles.resultMeta}>{directoryT("generation", { number: person.generationNumber })}</span>
                  ) : null}
                </button>
              </li>
            ))
          )}
        </ul>
      ) : null}
    </div>
  );
}
