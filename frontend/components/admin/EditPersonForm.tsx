"use client";

import { useTranslations } from "next-intl";
import { useRouter } from "@/i18n/navigation";
import { updateAdminPerson, type AdminPersonDto, type AdminPersonRequest } from "@/lib/api";
import { PersonForm } from "./PersonForm";

export function EditPersonForm({ person }: { person: AdminPersonDto }) {
  const t = useTranslations("adminPersonsPage");
  const router = useRouter();

  async function handleSubmit(body: AdminPersonRequest) {
    await updateAdminPerson(person.id, body);
    router.push("/admin/persons");
    router.refresh();
  }

  return <PersonForm initial={person} submitLabel={t("save")} onSubmit={handleSubmit} />;
}
