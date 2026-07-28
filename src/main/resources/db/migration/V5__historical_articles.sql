-- Admin-managed bilingual content for public pages (About, History,
-- Membership explainer). Only rows with status = 'PUBLISHED' are ever
-- served by the public API (enforced in ContentService, not here).
CREATE TABLE historical_articles (
    id           BIGINT NOT NULL AUTO_INCREMENT,
    slug         VARCHAR(100) NOT NULL,
    title_en     VARCHAR(255) NOT NULL,
    title_ne     VARCHAR(255) NULL,
    body_en      TEXT NOT NULL,
    body_ne      TEXT NULL,
    status       VARCHAR(20) NOT NULL,
    published_at DATETIME NULL,
    created_at   DATETIME NOT NULL,
    updated_at   DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_historical_articles_slug UNIQUE (slug)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
