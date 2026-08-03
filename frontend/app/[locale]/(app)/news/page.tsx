import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { getTranslations } from "next-intl/server";
import { PageHeader } from "@/components/shell/PageHeader";
import { NewsFeed } from "@/components/news/NewsFeed";
import { getAnnouncements, getMemberProfile } from "@/lib/api";

export default async function NewsPage() {
  const t = await getTranslations("newsPage");
  const cookieStore = await cookies();
  const cookieHeader = cookieStore.getAll().map((c) => `${c.name}=${c.value}`).join("; ");

  const profileResult = await getMemberProfile(cookieHeader);
  if (profileResult.kind === "unauthenticated") {
    redirect("/login");
  }

  const announcements = await getAnnouncements(cookieHeader);

  return (
    <>
      <PageHeader title={t("title")} />
      <NewsFeed announcements={announcements} />
    </>
  );
}
