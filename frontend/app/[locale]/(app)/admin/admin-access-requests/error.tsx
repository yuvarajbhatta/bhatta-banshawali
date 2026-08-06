"use client";

import { useTranslations } from "next-intl";
import { RouteError } from "@/components/shell/RouteError";

export default function AdminAccessRequestsError({ error, reset }: { error: Error & { digest?: string }; reset: () => void }) {
  const t = useTranslations("adminAccessRequestsPage");
  return <RouteError title={t("title")} error={error} reset={reset} />;
}
