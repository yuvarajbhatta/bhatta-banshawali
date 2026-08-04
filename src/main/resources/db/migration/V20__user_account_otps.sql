-- Backs email-verification and admin-access-request confirmation via a
-- 6-digit one-time code sent by email, rather than a clickable link -- see
-- com.familytree.services.OtpService. Parallel to user_account_tokens
-- (same hash/expire/single-use mechanics) but with an attempt counter,
-- since a short numeric code is guessable within a small number of tries
-- if attempts weren't capped. `code_hash` is a hex SHA-256 digest; the raw
-- code is never persisted, only ever held in memory long enough to email.
CREATE TABLE user_account_otps (
    id               BIGINT NOT NULL AUTO_INCREMENT,
    user_account_id  BIGINT NOT NULL,
    purpose          VARCHAR(30) NOT NULL,
    code_hash        CHAR(64) NOT NULL,
    attempt_count    INT NOT NULL DEFAULT 0,
    created_at       DATETIME NOT NULL,
    expires_at       DATETIME NOT NULL,
    consumed_at      DATETIME NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_user_account_otps_user
        FOREIGN KEY (user_account_id) REFERENCES user_accounts (id),
    INDEX idx_user_account_otps_account_purpose (user_account_id, purpose)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
