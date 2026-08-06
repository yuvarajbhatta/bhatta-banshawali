"use client";

import { useTranslations } from "next-intl";
import { useRouter } from "@/i18n/navigation";
import { createAdminPerson, type AdminPersonRequest } from "@/lib/api";
import { PersonForm } from "./PersonForm";

export function NewPersonForm() {
  const t = useTranslations("adminPersonsPage");
  const router = useRouter();

  async function handleSubmit(body: AdminPersonRequest) {
    const result = await createAdminPerson(body);
    if (result.possibleDuplicates.length > 0) {
      const names = result.possibleDuplicates
        .map((p) => `${p.firstName} ${p.lastName}`.trim())
        .join(", ");
      window.alert(t("possibleDuplicateWarning", { names }));
    }
    router.push(`/admin/persons/${result.person.id}/edit`);
    router.refresh();
  }

  return <PersonForm submitLabel={t("create")} onSubmit={handleSubmit} />;
}
