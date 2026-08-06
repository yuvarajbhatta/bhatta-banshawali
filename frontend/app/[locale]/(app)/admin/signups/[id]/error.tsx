"use client";

import { useTranslations } from "next-intl";
import { RouteError } from "@/components/shell/RouteError";

export default function AdminSignupDetailError({ error, reset }: { error: Error & { digest?: string }; reset: () => void }) {
  const t = useTranslations("adminSignupsPage");
  return <RouteError title={t("detailTitle")} error={error} reset={reset} />;
}
