package com.familytree.services;

import com.familytree.entity.AuditLogEntry;
import com.familytree.repository.AuditLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Records admin actions (docs/08 Phase 6) -- called from the service
 * layer (PersonService, RelationshipService, VerificationReviewService,
 * PersonCorrectionService), not the controller layer, so it captures
 * actions from both the legacy Thymeleaf admin pages and the newer REST
 * admin API. PersonService/RelationshipService don't otherwise know who
 * the caller is (Thymeleaf controllers don't pass a reviewer/actor
 * parameter the way the verification/correction services already did),
 * so the actor is resolved from SecurityContextHolder here rather than
 * threaded through every call site.
 */
@Service
public class AuditLogService {

    public static final String ACTION_PERSON_CREATED = "PERSON_CREATED";
    public static final String ACTION_PERSON_UPDATED = "PERSON_UPDATED";
    public static final String ACTION_PERSON_DELETED = "PERSON_DELETED";
    public static final String ACTION_PERSON_MERGED = "PERSON_MERGED";
    public static final String ACTION_RELATIONSHIP_CREATED = "RELATIONSHIP_CREATED";
    public static final String ACTION_RELATIONSHIP_UPDATED = "RELATIONSHIP_UPDATED";
    public static final String ACTION_RELATIONSHIP_DELETED = "RELATIONSHIP_DELETED";
    public static final String ACTION_SIGNUP_APPROVED = "SIGNUP_APPROVED";
    public static final String ACTION_SIGNUP_REJECTED = "SIGNUP_REJECTED";
    public static final String ACTION_SIGNUP_MORE_INFO_REQUESTED = "SIGNUP_MORE_INFO_REQUESTED";
    public static final String ACTION_CORRECTION_APPROVED = "CORRECTION_APPROVED";
    public static final String ACTION_CORRECTION_REJECTED = "CORRECTION_REJECTED";
    public static final String ACTION_ACCOUNT_LINKED = "ACCOUNT_LINKED";
    public static final String ACTION_ACCOUNT_UNLINKED = "ACCOUNT_UNLINKED";
    public static final String ACTION_ACCOUNT_DISABLED = "ACCOUNT_DISABLED";
    public static final String ACTION_ACCOUNT_ENABLED = "ACCOUNT_ENABLED";
    public static final String ACTION_ACCOUNT_DELETED = "ACCOUNT_DELETED";
    public static final String ACTION_ACCOUNT_SIGNUP_INFO_EDITED = "ACCOUNT_SIGNUP_INFO_EDITED";
    public static final String ACTION_ADMIN_ACCESS_REQUESTED = "ADMIN_ACCESS_REQUESTED";
    public static final String ACTION_ADMIN_ACCESS_APPROVED = "ADMIN_ACCESS_APPROVED";
    public static final String ACTION_ADMIN_ACCESS_DENIED = "ADMIN_ACCESS_DENIED";
    public static final String ACTION_ADMIN_ACCESS_REVOKED = "ADMIN_ACCESS_REVOKED";
    public static final String ACTION_CONTENT_CREATED = "CONTENT_CREATED";
    public static final String ACTION_CONTENT_UPDATED = "CONTENT_UPDATED";
    public static final String ACTION_CONTENT_STATUS_CHANGED = "CONTENT_STATUS_CHANGED";
    public static final String ACTION_CONTENT_DELETED = "CONTENT_DELETED";
    // Only a moderation takedown (an admin removing someone else's
    // upload) is logged -- see PersonPhotoService#delete.
    public static final String ACTION_PHOTO_DELETED = "PHOTO_DELETED";

    public static final String ENTITY_PERSON = "PERSON";
    public static final String ENTITY_RELATIONSHIP = "RELATIONSHIP";
    public static final String ENTITY_VERIFICATION_REQUEST = "VERIFICATION_REQUEST";
    public static final String ENTITY_CORRECTION_REQUEST = "CORRECTION_REQUEST";
    public static final String ENTITY_USER_ACCOUNT = "USER_ACCOUNT";
    public static final String ENTITY_CONTENT = "CONTENT";
    public static final String ENTITY_PERSON_PHOTO = "PERSON_PHOTO";

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /** Resolves the actor from the current security context (Thymeleaf-originated writes). */
    public void record(String action, String entityType, Long entityId, String summary) {
        record(action, entityType, entityId, summary, resolveCurrentActor());
    }

    /** Explicit actor (REST/service call sites that already know who's acting, e.g. an admin reviewer username). */
    public void record(String action, String entityType, Long entityId, String summary, String actorUsername) {
        AuditLogEntry entry = new AuditLogEntry();
        entry.setActorUsername(actorUsername != null ? actorUsername : "system");
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setSummary(summary);
        entry.setCreatedAt(LocalDateTime.now());
        auditLogRepository.save(entry);
    }

    public List<AuditLogEntry> recent(int limit) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit));
    }

    private String resolveCurrentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : "system";
    }
}
