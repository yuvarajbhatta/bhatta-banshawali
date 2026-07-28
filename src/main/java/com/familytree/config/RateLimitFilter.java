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
 * Throttles the two endpoints an anonymous caller can hit repeatedly to do
 * damage: signup (spam applications / family-member enumeration) and login
 * (credential stuffing) -- docs/09-security-threat-model.md risks 1 and 2.
 * Keyed by client IP via X-Forwarded-For (set by nginx; see the
 * banshawali.yrbhatta.com vhost), falling back to the socket address for
 * direct/local calls.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    static final int SIGNUP_CAPACITY = 5;
    static final Duration SIGNUP_PERIOD = Duration.ofHours(1);
    static final int LOGIN_CAPACITY = 10;
    static final Duration LOGIN_PERIOD = Duration.ofMinutes(15);

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

        if (isSignup && !rateLimiter.tryConsume("signup", clientIp, SIGNUP_CAPACITY, SIGNUP_PERIOD)) {
            respondTooManyRequests(response, request.getRequestURI().startsWith("/api/"));
            return;
        }
        if (isLogin && !rateLimiter.tryConsume("login", clientIp, LOGIN_CAPACITY, LOGIN_PERIOD)) {
            respondTooManyRequests(response, false);
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
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
