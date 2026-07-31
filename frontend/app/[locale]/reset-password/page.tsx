import { getTranslations } from "next-intl/server";
import { PageShell } from "@/components/PageShell";
import { ResetPasswordForm } from "@/components/ResetPasswordForm";

export default async function ResetPasswordPage({
  searchParams,
}: {
  searchParams: Promise<{ token?: string }>;
}) {
  const t = await getTranslations("resetPassword");
  const { token } = await searchParams;

  return (
    <PageShell title={t("title")}>
      {token ? <ResetPasswordForm token={token} /> : <p>{t("invalidToken")}</p>}
    </PageShell>
  );
}
