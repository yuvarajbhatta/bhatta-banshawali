import { getTranslations } from "next-intl/server";
import { PageShell } from "@/components/PageShell";
import { VerifyEmailOtpForm } from "@/components/VerifyEmailOtpForm";

export default async function VerifyEmailPage({
  searchParams,
}: {
  searchParams: Promise<{ email?: string }>;
}) {
  const t = await getTranslations("verifyEmail");
  const { email } = await searchParams;

  return (
    <PageShell title={t("title")}>
      {email ? <VerifyEmailOtpForm email={email} /> : <p>{t("missingEmail")}</p>}
    </PageShell>
  );
}
