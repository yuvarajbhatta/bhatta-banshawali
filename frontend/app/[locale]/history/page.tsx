import { getLocale } from "next-intl/server";
import { getPublishedArticle } from "@/lib/api";
import { localizeArticle } from "@/lib/localize-article";
import { Paragraphs } from "@/components/Paragraphs";
import { PageShell } from "@/components/PageShell";

export default async function HistoryPage() {
  const article = await getPublishedArticle("family-history");
  const locale = await getLocale();
  const localized = article ? localizeArticle(article, locale) : null;

  return (
    <PageShell title={localized?.title ?? "Family History"}>
      {localized ? <Paragraphs text={localized.body} /> : <p>Content coming soon.</p>}
    </PageShell>
  );
}
