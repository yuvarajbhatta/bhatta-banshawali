-- Adds a DB-level guarantee that the same (person, related person, type) edge
-- cannot exist twice. Until now this was only enforced by an application-level
-- existsBy check (RelationshipService.saveIfMissing), which does not protect
-- against races or direct data edits. Because the real production data has
-- not been inspected for existing duplicates (see docs/07-migration-plan.md),
-- this migration first removes any duplicate rows -- keeping the
-- lowest-id (oldest) row per group -- before adding the constraint, so it
-- does not fail if duplicates already exist.
DELETE FROM relationships
WHERE id NOT IN (
    SELECT keep_id FROM (
        SELECT MIN(id) AS keep_id
        FROM relationships
        GROUP BY person_id, related_person_id, relationship_type
    ) AS rows_to_keep
);

ALTER TABLE relationships
    ADD CONSTRAINT uk_relationships_person_related_type
    UNIQUE (person_id, related_person_id, relationship_type);
