-- Optional clan/lineage identifier a member can record on their own
-- Person (docs/06-ui-ux-specification.md dashboard "optional fields").
-- Nullable, no default: most existing rows won't have this recorded.
ALTER TABLE persons
    ADD COLUMN gotra VARCHAR(100) NULL;
