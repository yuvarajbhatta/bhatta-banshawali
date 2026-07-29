"use client";

import { useState, type FormEvent } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/Button";
import { Link } from "@/i18n/navigation";
import {
  AdminActionError,
  createAdminRelationship,
  deleteAdminRelationship,
  type AdminRelationshipDto,
  type AdminRelationshipRequest,
} from "@/lib/api";
import { PersonPicker } from "./PersonPicker";
import styles from "./RelationshipManager.module.css";
import queueStyles from "./QueueTable.module.css";

const TYPES: AdminRelationshipRequest["relationshipType"][] = ["FATHER", "MOTHER", "SPOUSE", "CHILD"];

export function RelationshipManager({ initialItems }: { initialItems: AdminRelationshipDto[] }) {
  const t = useTranslations("adminRelationshipsPage");
  const relationT = useTranslations("relationshipType");
  const [items, setItems] = useState(initialItems);
  const [person, setPerson] = useState<{ id: number; name: string } | null>(null);
  const [relatedPerson, setRelatedPerson] = useState<{ id: number; name: string } | null>(null);
  const [type, setType] = useState<AdminRelationshipRequest["relationshipType"]>("FATHER");
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<{ person?: string; relatedPerson?: string }>({});
  const [deletingId, setDeletingId] = useState<number | null>(null);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    const errors: typeof fieldErrors = {};
    if (!person) errors.person = t("errors.personRequired");
    if (!relatedPerson) errors.relatedPerson = t("errors.relatedPersonRequired");
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0 || !person || !relatedPerson) return;

    if (person.id === relatedPerson.id) {
      setFormError(t("errors.samePerson"));
      return;
    }

    setSubmitting(true);
    setFormError(null);
    try {
      const created = await createAdminRelationship({ personId: person.id, relatedPersonId: relatedPerson.id, relationshipType: type });
      setItems((current) => [created, ...current]);
      setPerson(null);
      setRelatedPerson(null);
    } catch (error) {
      if (error instanceof AdminActionError && error.message.includes("already exists")) {
        setFormError(t("errors.duplicate"));
      } else if (error instanceof AdminActionError && error.message.toLowerCase().includes("ancestor")) {
        setFormError(t("errors.cycle"));
      } else {
        setFormError(t("errors.generic"));
      }
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDelete(id: number) {
    if (!window.confirm(t("deleteConfirm"))) return;
    setDeletingId(id);
    try {
      await deleteAdminRelationship(id);
      setItems((current) => current.filter((item) => item.id !== id));
    } finally {
      setDeletingId(null);
    }
  }

  return (
    <div>
      <form className={styles.formCard} onSubmit={handleSubmit}>
        <h2>{t("addRelationship")}</h2>
        <div className={styles.row}>
          <div className={styles.field}>
            <label>{t("person")}</label>
            <PersonPicker
              label={t("person")}
              placeholder={t("selectPerson")}
              clearLabel={t("clear")}
              selected={person}
              onChange={setPerson}
            />
            {fieldErrors.person ? <span className={styles.fieldError}>{fieldErrors.person}</span> : null}
          </div>
          <div className={styles.field}>
            <label>{t("relatedPerson")}</label>
            <PersonPicker
              label={t("relatedPerson")}
              placeholder={t("selectRelatedPerson")}
              clearLabel={t("clear")}
              selected={relatedPerson}
              onChange={setRelatedPerson}
            />
            {fieldErrors.relatedPerson ? <span className={styles.fieldError}>{fieldErrors.relatedPerson}</span> : null}
          </div>
        </div>
        <div className={styles.row}>
          <div className={styles.field}>
            <label htmlFor="relationshipType">{t("type")}</label>
            <select id="relationshipType" value={type} onChange={(event) => setType(event.target.value as typeof type)}>
              {TYPES.map((value) => (
                <option key={value} value={value}>
                  {relationT(value)}
                </option>
              ))}
            </select>
          </div>
        </div>
        <div className={styles.formActions}>
          <Button type="submit" variant="primary" disabled={submitting}>
            {t("save")}
          </Button>
          {formError ? <span className={styles.formError}>{formError}</span> : null}
        </div>
      </form>

      {items.length === 0 ? (
        <div className={queueStyles.empty}>{t("empty")}</div>
      ) : (
        <div className={queueStyles.tableWrapper}>
          <table className={queueStyles.table}>
            <thead>
              <tr>
                <th>{t("columns.person")}</th>
                <th>{t("columns.type")}</th>
                <th>{t("columns.relatedPerson")}</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {items.map((relationship) => (
                <tr key={relationship.id}>
                  <td>
                    <Link href={`/directory/${relationship.personId}`} className={queueStyles.reviewLink}>
                      {relationship.personName}
                    </Link>
                  </td>
                  <td>{relationT(relationship.relationshipType)}</td>
                  <td>
                    <Link href={`/directory/${relationship.relatedPersonId}`} className={queueStyles.reviewLink}>
                      {relationship.relatedPersonName}
                    </Link>
                  </td>
                  <td>
                    <button
                      type="button"
                      className={queueStyles.reviewLink}
                      onClick={() => handleDelete(relationship.id)}
                      disabled={deletingId === relationship.id}
                    >
                      {t("delete")}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
