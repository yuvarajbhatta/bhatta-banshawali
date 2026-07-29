package com.familytree.controller;

import com.familytree.dto.AuditLogEntryDto;
import com.familytree.entity.AuditLogEntry;
import com.familytree.services.AuditLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuditLogApiControllerTest {

    @Mock
    private AuditLogService auditLogService;

    private AdminAuditLogApiController controller() {
        return new AdminAuditLogApiController(auditLogService);
    }

    private AuditLogEntry entry(long id) {
        AuditLogEntry entry = new AuditLogEntry();
        ReflectionTestUtils.setField(entry, "id", id);
        entry.setActorUsername("admin@example.com");
        entry.setAction(AuditLogService.ACTION_PERSON_CREATED);
        entry.setEntityType(AuditLogService.ENTITY_PERSON);
        entry.setEntityId(9L);
        entry.setSummary("Created person Yuva Bhatta");
        entry.setCreatedAt(LocalDateTime.now());
        return entry;
    }

    @Test
    void recentDefaultsToTwoHundred() {
        when(auditLogService.recent(200)).thenReturn(List.of(entry(1L)));

        List<AuditLogEntryDto> result = controller().recent(null);

        verify(auditLogService).recent(200);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).summary()).isEqualTo("Created person Yuva Bhatta");
    }

    @Test
    void recentClampsLimitToMax500() {
        when(auditLogService.recent(500)).thenReturn(List.of());

        controller().recent(10000);

        verify(auditLogService).recent(500);
    }

    @Test
    void recentClampsLimitToMinOne() {
        when(auditLogService.recent(1)).thenReturn(List.of());

        controller().recent(-5);

        verify(auditLogService).recent(1);
    }
}
