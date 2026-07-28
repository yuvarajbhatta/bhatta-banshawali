package com.familytree.controller;

import com.familytree.entity.CorrectionRequestStatus;
import com.familytree.repository.PersonCorrectionRequestRepository;
import com.familytree.services.PersonCorrectionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCorrectionControllerTest {

    @Mock
    private PersonCorrectionRequestRepository correctionRequestRepository;

    @Mock
    private PersonCorrectionService personCorrectionService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AdminCorrectionController controller;

    @Test
    void listDefaultsToPendingStatus() {
        Model model = new ExtendedModelMap();
        when(correctionRequestRepository.findAllByStatusOrderBySubmittedAtAsc(CorrectionRequestStatus.PENDING))
                .thenReturn(List.of());

        String viewName = controller.list(null, model);

        assertThat(viewName).isEqualTo("admin-corrections");
        assertThat(model.getAttribute("selectedStatus")).isEqualTo(CorrectionRequestStatus.PENDING);
    }

    @Test
    void listHonorsExplicitStatusFilter() {
        Model model = new ExtendedModelMap();
        when(correctionRequestRepository.findAllByStatusOrderBySubmittedAtAsc(CorrectionRequestStatus.APPROVED))
                .thenReturn(List.of());

        controller.list(CorrectionRequestStatus.APPROVED, model);

        assertThat(model.getAttribute("selectedStatus")).isEqualTo(CorrectionRequestStatus.APPROVED);
    }

    @Test
    void approveDelegatesToServiceWithReviewerNameAndRedirects() {
        when(authentication.getName()).thenReturn("admin");

        String viewName = controller.approve(1L, "note", authentication);

        assertThat(viewName).isEqualTo("redirect:/admin/corrections");
        verify(personCorrectionService).approve(1L, "admin", "note");
    }

    @Test
    void rejectDelegatesToServiceWithReviewerNameAndRedirects() {
        when(authentication.getName()).thenReturn("admin");

        String viewName = controller.reject(2L, "note", authentication);

        assertThat(viewName).isEqualTo("redirect:/admin/corrections");
        verify(personCorrectionService).reject(2L, "admin", "note");
    }
}
