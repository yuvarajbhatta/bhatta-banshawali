-- Signup applicant identity/lineage submissions and family-match outcomes
-- -- see docs/05-auth-and-verification.md. Match evidence
-- (matched_candidate_person_ids) is admin-only; nothing here is ever
-- returned to the applicant themselves (enforced in the service/API
-- layer, not by this schema).
CREATE TABLE verification_requests (
    id                            BIGINT NOT NULL AUTO_INCREMENT,
    user_account_id               BIGINT NOT NULL,
    submitted_full_name           VARCHAR(255) NOT NULL,
    submitted_full_name_nepali    VARCHAR(255) NULL,
    submitted_father_name         VARCHAR(255) NOT NULL,
    submitted_grandfather_name    VARCHAR(255) NOT NULL,
    submitted_dob_ad               DATE NULL,
    submitted_dob_bs_year          INT NULL,
    submitted_dob_bs_month         INT NULL,
    submitted_dob_bs_day           INT NULL,
    mother_name                   VARCHAR(255) NULL,
    place_of_birth                VARCHAR(255) NULL,
    ancestral_village             VARCHAR(255) NULL,
    family_branch                 VARCHAR(255) NULL,
    known_relative_name           VARCHAR(255) NULL,
    invitation_code               VARCHAR(100) NULL,
    applicant_note                VARCHAR(2000) NULL,
    match_confidence              VARCHAR(20) NOT NULL,
    matched_candidate_person_ids  VARCHAR(500) NULL,
    status                        VARCHAR(20) NOT NULL,
    reviewed_by_user_account_id   BIGINT NULL,
    reviewed_at                   DATETIME NULL,
    decision_note                 VARCHAR(2000) NULL,
    created_at                    DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_verification_requests_user
        FOREIGN KEY (user_account_id) REFERENCES user_accounts (id),
    CONSTRAINT fk_verification_requests_reviewed_by
        FOREIGN KEY (reviewed_by_user_account_id) REFERENCES user_accounts (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
