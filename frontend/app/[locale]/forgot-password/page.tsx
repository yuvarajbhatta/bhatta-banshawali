import { getTranslations } from "next-intl/server";
import { PageShell } from "@/components/PageShell";
import { ForgotPasswordForm } from "@/components/ForgotPasswordForm";

export default async function ForgotPasswordPage() {
  const t = await getTranslations("forgotPassword");

  return (
    <PageShell title={t("title")}>
      <ForgotPasswordForm />
    </PageShell>
  );
}
