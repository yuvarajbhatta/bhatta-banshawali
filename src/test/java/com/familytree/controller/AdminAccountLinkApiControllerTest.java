package com.familytree.controller;

import com.familytree.dto.LinkAccountRequestDto;
import com.familytree.dto.UnlinkedAccountDto;
import com.familytree.entity.UserAccount;
import com.familytree.entity.VerificationRequest;
import com.familytree.repository.VerificationRequestRepository;
import com.familytree.services.VerificationReviewService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAccountLinkApiControllerTest {

    @Mock
    private VerificationReviewService verificationReviewService;

    @Mock
    private VerificationRequestRepository verificationRequestRepository;

    @Mock
    private Authentication authentication;

    private AdminAccountLinkApiController controller() {
        return new AdminAccountLinkApiController(verificationReviewService, verificationRequestRepository);
    }

    private UserAccount account(long id, String email) {
        UserAccount account = new UserAccount();
        ReflectionTestUtils.setField(account, "id", id);
        account.setEmail(email);
        account.setCreatedAt(LocalDateTime.now());
        return account;
    }

    @Test
    void listIncludesSubmittedNameFromTheMostRecentVerificationRequest() {
        UserAccount account = account(6L, "yuva@example.com");
        when(verificationReviewService.findUnlinkedActiveAccounts()).thenReturn(List.of(account));

        VerificationRequest older = new VerificationRequest();
        older.setSubmittedFullName("Old Name");
        older.setCreatedAt(LocalDateTime.now().minusDays(2));
        VerificationRequest newer = new VerificationRequest();
        newer.setSubmittedFullName("Yuva Raj Bhatta");
        newer.setSubmittedFatherName("Bhoj Raj Bhatta");
        newer.setSubmittedGrandfatherName("Jhanka Nath Bhatta");
        newer.setCreatedAt(LocalDateTime.now());
        when(verificationRequestRepository.findByUserAccountId(6L)).thenReturn(List.of(older, newer));

        List<UnlinkedAccountDto> result = controller().list();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).submittedFullName()).isEqualTo("Yuva Raj Bhatta");
        assertThat(result.get(0).email()).isEqualTo("yuva@example.com");
    }

    @Test
    void listHandlesAccountsWithNoVerificationRequestAtAll() {
        UserAccount account = account(7L, "legacy@example.com");
        when(verificationReviewService.findUnlinkedActiveAccounts()).thenReturn(List.of(account));
        when(verificationRequestRepository.findByUserAccountId(7L)).thenReturn(List.of());

        List<UnlinkedAccountDto> result = controller().list();

        assertThat(result.get(0).submittedFullName()).isNull();
    }

    @Test
    void linkDelegatesToService() {
        when(authentication.getName()).thenReturn("admin");
        LinkAccountRequestDto request = new LinkAccountRequestDto();
        request.setPersonId(416L);

        controller().link(6L, request, authentication);

        verify(verificationReviewService).linkAccountToPerson(6L, 416L, "admin");
    }
}
