package com.familytree.controller;

import com.familytree.dto.AdminCorrectionSummaryDto;
import com.familytree.dto.AdminDecisionRequestDto;
import com.familytree.entity.CorrectablePersonField;
import com.familytree.entity.CorrectionRequestStatus;
import com.familytree.entity.Person;
import com.familytree.entity.PersonCorrectionRequest;
import com.familytree.entity.UserAccount;
import com.familytree.repository.PersonCorrectionRequestRepository;
import com.familytree.services.PersonCorrectionService;
import com.familytree.web.PersonDisplayHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCorrectionApiControllerTest {

    @Mock
    private PersonCorrectionRequestRepository correctionRequestRepository;

    @Mock
    private PersonCorrectionService personCorrectionService;

    @Mock
    private Authentication authentication;

    private AdminCorrectionApiController controller() {
        return new AdminCorrectionApiController(correctionRequestRepository, personCorrectionService, new PersonDisplayHelper());
    }

    private PersonCorrectionRequest request(Long id) {
        Person person = new Person();
        person.setId(3L);
        person.setFirstName("Yuva");
        person.setLastName("Bhatta");

        UserAccount submitter = new UserAccount();
        submitter.setEmail("member@example.com");

        PersonCorrectionRequest request = new PersonCorrectionRequest();
        ReflectionTestUtils.setField(request, "id", id);
        request.setPerson(person);
        request.setSubmittedBy(submitter);
        request.setField(CorrectablePersonField.NICKNAME);
        request.setCurrentValueSnapshot("Old Nick");
        request.setProposedValue("New Nick");
        request.setReason("Family always called him this.");
        request.setStatus(CorrectionRequestStatus.PENDING);
        request.setSubmittedAt(LocalDateTime.now());
        return request;
    }

    @Test
    void listDefaultsToPendingStatus() {
        when(correctionRequestRepository.findAllByStatusOrderBySubmittedAtAsc(CorrectionRequestStatus.PENDING))
                .thenReturn(List.of(request(1L)));

        List<AdminCorrectionSummaryDto> results = controller().list(null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).personName()).isEqualTo("Yuva Bhatta");
        assertThat(results.get(0).submittedByEmail()).isEqualTo("member@example.com");
        assertThat(results.get(0).field()).isEqualTo(CorrectablePersonField.NICKNAME);
    }

    @Test
    void listHonorsExplicitStatusFilter() {
        when(correctionRequestRepository.findAllByStatusOrderBySubmittedAtAsc(CorrectionRequestStatus.APPROVED))
                .thenReturn(List.of());

        assertThat(controller().list(CorrectionRequestStatus.APPROVED)).isEmpty();
    }

    @Test
    void approveDelegatesToServiceAndReturnsRefreshedSummary() {
        when(authentication.getName()).thenReturn("admin@example.com");
        PersonCorrectionRequest request = request(4L);
        when(correctionRequestRepository.findById(4L)).thenReturn(Optional.of(request));

        AdminDecisionRequestDto body = new AdminDecisionRequestDto();
        body.setDecisionNote("Confirmed with family");

        AdminCorrectionSummaryDto result = controller().approve(4L, body, authentication);

        verify(personCorrectionService).approve(4L, "admin@example.com", "Confirmed with family");
        assertThat(result.id()).isEqualTo(4L);
    }

    @Test
    void rejectDelegatesToService() {
        when(authentication.getName()).thenReturn("admin@example.com");
        when(correctionRequestRepository.findById(6L)).thenReturn(Optional.of(request(6L)));

        controller().reject(6L, null, authentication);

        verify(personCorrectionService).reject(6L, "admin@example.com", null);
    }

    @Test
    void approveThrows404WhenRequestMissingAfterServiceCall() {
        when(authentication.getName()).thenReturn("admin@example.com");
        when(correctionRequestRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller().approve(999L, null, authentication))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Correction request not found");
    }
}
