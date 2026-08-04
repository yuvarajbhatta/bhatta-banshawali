package com.familytree.config;

import com.familytree.entity.UserAccountStatus;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;

class LoginFailureHandlerTest {

    private final LoginFailureHandler handler = new LoginFailureHandler();

    @Test
    void redirectsToTheGenericErrorForBadCredentials() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(new MockHttpServletRequest(), response, new BadCredentialsException("nope"));

        assertThat(response.getRedirectedUrl()).isEqualTo("/login?error");
    }

    @Test
    void redirectsWithAPendingReviewReasonWhenTheAccountIsPending() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(new MockHttpServletRequest(), response,
                new AccountNotActiveException(UserAccountStatus.PENDING_EMAIL_VERIFICATION));

        assertThat(response.getRedirectedUrl()).isEqualTo("/login?error=pending_review");
    }

    @Test
    void redirectsWithADisabledReasonWhenTheAccountIsDisabled() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(new MockHttpServletRequest(), response,
                new AccountNotActiveException(UserAccountStatus.DISABLED));

        assertThat(response.getRedirectedUrl()).isEqualTo("/login?error=disabled");
    }

    @Test
    void redirectsWithALockedReasonWhenTheAccountIsLocked() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(new MockHttpServletRequest(), response,
                new AccountNotActiveException(UserAccountStatus.LOCKED));

        assertThat(response.getRedirectedUrl()).isEqualTo("/login?error=locked");
    }
}
