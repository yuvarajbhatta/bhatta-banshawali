import type { ArticleDto } from "./api";

// Picks the Nepali title/body only when the viewer is on the Nepali locale
// AND a translation actually exists; falls back to English in every other
// case (including "ne" locale with no Nepali translation yet).
export function localizeArticle(article: ArticleDto, locale: string): { title: string; body: string } {
  if (locale === "ne" && article.titleNe && article.bodyNe) {
    return { title: article.titleNe, body: article.bodyNe };
  }
  return { title: article.titleEn, body: article.bodyEn };
}
