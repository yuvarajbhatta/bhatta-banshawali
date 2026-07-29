"use client";

import { useTranslations } from "next-intl";
import { useRouter } from "@/i18n/navigation";
import { createAdminPerson, type AdminPersonRequest } from "@/lib/api";
import { PersonForm } from "./PersonForm";

export function NewPersonForm() {
  const t = useTranslations("adminPersonsPage");
  const router = useRouter();

  async function handleSubmit(body: AdminPersonRequest) {
    const created = await createAdminPerson(body);
    router.push(`/admin/persons/${created.id}/edit`);
    router.refresh();
  }

  return <PersonForm submitLabel={t("create")} onSubmit={handleSubmit} />;
}
