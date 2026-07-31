-- Backs "forgot password" and "email verification". One shared table for
-- both purposes (discriminated by `purpose`) since they need identical
-- generate/hash/expire/single-use/consume mechanics -- see
-- com.familytree.services.TokenService. `token_hash` is a hex SHA-256
-- digest; the raw token is never persisted, only ever held in memory long
-- enough to put in the email link.
ALTER TABLE user_accounts ADD COLUMN email_verified_at DATETIME NULL;

CREATE TABLE user_account_tokens (
    id               BIGINT NOT NULL AUTO_INCREMENT,
    user_account_id  BIGINT NOT NULL,
    purpose          VARCHAR(30) NOT NULL,
    token_hash       CHAR(64) NOT NULL,
    created_at       DATETIME NOT NULL,
    expires_at       DATETIME NOT NULL,
    consumed_at      DATETIME NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_user_account_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_user_account_tokens_user
        FOREIGN KEY (user_account_id) REFERENCES user_accounts (id),
    INDEX idx_user_account_tokens_account_purpose (user_account_id, purpose)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
