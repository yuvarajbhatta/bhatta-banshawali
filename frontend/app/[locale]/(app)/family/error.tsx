"use client";

import { useTranslations } from "next-intl";
import { RouteError } from "@/components/shell/RouteError";

export default function FamilyError({ error, reset }: { error: Error & { digest?: string }; reset: () => void }) {
  const t = useTranslations("familyPage");
  return <RouteError title={t("title")} error={error} reset={reset} />;
}
