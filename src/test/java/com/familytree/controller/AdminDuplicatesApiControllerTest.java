package com.familytree.controller;

import com.familytree.dto.DuplicateCandidateDto;
import com.familytree.dto.MergeRequestDto;
import com.familytree.dto.MergeResultDto;
import com.familytree.services.DuplicateCandidateService;
import com.familytree.services.PersonMergeConflictException;
import com.familytree.services.PersonMergeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDuplicatesApiControllerTest {

    @Mock
    private DuplicateCandidateService duplicateCandidateService;

    @Mock
    private PersonMergeService personMergeService;

    @Mock
    private Authentication authentication;

    private AdminDuplicatesApiController controller() {
        return new AdminDuplicatesApiController(duplicateCandidateService, personMergeService);
    }

    private MergeRequestDto request(Long survivorId, Long loserId) {
        MergeRequestDto request = new MergeRequestDto();
        request.setSurvivorId(survivorId);
        request.setLoserId(loserId);
        return request;
    }

    @Test
    void listDelegatesToService() {
        List<DuplicateCandidateDto> candidates = List.of();
        when(duplicateCandidateService.findCandidates()).thenReturn(candidates);

        assertThat(controller().list()).isEqualTo(candidates);
    }

    @Test
    void mergeDelegatesToServiceWithActorFromAuthentication() {
        when(authentication.getName()).thenReturn("admin@example.com");
        MergeResultDto result = new MergeResultDto(1L, 2, 1, 1, 1, 1);
        when(personMergeService.merge(1L, 2L, "admin@example.com")).thenReturn(result);

        assertThat(controller().merge(request(1L, 2L), authentication)).isEqualTo(result);
    }

    @Test
    void mergeMapsConflictExceptionTo409() {
        when(authentication.getName()).thenReturn("admin@example.com");
        when(personMergeService.merge(1L, 2L, "admin@example.com"))
                .thenThrow(new PersonMergeConflictException("directly related"));

        assertThatThrownBy(() -> controller().merge(request(1L, 2L), authentication))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("directly related");
    }
}
