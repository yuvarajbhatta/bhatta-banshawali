-- Baseline migration: reconstructs the schema that Hibernate's
-- `spring.jpa.hibernate.ddl-auto=update` has been generating and evolving
-- from the current entity mappings (Person, Relationship, AppUser).
--
-- IMPORTANT: this file is derived from reading the entity source, not from a
-- `SHOW CREATE TABLE` dump of the live production database. Before this
-- migration is trusted in production (i.e. before `ddl-auto=validate` is
-- allowed to run there), diff this script's output against the real
-- production schema on a copy of the database. On an EXISTING database,
-- Flyway will baseline (mark this version as already applied) rather than
-- execute it, so this script only actually runs on fresh databases
-- (local dev, CI, a new staging instance) -- see `spring.flyway.baseline-on-migrate`
-- in application.properties.

CREATE TABLE persons (
    id                  BIGINT NOT NULL AUTO_INCREMENT,
    generation_number   INT NULL,
    first_name          VARCHAR(100) NOT NULL,
    first_name_nepali   VARCHAR(100) NULL,
    middle_name         VARCHAR(255) NULL,
    middle_name_nepali  VARCHAR(100) NULL,
    last_name           VARCHAR(100) NOT NULL,
    last_name_nepali    VARCHAR(100) NULL,
    nickname            VARCHAR(100) NULL,
    gender              VARCHAR(255) NULL,
    birth_date          DATE NULL,
    death_date          DATE NULL,
    photo_path          VARCHAR(255) NULL,
    birth_place         VARCHAR(255) NULL,
    current_address     VARCHAR(500) NULL,
    notes               VARCHAR(4000) NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE relationships (
    id                  BIGINT NOT NULL AUTO_INCREMENT,
    person_id           BIGINT NOT NULL,
    related_person_id   BIGINT NOT NULL,
    relationship_type   VARCHAR(255) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_relationships_person
        FOREIGN KEY (person_id) REFERENCES persons (id),
    CONSTRAINT fk_relationships_related_person
        FOREIGN KEY (related_person_id) REFERENCES persons (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE app_users (
    id        BIGINT NOT NULL AUTO_INCREMENT,
    username  VARCHAR(255) NOT NULL,
    password  VARCHAR(255) NOT NULL,
    role      VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_app_users_username UNIQUE (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
