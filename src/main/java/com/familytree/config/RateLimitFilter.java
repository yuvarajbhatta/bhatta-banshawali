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
 * typeahead in admin forms) so normal use never trips them. Keyed by
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
