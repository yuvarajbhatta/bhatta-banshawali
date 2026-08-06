"use client";

import { useTranslations } from "next-intl";
import { RouteError } from "@/components/shell/RouteError";

export default function AdminDuplicatesError({ error, reset }: { error: Error & { digest?: string }; reset: () => void }) {
  const t = useTranslations("adminDuplicatesPage");
  return <RouteError title={t("title")} error={error} reset={reset} />;
}
