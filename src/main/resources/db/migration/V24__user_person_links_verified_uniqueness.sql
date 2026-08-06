-- UserPersonLinkService.createVerifiedLink enforced "at most one VERIFIED
-- link per person, at most one per account" purely at the application
-- layer (a SELECT check before the INSERT), which is a TOCTOU race: two
-- concurrent requests can both pass the check before either commits.
-- MySQL has no native partial/filtered unique index, so this uses the
-- standard workaround -- a generated column that is NULL unless
-- link_status = 'VERIFIED' (NULLs never collide in a unique index), then
-- a real unique constraint on that column. Non-VERIFIED rows (PENDING,
-- UNLINKED) are entirely unconstrained, matching existing behavior.
ALTER TABLE user_person_links
    ADD COLUMN verified_user_account_id BIGINT
        GENERATED ALWAYS AS (CASE WHEN link_status = 'VERIFIED' THEN user_account_id END) VIRTUAL,
    ADD COLUMN verified_person_id BIGINT
        GENERATED ALWAYS AS (CASE WHEN link_status = 'VERIFIED' THEN person_id END) VIRTUAL,
    ADD CONSTRAINT uk_user_person_links_verified_account UNIQUE (verified_user_account_id),
    ADD CONSTRAINT uk_user_person_links_verified_person UNIQUE (verified_person_id);
