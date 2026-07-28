import { getLocale } from "next-intl/server";
import { getPublishedArticle } from "@/lib/api";
import { localizeArticle } from "@/lib/localize-article";
import { Paragraphs } from "@/components/Paragraphs";
import { PageShell } from "@/components/PageShell";

export default async function MembershipPage() {
  const article = await getPublishedArticle("membership-verification");
  const locale = await getLocale();
  const localized = article ? localizeArticle(article, locale) : null;

  return (
    <PageShell title={localized?.title ?? "How Membership Verification Works"}>
      {localized ? <Paragraphs text={localized.body} /> : <p>Content coming soon.</p>}
    </PageShell>
  );
}
