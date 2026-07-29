-- A member asking an existing admin to grant them the ADMINISTRATOR
-- role -- see docs/08-implementation-roadmap.md Phase 6. Reviewed the
-- same way signup/correction requests are: an admin sees it and
-- approves or denies it.
CREATE TABLE admin_access_requests (
    id                       BIGINT NOT NULL AUTO_INCREMENT,
    user_account_id          BIGINT NOT NULL,
    status                   VARCHAR(20) NOT NULL,
    requested_at             DATETIME NOT NULL,
    reviewed_by_username     VARCHAR(255) NULL,
    reviewed_at              DATETIME NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_admin_access_requests_user
        FOREIGN KEY (user_account_id) REFERENCES user_accounts (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
