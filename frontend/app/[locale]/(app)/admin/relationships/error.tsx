"use client";

import { useTranslations } from "next-intl";
import { RouteError } from "@/components/shell/RouteError";

export default function AdminRelationshipsError({ error, reset }: { error: Error & { digest?: string }; reset: () => void }) {
  const t = useTranslations("adminRelationshipsPage");
  return <RouteError title={t("title")} error={error} reset={reset} />;
}
