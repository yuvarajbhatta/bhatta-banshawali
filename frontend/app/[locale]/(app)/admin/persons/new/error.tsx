"use client";

import { useTranslations } from "next-intl";
import { RouteError } from "@/components/shell/RouteError";

export default function AdminPersonsNewError({ error, reset }: { error: Error & { digest?: string }; reset: () => void }) {
  const t = useTranslations("adminPersonsPage");
  return <RouteError title={t("newTitle")} error={error} reset={reset} />;
}
