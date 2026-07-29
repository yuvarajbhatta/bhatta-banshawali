-- A general-purpose record of admin actions (docs/08 Phase 6) -- who did
-- what to which record and when. Written by AuditLogService, called from
-- the service layer (PersonService, RelationshipService,
-- VerificationReviewService, PersonCorrectionService) so it captures
-- actions taken through either the legacy Thymeleaf admin pages or the
-- newer REST admin UI, not just one or the other.
CREATE TABLE audit_log_entries (
    id                BIGINT NOT NULL AUTO_INCREMENT,
    actor_username    VARCHAR(255) NOT NULL,
    action            VARCHAR(50) NOT NULL,
    entity_type       VARCHAR(50) NOT NULL,
    entity_id         BIGINT NULL,
    summary           VARCHAR(1000) NOT NULL,
    created_at        DATETIME NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_audit_log_entries_created_at ON audit_log_entries (created_at);
