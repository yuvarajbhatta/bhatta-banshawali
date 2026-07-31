-- Additive column for the new father-name-based match strategy
-- (FamilyMatchService's "existing person" vs "new person, child of an
-- existing father" strategies) -- comma-separated Person IDs of candidate
-- FATHERS the matcher found by searching on the applicant's stated
-- father's name, distinct from matched_candidate_person_ids (which holds
-- candidates for the applicant's OWN name). Admin-only, same convention as
-- that column: never returned to the applicant.
--
-- Both candidate-ID columns are widened from VARCHAR(500) to VARCHAR(4000)
-- here: a real signup against this app's own seed data (2000+ Persons, all
-- sharing one surname) overflowed VARCHAR(500) outright once fuzzy name
-- matching was added to FamilyMatchService, since a family tree with a
-- shared surname and a small pool of recurring first names can plausibly
-- produce dozens of matching candidates. The matcher's fuzzy threshold was
-- also tightened separately (NameMatcher) to cut down how often that
-- happens, but the column itself needs headroom regardless.
ALTER TABLE verification_requests
    MODIFY COLUMN matched_candidate_person_ids VARCHAR(4000) NULL;

ALTER TABLE verification_requests
    ADD COLUMN matched_father_candidate_person_ids VARCHAR(4000) NULL
    AFTER matched_candidate_person_ids;
