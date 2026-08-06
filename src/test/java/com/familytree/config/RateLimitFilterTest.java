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

    @Test
    void cfConnectingIpTakesPrecedenceSoASpoofedXForwardedForCannotBypassTheLimit() throws Exception {
        for (int i = 0; i < RateLimitFilter.SIGNUP_CAPACITY; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/signup");
            request.addHeader("CF-Connecting-IP", "203.0.113.30");
            // A different, attacker-controlled X-Forwarded-For on every request --
            // this must not let the real client dodge its own bucket.
            request.addHeader("X-Forwarded-For", "1.2.3." + i);
            filter.doFilter(request, new MockHttpServletResponse(), filterChain);
        }

        MockHttpServletRequest blockedRequest = new MockHttpServletRequest("POST", "/api/v1/signup");
        blockedRequest.addHeader("CF-Connecting-IP", "203.0.113.30");
        blockedRequest.addHeader("X-Forwarded-For", "9.9.9.9");
        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        filter.doFilter(blockedRequest, blockedResponse, filterChain);

        assertThat(blockedResponse.getStatus()).isEqualTo(429);
    }

    @Test
    void allowsFamilyTreeRequestsUpToTheLimitThenBlocks() throws Exception {
        for (int i = 0; i < RateLimitFilter.FAMILY_TREE_CAPACITY; i++) {
            MockHttpServletRequest request = familyTreeRequest("203.0.113.40");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, filterChain);
            assertThat(response.getStatus()).isEqualTo(200);
        }

        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        filter.doFilter(familyTreeRequest("203.0.113.40"), blockedResponse, filterChain);

        assertThat(blockedResponse.getStatus()).isEqualTo(429);
    }

    @Test
    void familyTreeLimitIsPerIpAddress() throws Exception {
        for (int i = 0; i < RateLimitFilter.FAMILY_TREE_CAPACITY; i++) {
            filter.doFilter(familyTreeRequest("203.0.113.41"), new MockHttpServletResponse(), filterChain);
        }

        MockHttpServletResponse otherIpResponse = new MockHttpServletResponse();
        filter.doFilter(familyTreeRequest("203.0.113.42"), otherIpResponse, filterChain);

        assertThat(otherIpResponse.getStatus()).isEqualTo(200);
    }

    @Test
    void allowsPersonSearchRequestsUpToTheLimitThenBlocks() throws Exception {
        for (int i = 0; i < RateLimitFilter.PERSON_SEARCH_CAPACITY; i++) {
            MockHttpServletRequest request = personSearchRequest("203.0.113.50");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, filterChain);
            assertThat(response.getStatus()).isEqualTo(200);
        }

        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        filter.doFilter(personSearchRequest("203.0.113.50"), blockedResponse, filterChain);

        assertThat(blockedResponse.getStatus()).isEqualTo(429);
        assertThat(blockedResponse.getContentType()).startsWith("application/json");
    }

    @Test
    void familyTreeAndPersonSearchLimitsAreIndependentScopes() throws Exception {
        for (int i = 0; i < RateLimitFilter.FAMILY_TREE_CAPACITY; i++) {
            filter.doFilter(familyTreeRequest("203.0.113.60"), new MockHttpServletResponse(), filterChain);
        }
        // Family-tree quota is exhausted for this IP, but person search is a different scope.
        MockHttpServletResponse searchResponse = new MockHttpServletResponse();
        filter.doFilter(personSearchRequest("203.0.113.60"), searchResponse, filterChain);

        assertThat(searchResponse.getStatus()).isEqualTo(200);
    }

    @Test
    void postToPersonsIsNotThrottledAsPersonSearch() throws Exception {
        // Only GET /api/v1/persons (search) is throttled here -- POST is an
        // unrelated admin-CRUD path and must never share this bucket.
        for (int i = 0; i < RateLimitFilter.PERSON_SEARCH_CAPACITY + 5; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/persons");
            request.addHeader("X-Forwarded-For", "203.0.113.70");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, filterChain);
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    @Test
    void allowsPasswordResetRequestsUpToTheLimitThenBlocks() throws Exception {
        for (int i = 0; i < RateLimitFilter.PASSWORD_RESET_REQUEST_CAPACITY; i++) {
            MockHttpServletRequest request = passwordResetRequestRequest("203.0.113.80");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, filterChain);
            assertThat(response.getStatus()).isEqualTo(200);
        }

        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        filter.doFilter(passwordResetRequestRequest("203.0.113.80"), blockedResponse, filterChain);

        assertThat(blockedResponse.getStatus()).isEqualTo(429);
        assertThat(blockedResponse.getContentType()).startsWith("application/json");
    }

    @Test
    void passwordResetConfirmAndVerifyEmailConfirmShareOneTokenConfirmBucket() throws Exception {
        for (int i = 0; i < RateLimitFilter.TOKEN_CONFIRM_CAPACITY; i++) {
            filter.doFilter(passwordResetConfirmRequest("203.0.113.81"), new MockHttpServletResponse(), filterChain);
        }
        // Quota exhausted via password-reset/confirm; verify-email/confirm
        // shares the same bucket, so it's blocked too.
        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        filter.doFilter(verifyEmailConfirmRequest("203.0.113.81"), blockedResponse, filterChain);

        assertThat(blockedResponse.getStatus()).isEqualTo(429);
    }

    @Test
    void tokenConfirmLimitIsIndependentOfPasswordResetRequestLimit() throws Exception {
        for (int i = 0; i < RateLimitFilter.PASSWORD_RESET_REQUEST_CAPACITY; i++) {
            filter.doFilter(passwordResetRequestRequest("203.0.113.82"), new MockHttpServletResponse(), filterChain);
        }
        // Request-side quota is exhausted for this IP, but confirm is a
        // different scope.
        MockHttpServletResponse confirmResponse = new MockHttpServletResponse();
        filter.doFilter(passwordResetConfirmRequest("203.0.113.82"), confirmResponse, filterChain);

        assertThat(confirmResponse.getStatus()).isEqualTo(200);
    }

    @Test
    void adminAccessRequestConfirmSharesTheTokenConfirmBucket() throws Exception {
        for (int i = 0; i < RateLimitFilter.TOKEN_CONFIRM_CAPACITY; i++) {
            filter.doFilter(passwordResetConfirmRequest("203.0.113.83"), new MockHttpServletResponse(), filterChain);
        }
        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        filter.doFilter(adminAccessRequestConfirmRequest("203.0.113.83"), blockedResponse, filterChain);

        assertThat(blockedResponse.getStatus()).isEqualTo(429);
    }

    @Test
    void allowsOtpRequestsUpToTheLimitThenBlocks() throws Exception {
        for (int i = 0; i < RateLimitFilter.OTP_REQUEST_CAPACITY; i++) {
            MockHttpServletRequest request = verifyEmailResendRequest("203.0.113.84");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, filterChain);
            assertThat(response.getStatus()).isEqualTo(200);
        }

        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        filter.doFilter(verifyEmailResendRequest("203.0.113.84"), blockedResponse, filterChain);

        assertThat(blockedResponse.getStatus()).isEqualTo(429);
    }

    @Test
    void verifyEmailResendAndAdminAccessRequestShareOneOtpRequestBucket() throws Exception {
        for (int i = 0; i < RateLimitFilter.OTP_REQUEST_CAPACITY; i++) {
            filter.doFilter(verifyEmailResendRequest("203.0.113.85"), new MockHttpServletResponse(), filterChain);
        }
        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        filter.doFilter(adminAccessRequestRequest("203.0.113.85"), blockedResponse, filterChain);

        assertThat(blockedResponse.getStatus()).isEqualTo(429);
    }

    @Test
    void allowsPhotoUploadRequestsUpToTheLimitThenBlocks() throws Exception {
        for (int i = 0; i < RateLimitFilter.PHOTO_UPLOAD_CAPACITY; i++) {
            MockHttpServletRequest request = photoUploadRequest("203.0.113.90", 42);
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, filterChain);
            assertThat(response.getStatus()).isEqualTo(200);
        }

        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        filter.doFilter(photoUploadRequest("203.0.113.90", 42), blockedResponse, filterChain);

        assertThat(blockedResponse.getStatus()).isEqualTo(429);
    }

    @Test
    void photoUploadLimitAppliesRegardlessOfWhichPersonIdIsInThePath() throws Exception {
        // One bucket per client IP across every person, not one bucket per
        // person -- otherwise a caller dodges the limit just by targeting a
        // different personId on each request.
        for (int i = 0; i < RateLimitFilter.PHOTO_UPLOAD_CAPACITY; i++) {
            filter.doFilter(photoUploadRequest("203.0.113.91", 100 + i), new MockHttpServletResponse(), filterChain);
        }

        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        filter.doFilter(photoUploadRequest("203.0.113.91", 999), blockedResponse, filterChain);

        assertThat(blockedResponse.getStatus()).isEqualTo(429);
    }

    @Test
    void signupPhotoUploadSharesTheSamePhotoUploadBucketAsPersonPhotos() throws Exception {
        // Same risk (real per-request decode/re-encode/disk-write work,
        // no admin review queue), so it's deliberately the same bucket,
        // not a separate one an attacker could exhaust independently.
        for (int i = 0; i < RateLimitFilter.PHOTO_UPLOAD_CAPACITY; i++) {
            filter.doFilter(signupPhotoRequest("203.0.113.93"), new MockHttpServletResponse(), filterChain);
        }

        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        filter.doFilter(signupPhotoRequest("203.0.113.93"), blockedResponse, filterChain);

        assertThat(blockedResponse.getStatus()).isEqualTo(429);
    }

    @Test
    void listingPhotosIsNotThrottledAsUpload() throws Exception {
        // Only POST (the actual upload) is throttled here -- GET (listing)
        // and the file-serving endpoint are unrelated read paths.
        for (int i = 0; i < RateLimitFilter.PHOTO_UPLOAD_CAPACITY + 5; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/persons/42/photos");
            request.addHeader("X-Forwarded-For", "203.0.113.92");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, filterChain);
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    private MockHttpServletRequest photoUploadRequest(String clientIp, long personId) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/persons/" + personId + "/photos");
        request.addHeader("X-Forwarded-For", clientIp);
        return request;
    }

    private MockHttpServletRequest signupPhotoRequest(String clientIp) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/signup/photo");
        request.addHeader("X-Forwarded-For", clientIp);
        return request;
    }

    private MockHttpServletRequest passwordResetRequestRequest(String clientIp) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/password-reset/request");
        request.addHeader("X-Forwarded-For", clientIp);
        return request;
    }

    private MockHttpServletRequest passwordResetConfirmRequest(String clientIp) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/password-reset/confirm");
        request.addHeader("X-Forwarded-For", clientIp);
        return request;
    }

    private MockHttpServletRequest verifyEmailConfirmRequest(String clientIp) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/verify-email/confirm");
        request.addHeader("X-Forwarded-For", clientIp);
        return request;
    }

    private MockHttpServletRequest verifyEmailResendRequest(String clientIp) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/verify-email/resend");
        request.addHeader("X-Forwarded-For", clientIp);
        return request;
    }

    private MockHttpServletRequest adminAccessRequestRequest(String clientIp) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/me/admin-access-request");
        request.addHeader("X-Forwarded-For", clientIp);
        return request;
    }

    private MockHttpServletRequest adminAccessRequestConfirmRequest(String clientIp) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/me/admin-access-request/confirm");
        request.addHeader("X-Forwarded-For", clientIp);
        return request;
    }

    private MockHttpServletRequest familyTreeRequest(String clientIp) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/family-tree");
        request.addHeader("X-Forwarded-For", clientIp);
        return request;
    }

    private MockHttpServletRequest personSearchRequest(String clientIp) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/persons");
        request.addHeader("X-Forwarded-For", clientIp);
        return request;
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
