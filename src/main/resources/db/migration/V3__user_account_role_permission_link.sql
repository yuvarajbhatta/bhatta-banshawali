-- Introduces UserAccount/Role/Permission/UserPersonLink alongside the
-- existing AppUser table (not replacing it yet -- see
-- docs/07-migration-plan.md and docs/08-implementation-roadmap.md). Nothing
-- reads or writes these tables yet; the authentication/authorization cutover
-- happens in a later phase once the signup and verification workflow needs
-- them.

CREATE TABLE roles (
    id          BIGINT NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50) NOT NULL,
    description VARCHAR(255) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_roles_name UNIQUE (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE permissions (
    id          BIGINT NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_permissions_name UNIQUE (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE role_permissions (
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role
        FOREIGN KEY (role_id) REFERENCES roles (id),
    CONSTRAINT fk_role_permissions_permission
        FOREIGN KEY (permission_id) REFERENCES permissions (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE user_accounts (
    id                BIGINT NOT NULL AUTO_INCREMENT,
    email             VARCHAR(255) NOT NULL,
    password_hash     VARCHAR(255) NOT NULL,
    status            VARCHAR(40) NOT NULL,
    preferred_language VARCHAR(10) NULL,
    mfa_enabled       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at        DATETIME NOT NULL,
    last_login_at     DATETIME NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_accounts_email UNIQUE (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE user_account_roles (
    user_account_id BIGINT NOT NULL,
    role_id         BIGINT NOT NULL,
    PRIMARY KEY (user_account_id, role_id),
    CONSTRAINT fk_user_account_roles_user
        FOREIGN KEY (user_account_id) REFERENCES user_accounts (id),
    CONSTRAINT fk_user_account_roles_role
        FOREIGN KEY (role_id) REFERENCES roles (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- user_account_id and person_id are both nullable: a pending applicant may
-- not yet be linked to a person, and this table -- not a foreign key on
-- Person or UserAccount directly -- is what represents the link, so that
-- neither entity gains access to the other merely by existing.
CREATE TABLE user_person_links (
    id                        BIGINT NOT NULL AUTO_INCREMENT,
    user_account_id           BIGINT NULL,
    person_id                 BIGINT NULL,
    link_status               VARCHAR(20) NOT NULL,
    verified_by_user_account_id BIGINT NULL,
    verified_at               DATETIME NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_user_person_links_user
        FOREIGN KEY (user_account_id) REFERENCES user_accounts (id),
    CONSTRAINT fk_user_person_links_person
        FOREIGN KEY (person_id) REFERENCES persons (id),
    CONSTRAINT fk_user_person_links_verified_by
        FOREIGN KEY (verified_by_user_account_id) REFERENCES user_accounts (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
