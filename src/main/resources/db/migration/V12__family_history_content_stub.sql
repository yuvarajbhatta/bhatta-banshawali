-- A draft placeholder for the public /history page, which previously
-- had no HistoricalArticle row at all and just showed a hardcoded
-- "pending" message -- see docs/08-implementation-roadmap.md Phase 6
-- content management. Deliberately left as an empty prompt, not
-- invented content: real family history should be written by an
-- administrator via the new content editor (see
-- docs/07-migration-plan.md "Do not invent historical facts").
INSERT INTO historical_articles (slug, title_en, title_ne, body_en, body_ne, status, published_at, created_at, updated_at)
VALUES (
    'family-history',
    'Family History',
    'पारिवारिक इतिहास',
    'This page is ready for the family''s history to be written. Use the content editor in the admin panel to add it, then publish this article when it''s ready to be public.',
    NULL,
    'DRAFT',
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
