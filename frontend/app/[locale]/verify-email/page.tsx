import { getTranslations } from "next-intl/server";
import { PageShell } from "@/components/PageShell";
import { VerifyEmailAction } from "@/components/VerifyEmailAction";

export default async function VerifyEmailPage({
  searchParams,
}: {
  searchParams: Promise<{ token?: string }>;
}) {
  const t = await getTranslations("verifyEmail");
  const { token } = await searchParams;

  return (
    <PageShell title={t("title")}>
      {token ? <VerifyEmailAction token={token} /> : <p>{t("missingToken")}</p>}
    </PageShell>
  );
}
