package com.familytree.config;

import com.familytree.dto.ErrorResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.regex.Pattern;

/**
 * Throttles endpoints a caller can hit repeatedly to do damage: signup
 * (spam applications / family-member enumeration) and login (credential
 * stuffing) -- docs/09-security-threat-model.md risks 1 and 2 -- plus,
 * since risk 15 (scraping the family tree) found neither endpoint
 * throttled at all, the two data-exposure endpoints a scripted loop would
 * actually hit to bulk-extract family data: the whole-tree fetch and
 * person search. Those two limits are deliberately generous (a real user
 * hits /api/v1/family-tree once per /tree or /family page load -- window
 * expansion is pure client-side slicing, no re-fetch, see
 * useTreeWindow.ts -- and /api/v1/persons search is live-typing
 * typeahead in admin forms) so normal use never trips them. Also throttles
 * password-reset-request (inbox-spamming a target) and the three token/OTP
 * confirm endpoints (password-reset, email-verify, admin-access-request,
 * sharing one bucket -- cheap defense-in-depth against brute-forcing, see
 * TokenService/OtpService). Also photo upload (PersonPhotoController) --
 * real per-request work (image decode/re-encode, disk write) with no admin
 * review queue in front of it. Also throttles the email-verify resend and
 * admin-access-request endpoints (inbox-spamming a target with OTP emails,
 * same shape as password-reset-request). Keyed by
 * client IP via X-Forwarded-For (set by nginx; see the
 * banshawali.yrbhatta.com vhost), falling back to the socket address for
 * direct/local calls.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    static final int SIGNUP_CAPACITY = 5;
    static final Duration SIGNUP_PERIOD = Duration.ofHours(1);
    static final int LOGIN_CAPACITY = 10;
    static final Duration LOGIN_PERIOD = Duration.ofMinutes(15);
    static final int FAMILY_TREE_CAPACITY = 20;
    static final Duration FAMILY_TREE_PERIOD = Duration.ofMinutes(1);
    static final int PERSON_SEARCH_CAPACITY = 60;
    static final Duration PERSON_SEARCH_PERIOD = Duration.ofMinutes(1);
    // Request-side: stops inbox-spamming a target via repeated reset
    // requests. Confirm-side: cheap defense-in-depth against token
    // brute-forcing, even though 256-bit SecureRandom tokens (see
    // TokenService) already make guessing infeasible on their own. Shared
    // across both confirm endpoints (password-reset and email-verify) --
    // same kind of action, no reason to track them separately.
    static final int PASSWORD_RESET_REQUEST_CAPACITY = 5;
    static final Duration PASSWORD_RESET_REQUEST_PERIOD = Duration.ofHours(1);
    static final int TOKEN_CONFIRM_CAPACITY = 10;
    static final Duration TOKEN_CONFIRM_PERIOD = Duration.ofMinutes(15);
    // Same shape/risk as password-reset-request: issuing a fresh OTP sends
    // an email, so an unthrottled caller could inbox-spam any address.
    static final int OTP_REQUEST_CAPACITY = 5;
    static final Duration OTP_REQUEST_PERIOD = Duration.ofHours(1);
    // Photo upload does real work per request (image decode/re-encode,
    // disk write) and ships with no admin review queue -- capped tighter
    // than the read-only endpoints above so a scripted loop can't cheaply
    // fill /srv/data/familytree with junk faster than a real user could
    // ever upload actual family photos.
    static final int PHOTO_UPLOAD_CAPACITY = 10;
    static final Duration PHOTO_UPLOAD_PERIOD = Duration.ofMinutes(15);

    private static final Pattern PHOTO_UPLOAD_PATH = Pattern.compile("/api/v1/persons/\\d+/photos");

    private final RateLimiter rateLimiter;
    // A plain instance, not the application's configured ObjectMapper bean:
    // this only ever serializes one fixed, static error shape, so it has no
    // need of the app's Jackson customizations (and depending on that bean
    // would drag SecurityConfig into every narrow test slice that doesn't
    // otherwise configure Jackson).
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RateLimitFilter(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String clientIp = clientIp(request);
        boolean isSignup = "POST".equals(request.getMethod()) && "/api/v1/signup".equals(request.getRequestURI());
        boolean isLogin = "POST".equals(request.getMethod()) && "/login".equals(request.getRequestURI());
        boolean isFamilyTree = "GET".equals(request.getMethod()) && "/api/v1/family-tree".equals(request.getRequestURI());
        boolean isPersonSearch = "GET".equals(request.getMethod()) && "/api/v1/persons".equals(request.getRequestURI());
        boolean isPasswordResetRequest = "POST".equals(request.getMethod())
                && "/api/v1/password-reset/request".equals(request.getRequestURI());
        boolean isTokenConfirm = "POST".equals(request.getMethod())
                && ("/api/v1/password-reset/confirm".equals(request.getRequestURI())
                        || "/api/v1/verify-email/confirm".equals(request.getRequestURI())
                        || "/api/v1/me/admin-access-request/confirm".equals(request.getRequestURI()));
        boolean isPhotoUpload = "POST".equals(request.getMethod()) && PHOTO_UPLOAD_PATH.matcher(request.getRequestURI()).matches();
        boolean isOtpRequest = "POST".equals(request.getMethod())
                && ("/api/v1/verify-email/resend".equals(request.getRequestURI())
                        || "/api/v1/me/admin-access-request".equals(request.getRequestURI()));

        if (isSignup && !rateLimiter.tryConsume("signup", clientIp, SIGNUP_CAPACITY, SIGNUP_PERIOD)) {
            respondTooManyRequests(response, request.getRequestURI().startsWith("/api/"));
            return;
        }
        if (isLogin && !rateLimiter.tryConsume("login", clientIp, LOGIN_CAPACITY, LOGIN_PERIOD)) {
            respondTooManyRequests(response, false);
            return;
        }
        if (isFamilyTree && !rateLimiter.tryConsume("family-tree", clientIp, FAMILY_TREE_CAPACITY, FAMILY_TREE_PERIOD)) {
            respondTooManyRequests(response, true);
            return;
        }
        if (isPersonSearch && !rateLimiter.tryConsume("person-search", clientIp, PERSON_SEARCH_CAPACITY, PERSON_SEARCH_PERIOD)) {
            respondTooManyRequests(response, true);
            return;
        }
        if (isPasswordResetRequest && !rateLimiter.tryConsume("password-reset-request", clientIp,
                PASSWORD_RESET_REQUEST_CAPACITY, PASSWORD_RESET_REQUEST_PERIOD)) {
            respondTooManyRequests(response, true);
            return;
        }
        if (isTokenConfirm && !rateLimiter.tryConsume("token-confirm", clientIp, TOKEN_CONFIRM_CAPACITY, TOKEN_CONFIRM_PERIOD)) {
            respondTooManyRequests(response, true);
            return;
        }
        if (isPhotoUpload && !rateLimiter.tryConsume("photo-upload", clientIp, PHOTO_UPLOAD_CAPACITY, PHOTO_UPLOAD_PERIOD)) {
            respondTooManyRequests(response, true);
            return;
        }
        if (isOtpRequest && !rateLimiter.tryConsume("otp-request", clientIp, OTP_REQUEST_CAPACITY, OTP_REQUEST_PERIOD)) {
            respondTooManyRequests(response, true);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void respondTooManyRequests(HttpServletResponse response, boolean asJson) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        if (asJson) {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(),
                    new ErrorResponseDto("Too many requests. Please try again later."));
        } else {
            response.setContentType(MediaType.TEXT_PLAIN_VALUE);
            response.getWriter().write("Too many requests. Please try again later.");
        }
    }

    private String clientIp(HttpServletRequest request) {
        // CF-Connecting-IP first: production sits behind a Cloudflare Tunnel
        // (see /etc/cloudflared/config.yml), and Cloudflare's edge always
        // sets this header itself, overwriting anything a client sends --
        // unlike X-Forwarded-For, which nginx (proxy_add_x_forwarded_for)
        // appends the real peer address to rather than replacing, so a
        // caller can freely prepend an arbitrary fake value to it and
        // defeat a leftmost-entry rate limit entirely. X-Forwarded-For
        // stays as a fallback only for local/dev access with no Cloudflare
        // in front.
        String cfConnectingIp = request.getHeader("CF-Connecting-IP");
        if (cfConnectingIp != null && !cfConnectingIp.isBlank()) {
            return cfConnectingIp.trim();
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
