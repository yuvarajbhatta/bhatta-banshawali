package com.familytree.controller;

import com.familytree.entity.Person;
import com.familytree.entity.VerificationRequest;
import com.familytree.entity.VerificationStatus;
import com.familytree.repository.PersonRepository;
import com.familytree.repository.VerificationRequestRepository;
import com.familytree.services.VerificationReviewService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminVerificationControllerTest {

    @Mock
    private VerificationRequestRepository verificationRequestRepository;

    @Mock
    private PersonRepository personRepository;

    @Mock
    private VerificationReviewService verificationReviewService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AdminVerificationController controller;

    @Test
    void listDefaultsToPendingStatus() {
        Model model = new ExtendedModelMap();
        when(verificationRequestRepository.findAllByStatusOrderByCreatedAtAsc(VerificationStatus.PENDING))
                .thenReturn(List.of());

        String viewName = controller.list(null, model);

        assertThat(viewName).isEqualTo("admin-signups");
        assertThat(model.getAttribute("selectedStatus")).isEqualTo(VerificationStatus.PENDING);
    }

    @Test
    void listHonorsExplicitStatusFilter() {
        Model model = new ExtendedModelMap();
        when(verificationRequestRepository.findAllByStatusOrderByCreatedAtAsc(VerificationStatus.APPROVED))
                .thenReturn(List.of());

        controller.list(VerificationStatus.APPROVED, model);

        assertThat(model.getAttribute("selectedStatus")).isEqualTo(VerificationStatus.APPROVED);
    }

    @Test
    void detailResolvesCandidatePersonsFromCommaSeparatedIds() {
        VerificationRequest request = new VerificationRequest();
        request.setMatchedCandidatePersonIds("1,2,3");
        when(verificationRequestRepository.findById(5L)).thenReturn(Optional.of(request));
        Person candidate = new Person();
        candidate.setId(1L);
        when(personRepository.findAllById(List.of(1L, 2L, 3L))).thenReturn(List.of(candidate));

        Model model = new ExtendedModelMap();
        String viewName = controller.detail(5L, model);

        assertThat(viewName).isEqualTo("admin-signup-detail");
        assertThat(model.getAttribute("candidatePersons")).isEqualTo(List.of(candidate));
    }

    @Test
    void detailHandlesNoCandidatesWithoutParsingAnything() {
        VerificationRequest request = new VerificationRequest();
        request.setMatchedCandidatePersonIds("");
        when(verificationRequestRepository.findById(6L)).thenReturn(Optional.of(request));

        Model model = new ExtendedModelMap();
        controller.detail(6L, model);

        assertThat(model.getAttribute("candidatePersons")).isEqualTo(List.of());
    }

    @Test
    void approveDelegatesToServiceWithReviewerNameAndRedirects() {
        when(authentication.getName()).thenReturn("admin");

        String viewName = controller.approve(7L, "note", authentication);

        assertThat(viewName).isEqualTo("redirect:/admin/signups");
        verify(verificationReviewService).approve(7L, "admin", "note");
    }

    @Test
    void rejectDelegatesToServiceWithReviewerNameAndRedirects() {
        when(authentication.getName()).thenReturn("admin");

        String viewName = controller.reject(8L, "note", authentication);

        assertThat(viewName).isEqualTo("redirect:/admin/signups");
        verify(verificationReviewService).reject(8L, "admin", "note");
    }

    @Test
    void requestMoreInfoDelegatesToServiceWithReviewerNameAndRedirects() {
        when(authentication.getName()).thenReturn("admin");

        String viewName = controller.requestMoreInfo(9L, "note", authentication);

        assertThat(viewName).isEqualTo("redirect:/admin/signups");
        verify(verificationReviewService).requestMoreInfo(9L, "admin", "note");
    }
}
