package com.familytree.controller;

import com.familytree.dto.AdminSignupDecisionRequestDto;
import com.familytree.dto.AdminSignupDetailDto;
import com.familytree.dto.AdminSignupSummaryDto;
import com.familytree.entity.MatchConfidence;
import com.familytree.entity.Person;
import com.familytree.entity.VerificationRequest;
import com.familytree.entity.VerificationStatus;
import com.familytree.repository.PersonRepository;
import com.familytree.repository.UserAccountRepository;
import com.familytree.repository.UserPersonLinkRepository;
import com.familytree.repository.VerificationRequestRepository;
import com.familytree.services.PersonProfileAssembler;
import com.familytree.services.RelationshipService;
import com.familytree.services.VerificationReviewService;
import com.familytree.services.ViewerContextResolver;
import com.familytree.web.PersonDisplayHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
class AdminVerificationApiControllerTest {

    @Mock
    private VerificationRequestRepository verificationRequestRepository;

    @Mock
    private PersonRepository personRepository;

    @Mock
    private VerificationReviewService verificationReviewService;

    @Mock
    private RelationshipService relationshipService;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private UserPersonLinkRepository userPersonLinkRepository;

    @Mock
    private Authentication authentication;

    private AdminVerificationApiController controller() {
        PersonProfileAssembler assembler = new PersonProfileAssembler(relationshipService, new PersonDisplayHelper());
        ViewerContextResolver viewerContextResolver = new ViewerContextResolver(userAccountRepository, userPersonLinkRepository);
        return new AdminVerificationApiController(verificationRequestRepository, personRepository,
                verificationReviewService, assembler, viewerContextResolver);
    }

    private Authentication asAdmin() {
        when(authentication.getName()).thenReturn("admin@example.com");
        // lenient: not every test path resolves a ViewerContext (list() never does)
        org.mockito.Mockito.lenient().doReturn(List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(authentication).getAuthorities();
        org.mockito.Mockito.lenient().when(userAccountRepository.findByEmail("admin@example.com")).thenReturn(Optional.empty());
        return authentication;
    }

    private VerificationRequest request(Long id) {
        VerificationRequest request = new VerificationRequest();
        ReflectionTestUtils.setField(request, "id", id);
        request.setSubmittedFullName("Yuva Bhatta");
        request.setSubmittedFatherName("Bhoj Bhatta");
        request.setSubmittedGrandfatherName("Rana Bhatta");
        request.setMatchConfidence(MatchConfidence.MEDIUM);
        request.setStatus(VerificationStatus.PENDING);
        request.setCreatedAt(LocalDateTime.now());
        return request;
    }

    @Test
    void listDefaultsToPendingStatus() {
        when(verificationRequestRepository.findAllByStatusOrderByCreatedAtAsc(VerificationStatus.PENDING))
                .thenReturn(List.of(request(1L)));

        List<AdminSignupSummaryDto> results = controller().list(null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).submittedFullName()).isEqualTo("Yuva Bhatta");
        assertThat(results.get(0).status()).isEqualTo(VerificationStatus.PENDING);
    }

    @Test
    void listHonorsExplicitStatusFilter() {
        when(verificationRequestRepository.findAllByStatusOrderByCreatedAtAsc(VerificationStatus.APPROVED))
                .thenReturn(List.of());

        List<AdminSignupSummaryDto> results = controller().list(VerificationStatus.APPROVED);

        assertThat(results).isEmpty();
    }

    @Test
    void detailResolvesCandidatePersonsFromCommaSeparatedIds() {
        VerificationRequest request = request(5L);
        request.setMatchedCandidatePersonIds("1, 2");
        when(verificationRequestRepository.findById(5L)).thenReturn(Optional.of(request));

        Person candidate1 = new Person();
        candidate1.setId(1L);
        candidate1.setFirstName("Bhoj");
        candidate1.setLastName("Bhatta");
        Person candidate2 = new Person();
        candidate2.setId(2L);
        candidate2.setFirstName("Rana");
        candidate2.setLastName("Bhatta");
        when(personRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(candidate1, candidate2));

        AdminSignupDetailDto detail = controller().detail(5L, asAdmin());

        assertThat(detail.candidates()).hasSize(2);
        assertThat(detail.candidates().get(0).englishFullName()).isEqualTo("Bhoj Bhatta");
    }

