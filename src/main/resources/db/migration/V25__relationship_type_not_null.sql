-- relationship_type was nullable at the DB layer (V1__baseline_schema.sql)
-- despite every write path (RelationshipService) always setting it -- a
-- relationship row with no type is meaningless to every reader (nothing
-- filters or interprets it). Tightens the schema to match what the app
-- has always actually written. If this fails on deploy, it means a NULL-
-- typed row genuinely exists and needs manual triage (not something to
-- guess-fix here) -- see docs/09-security-threat-model.md.
ALTER TABLE relationships
    MODIFY COLUMN relationship_type VARCHAR(255) NOT NULL;
