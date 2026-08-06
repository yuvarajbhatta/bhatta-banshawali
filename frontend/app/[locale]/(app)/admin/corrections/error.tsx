"use client";

import { useTranslations } from "next-intl";
import { RouteError } from "@/components/shell/RouteError";

export default function AdminCorrectionsError({ error, reset }: { error: Error & { digest?: string }; reset: () => void }) {
  const t = useTranslations("adminCorrectionsPage");
  return <RouteError title={t("title")} error={error} reset={reset} />;
}
