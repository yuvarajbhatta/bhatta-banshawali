package com.familytree.services;

import com.familytree.entity.AuditLogEntry;
import com.familytree.repository.AuditLogRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    private AuditLogService service() {
        return new AuditLogService(auditLogRepository);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recordWithExplicitActorSavesEntry() {
        service().record(AuditLogService.ACTION_SIGNUP_APPROVED, AuditLogService.ENTITY_VERIFICATION_REQUEST, 5L,
                "Approved signup for Yuva Bhatta", "admin@example.com");

        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLogEntry saved = captor.getValue();
        assertThat(saved.getActorUsername()).isEqualTo("admin@example.com");
        assertThat(saved.getAction()).isEqualTo(AuditLogService.ACTION_SIGNUP_APPROVED);
        assertThat(saved.getEntityType()).isEqualTo(AuditLogService.ENTITY_VERIFICATION_REQUEST);
        assertThat(saved.getEntityId()).isEqualTo(5L);
        assertThat(saved.getSummary()).isEqualTo("Approved signup for Yuva Bhatta");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void recordWithNullActorFallsBackToSystem() {
        service().record(AuditLogService.ACTION_PERSON_CREATED, AuditLogService.ENTITY_PERSON, 1L, "Created person Yuva Bhatta", null);

        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getActorUsername()).isEqualTo("system");
    }

    @Test
    void recordWithoutExplicitActorResolvesFromSecurityContext() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("admin", "n/a"));

        service().record(AuditLogService.ACTION_PERSON_UPDATED, AuditLogService.ENTITY_PERSON, 2L, "Updated person Yuva Bhatta");

        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getActorUsername()).isEqualTo("admin");
    }

    @Test
    void recordWithoutExplicitActorFallsBackToSystemWhenUnauthenticated() {
        service().record(AuditLogService.ACTION_PERSON_DELETED, AuditLogService.ENTITY_PERSON, 3L, "Deleted person Yuva Bhatta");

        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getActorUsername()).isEqualTo("system");
    }

    @Test
    void recentDelegatesToRepositoryWithLimit() {
        when(auditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 50))).thenReturn(java.util.List.of());

        assertThat(service().recent(50)).isEmpty();
    }
}
