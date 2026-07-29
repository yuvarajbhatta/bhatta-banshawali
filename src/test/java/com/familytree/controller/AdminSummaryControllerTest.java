package com.familytree.controller;

import com.familytree.dto.AdminSummaryDto;
import com.familytree.entity.CorrectablePersonField;
import com.familytree.entity.CorrectionRequestStatus;
import com.familytree.entity.Person;
import com.familytree.entity.PersonCorrectionRequest;
import com.familytree.entity.VerificationRequest;
import com.familytree.entity.VerificationStatus;
import com.familytree.repository.PersonCorrectionRequestRepository;
import com.familytree.repository.VerificationRequestRepository;
import com.familytree.web.PersonDisplayHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminSummaryControllerTest {

    @Mock
    private VerificationRequestRepository verificationRequestRepository;

    @Mock
    private PersonCorrectionRequestRepository correctionRequestRepository;

    private AdminSummaryController controller() {
        return new AdminSummaryController(verificationRequestRepository, correctionRequestRepository, new PersonDisplayHelper());
    }

    @Test
    void summaryReportsCountsAndRecentItemsForBothQueues() {
        VerificationRequest signup = new VerificationRequest();
        signup.setSubmittedFullName("Yuva Bhatta");
        signup.setCreatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        when(verificationRequestRepository.findAllByStatusOrderByCreatedAtAsc(VerificationStatus.PENDING))
                .thenReturn(List.of(signup));

        Person person = new Person();
        person.setFirstName("Yuva");
        person.setLastName("Bhatta");
        PersonCorrectionRequest correction = new PersonCorrectionRequest();
        correction.setPerson(person);
        correction.setField(CorrectablePersonField.NICKNAME);
        correction.setSubmittedAt(LocalDateTime.of(2026, 1, 2, 11, 0));
        when(correctionRequestRepository.findAllByStatusOrderBySubmittedAtAsc(CorrectionRequestStatus.PENDING))
                .thenReturn(List.of(correction));

        AdminSummaryDto summary = controller().summary();

        assertThat(summary.pendingSignupCount()).isEqualTo(1);
        assertThat(summary.pendingCorrectionCount()).isEqualTo(1);
        assertThat(summary.recentPendingSignups()).hasSize(1);
        assertThat(summary.recentPendingSignups().get(0).submittedFullName()).isEqualTo("Yuva Bhatta");
        assertThat(summary.recentPendingCorrections()).hasSize(1);
        assertThat(summary.recentPendingCorrections().get(0).personName()).isEqualTo("Yuva Bhatta");
        assertThat(summary.recentPendingCorrections().get(0).field()).isEqualTo("NICKNAME");
    }

    @Test
    void summaryLimitsRecentListsToFiveEvenWhenMorePending() {
        List<VerificationRequest> signups = java.util.stream.IntStream.range(0, 8)
                .mapToObj(i -> {
                    VerificationRequest request = new VerificationRequest();
                    request.setSubmittedFullName("Applicant " + i);
                    request.setCreatedAt(LocalDateTime.now());
                    return request;
                })
                .toList();
        when(verificationRequestRepository.findAllByStatusOrderByCreatedAtAsc(VerificationStatus.PENDING))
                .thenReturn(signups);
        when(correctionRequestRepository.findAllByStatusOrderBySubmittedAtAsc(CorrectionRequestStatus.PENDING))
                .thenReturn(List.of());

        AdminSummaryDto summary = controller().summary();

        assertThat(summary.pendingSignupCount()).isEqualTo(8);
        assertThat(summary.recentPendingSignups()).hasSize(5);
    }

    @Test
    void summaryHandlesEmptyQueuesCleanly() {
        when(verificationRequestRepository.findAllByStatusOrderByCreatedAtAsc(VerificationStatus.PENDING))
                .thenReturn(List.of());
        when(correctionRequestRepository.findAllByStatusOrderBySubmittedAtAsc(CorrectionRequestStatus.PENDING))
                .thenReturn(List.of());

        AdminSummaryDto summary = controller().summary();

        assertThat(summary.pendingSignupCount()).isZero();
        assertThat(summary.pendingCorrectionCount()).isZero();
        assertThat(summary.recentPendingSignups()).isEmpty();
        assertThat(summary.recentPendingCorrections()).isEmpty();
    }
}
