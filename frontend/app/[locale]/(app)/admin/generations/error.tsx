"use client";

import { useTranslations } from "next-intl";
import { RouteError } from "@/components/shell/RouteError";

export default function AdminGenerationsError({ error, reset }: { error: Error & { digest?: string }; reset: () => void }) {
  const t = useTranslations("adminGenerationsPage");
  return <RouteError title={t("title")} error={error} reset={reset} />;
}
