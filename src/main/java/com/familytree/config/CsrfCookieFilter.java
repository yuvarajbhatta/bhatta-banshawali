package com.familytree.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Forces the CSRF token to actually be resolved (and, with
 * CookieCsrfTokenRepository, written as the XSRF-TOKEN cookie) on every
 * request. Spring Security's default token handling is deferred -- the
 * cookie is only ever written if something reads the CsrfToken request
 * attribute, which a Thymeleaf form does automatically via `_csrf` but
 * a Next.js page never does. Without this, a member who only ever
 * browses the Next.js frontend (dashboard, directory) would never get
 * the cookie, and their first authenticated POST (e.g. submitting a
 * correction) would fail CSRF validation. This is Spring Security's own
 * documented pattern for cookie-based CSRF with a JavaScript client.
 */
public class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            csrfToken.getToken();
        }
        filterChain.doFilter(request, response);
    }
}