    @Test
    void detailResolvesFatherCandidatesFromTheNewColumnSeparatelyFromExistingCandidates() {
        VerificationRequest request = request(6L);
        request.setMatchedCandidatePersonIds("1");
        request.setMatchedFatherCandidatePersonIds("2");
        when(verificationRequestRepository.findById(6L)).thenReturn(Optional.of(request));

        Person existingCandidate = new Person();
        existingCandidate.setId(1L);
        existingCandidate.setFirstName("Yuva");
        existingCandidate.setLastName("Bhatta");
        when(personRepository.findAllById(List.of(1L))).thenReturn(List.of(existingCandidate));

        Person fatherCandidate = new Person();
        fatherCandidate.setId(2L);
        fatherCandidate.setFirstName("Bhoj");
        fatherCandidate.setLastName("Bhatta");
        when(personRepository.findAllById(List.of(2L))).thenReturn(List.of(fatherCandidate));

        AdminSignupDetailDto detail = controller().detail(6L, asAdmin());

        assertThat(detail.candidates()).extracting(p -> p.id()).containsExactly(1L);
        assertThat(detail.fatherCandidates()).extracting(p -> p.id()).containsExactly(2L);
    }

    @Test
    void detailThrows404WhenRequestDoesNotExist() {
        when(verificationRequestRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller().detail(999L, authentication))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Verification request not found");
    }

    @Test
    void approveDelegatesToServiceWithLinkedPersonIdAndReturnsUpdatedDetail() {
        VerificationRequest request = request(7L);
        when(verificationRequestRepository.findById(7L)).thenReturn(Optional.of(request));

        AdminSignupDecisionRequestDto body = new AdminSignupDecisionRequestDto();
        body.setDecisionNote("Looks right");
        body.setLinkedPersonId(42L);

        controller().approve(7L, body, asAdmin());

        verify(verificationReviewService).approve(7L, "admin@example.com", "Looks right", 42L, null);
    }

    @Test
    void approveWithNoBodyStillWorks() {
        VerificationRequest request = request(7L);
        when(verificationRequestRepository.findById(7L)).thenReturn(Optional.of(request));

        controller().approve(7L, null, asAdmin());

        verify(verificationReviewService).approve(7L, "admin@example.com", null, null, null);
    }

    @Test
    void approveDelegatesToServiceWithCreateAsChildOfFatherId() {
        VerificationRequest request = request(7L);
        when(verificationRequestRepository.findById(7L)).thenReturn(Optional.of(request));

        AdminSignupDecisionRequestDto body = new AdminSignupDecisionRequestDto();
        body.setDecisionNote("Confirmed via grandfather match");
        body.setCreateAsChildOfFatherId(99L);

        controller().approve(7L, body, asAdmin());

        verify(verificationReviewService).approve(7L, "admin@example.com", "Confirmed via grandfather match", null, 99L);
    }

    @Test
    void rejectDelegatesToService() {
        VerificationRequest request = request(8L);
        when(verificationRequestRepository.findById(8L)).thenReturn(Optional.of(request));

        AdminSignupDecisionRequestDto body = new AdminSignupDecisionRequestDto();
        body.setDecisionNote("No match found");

        controller().reject(8L, body, asAdmin());

        verify(verificationReviewService).reject(8L, "admin@example.com", "No match found");
    }

    @Test
    void requestMoreInfoDelegatesToService() {
        VerificationRequest request = request(9L);
        when(verificationRequestRepository.findById(9L)).thenReturn(Optional.of(request));

        controller().requestMoreInfo(9L, null, asAdmin());

        verify(verificationReviewService).requestMoreInfo(9L, "admin@example.com", null);
    }
}
