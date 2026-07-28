package com.familytree.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class RateLimitFilterTest {

    private RateLimitFilter filter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter(new RateLimiter());
        filterChain = Mockito.mock(FilterChain.class);
    }

    @Test
    void allowsSignupRequestsUpToTheLimitThenBlocks() throws Exception {
        for (int i = 0; i < RateLimitFilter.SIGNUP_CAPACITY; i++) {
            MockHttpServletRequest request = signupRequest("203.0.113.5");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, filterChain);
            assertThat(response.getStatus()).isEqualTo(200);
        }
        verify(filterChain, times(RateLimitFilter.SIGNUP_CAPACITY)).doFilter(Mockito.any(), Mockito.any());

        MockHttpServletRequest blockedRequest = signupRequest("203.0.113.5");
        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        filter.doFilter(blockedRequest, blockedResponse, filterChain);

        assertThat(blockedResponse.getStatus()).isEqualTo(429);
        assertThat(blockedResponse.getContentAsString()).contains("Too many requests");
        verify(filterChain, times(RateLimitFilter.SIGNUP_CAPACITY)).doFilter(Mockito.any(), Mockito.any());
    }

    @Test
    void signupLimitIsPerIpAddress() throws Exception {
        for (int i = 0; i < RateLimitFilter.SIGNUP_CAPACITY; i++) {
            filter.doFilter(signupRequest("198.51.100.1"), new MockHttpServletResponse(), filterChain);
        }

        MockHttpServletResponse otherIpResponse = new MockHttpServletResponse();
        filter.doFilter(signupRequest("198.51.100.2"), otherIpResponse, filterChain);

        assertThat(otherIpResponse.getStatus()).isEqualTo(200);
    }

    @Test
    void loginRequestsHaveTheirOwnSeparateLimit() throws Exception {
        for (int i = 0; i < RateLimitFilter.SIGNUP_CAPACITY; i++) {
            filter.doFilter(signupRequest("192.0.2.9"), new MockHttpServletResponse(), filterChain);
        }
        // Signup quota is exhausted for this IP, but login is a different scope.
        MockHttpServletResponse loginResponse = new MockHttpServletResponse();
        filter.doFilter(loginRequest("192.0.2.9"), loginResponse, filterChain);

        assertThat(loginResponse.getStatus()).isEqualTo(200);
    }

    @Test
    void loginRequestsBlockedWithPlainTextNotJson() throws Exception {
        for (int i = 0; i < RateLimitFilter.LOGIN_CAPACITY; i++) {
            filter.doFilter(loginRequest("192.0.2.10"), new MockHttpServletResponse(), filterChain);
        }

        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        filter.doFilter(loginRequest("192.0.2.10"), blockedResponse, filterChain);

        assertThat(blockedResponse.getStatus()).isEqualTo(429);
        assertThat(blockedResponse.getContentType()).startsWith("text/plain");
    }

    @Test
    void unrelatedPathsAreNeverThrottled() throws Exception {
        for (int i = 0; i < RateLimitFilter.SIGNUP_CAPACITY + 5; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/public-stats");
            request.addHeader("X-Forwarded-For", "203.0.113.9");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, filterChain);
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    @Test
    void usesLeftmostXForwardedForEntryAsTheClientIp() throws Exception {
        for (int i = 0; i < RateLimitFilter.SIGNUP_CAPACITY; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/signup");
            request.addHeader("X-Forwarded-For", "203.0.113.20, 10.0.0.1");
            filter.doFilter(request, new MockHttpServletResponse(), filterChain);
        }

        MockHttpServletRequest blockedRequest = new MockHttpServletRequest("POST", "/api/v1/signup");
        blockedRequest.addHeader("X-Forwarded-For", "203.0.113.20, 10.0.0.2");
        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        filter.doFilter(blockedRequest, blockedResponse, filterChain);

        assertThat(blockedResponse.getStatus()).isEqualTo(429);
    }

    private MockHttpServletRequest signupRequest(String clientIp) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/signup");
        request.addHeader("X-Forwarded-For", clientIp);
        return request;
    }

    private MockHttpServletRequest loginRequest(String clientIp) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/login");
        request.addHeader("X-Forwarded-For", clientIp);
        return request;
    }
}
