"use client";

import { useTranslations } from "next-intl";
import { RouteError } from "@/components/shell/RouteError";

export default function AdminContentError({ error, reset }: { error: Error & { digest?: string }; reset: () => void }) {
  const t = useTranslations("adminContentPage");
  return <RouteError title={t("title")} error={error} reset={reset} />;
}
