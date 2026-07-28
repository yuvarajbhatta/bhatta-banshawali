-- Member-submitted "this field about this person is wrong" requests --
-- see docs/08-implementation-roadmap.md Phase 4. Deliberately one
-- Person field per row, not the full multi-entity-type ChangeRequest
-- described in docs/04-data-model.md's target schema.
CREATE TABLE person_correction_requests (
    id                            BIGINT NOT NULL AUTO_INCREMENT,
    person_id                     BIGINT NOT NULL,
    submitted_by_user_account_id  BIGINT NOT NULL,
    field                         VARCHAR(30) NOT NULL,
    current_value_snapshot        VARCHAR(1000) NULL,
    proposed_value                VARCHAR(1000) NOT NULL,
    reason                        VARCHAR(1000) NOT NULL,
    status                        VARCHAR(20) NOT NULL,
    submitted_at                  DATETIME NOT NULL,
    reviewed_by_username          VARCHAR(255) NULL,
    reviewed_at                   DATETIME NULL,
    decision_note                 VARCHAR(2000) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_person_correction_requests_person
        FOREIGN KEY (person_id) REFERENCES persons (id),
    CONSTRAINT fk_person_correction_requests_submitted_by
        FOREIGN KEY (submitted_by_user_account_id) REFERENCES user_accounts (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
