import { useTranslations } from "next-intl";
import { PageShell } from "@/components/PageShell";
import { SignupForm } from "@/components/SignupForm";

export default function SignupPage() {
  const t = useTranslations("signupPage");

  return (
    <PageShell title={t("title")}>
      <SignupForm />
    </PageShell>
  );
}
