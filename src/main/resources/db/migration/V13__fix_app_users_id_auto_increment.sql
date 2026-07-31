-- The live production app_users.id column has always lacked AUTO_INCREMENT,
-- even though V1__baseline_schema.sql declared it -- the baseline migration
-- was a snapshot of the intended schema, not a byte-for-byte capture of the
-- table as it actually existed pre-Flyway, and this drift went unnoticed.
-- Dormant because the only insert path (AdminUserInitializer's
-- registerAdminIfMissing/registerUserIfMissing) always no-ops against an
-- existing username in every real deploy so far -- see docs/07-migration-plan.md.
-- MySQL sets the AUTO_INCREMENT counter to MAX(id)+1 automatically here since
-- id is already the PRIMARY KEY with existing rows.
ALTER TABLE app_users
    MODIFY id BIGINT NOT NULL AUTO_INCREMENT;
