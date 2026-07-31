package com.familytree.config;

import com.familytree.repository.AppUserRepository;
import com.familytree.repository.UserAccountRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(AppUserRepository appUserRepository,
                                                  UserAccountRepository userAccountRepository) {
        return new BridgingUserDetailsService(appUserRepository, userAccountRepository);
    }

    // docs/09-security-threat-model.md item 6 (XSS), for defense-in-depth on
    // the shrinking set of Thymeleaf-rendered pages this backend still serves
    // directly (most real pages are Next.js now). script-src needs
    // 'unsafe-inline' here, unlike the Next.js side (proxy.ts, which uses a
    // per-request nonce instead): lineage.html and the language-switch
    // fragment included on nearly every legacy page rely on inline <script>
    // blocks and onclick="..." handlers throughout, confirmed by a live
    // click-through that a bare 'self' policy breaks these pages outright.
    // The other directives (frame-ancestors, base-uri, form-action,
    // connect-src, img-src) still hold real value even with this concession.
    private static final String CONTENT_SECURITY_POLICY = String.join("; ",
            "default-src 'self'",
            "script-src 'self' 'unsafe-inline'",
            "style-src 'self' 'unsafe-inline'",
            "img-src 'self' data:",
            "connect-src 'self'",
            "frame-ancestors 'none'",
            "base-uri 'self'",
            "form-action 'self'");

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, RateLimiter rateLimiter) throws Exception {
        http
                .headers(headers -> headers.contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY)))
                // CSRF protects state-changing requests made using an existing
                // authenticated session cookie; an anonymous signup POST has no
                // session to hijack, so that one endpoint stays exempt. Every
                // other mutation (including authenticated JSON POSTs from the
                // Next.js frontend, e.g. POST /api/v1/persons/{id}/corrections)
                // needs a real CSRF token -- a Thymeleaf form gets one via the
                // server-rendered hidden _csrf field, but a Next.js fetch() call
                // has no such field to read. CookieCsrfTokenRepository exposes
                // the token as a plain (non-HttpOnly) "XSRF-TOKEN" cookie so
                // client-side JS can read it and echo it back in the
                // "X-XSRF-TOKEN" header -- Spring Security's built-in
                // SPA-friendly pattern, unaffected for the existing Thymeleaf
                // forms since th:value="${_csrf.token}" reads the same
                // resolved CsrfToken regardless of where it's stored.
                //
                // csrfTokenRequestHandler must be the plain
                // CsrfTokenRequestAttributeHandler, not the (6.x) default
                // XorCsrfTokenRequestAttributeHandler: the Xor handler
                // BREACH-masks the token, so the plain value written to the
                // cookie is not the value it expects back in the header --
                // the naive "read cookie, echo header" pattern the frontend
                // uses 403s on every request without this.
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers("/api/v1/signup"))
                .addFilterBefore(new RateLimitFilter(rateLimiter), UsernamePasswordAuthenticationFilter.class)
                // Default HttpSessionRequestCache caches ANY unauthenticated
                // GET, including the Next.js login page's own "is this
                // visitor already signed in?" SSR check (a GET /api/v1/me
                // made with whatever session cookie the browser currently
                // has). If that check runs while unauthenticated -- e.g.
                // right after a failed login attempt on /login?error, which
                // (unlike /login?logout) doesn't skip the check -- it gets
                // cached, and formLogin's defaultSuccessUrl(url, false)
                // below replays it as the post-login redirect target
                // instead of "/dashboard": the browser navigates straight
                // to a raw JSON API endpoint, which 404s outright for a
                // legacy AppUser admin login (MemberProfileController has
                // no UserAccount to look up). Only real page requests
                // should ever be "the page to return to after login" --
                // API calls never should, whether hit directly by the
                // browser or via SSR.
                .requestCache(cache -> {
                    HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
                    requestCache.setRequestMatcher(new NegatedRequestMatcher(PathPatternRequestMatcher.pathPattern("/api/**")));
                    cache.requestCache(requestCache);
                })
                // Forces the XSRF-TOKEN cookie to actually be written on
                // every request, not just when a Thymeleaf form reads
                // _csrf -- see CsrfCookieFilter.
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
                // Without this, an unauthenticated call to a protected
                // /api/** endpoint hits formLogin's default entry point,
                // which redirects to /login -- fine for a browser
                // navigating to a Thymeleaf page, but a JSON API caller
                // (the Next.js dashboard's server-to-server fetch) follows
                // that redirect and gets the login page's HTML back with a
                // 200 status instead of a usable 401. API paths get a real
                // 401 here instead; everything else keeps the login
                // redirect. Both matchers must be registered explicitly:
                // registering only the /api/** one makes Spring Security
                // use it as THE single entry point for every request
                // (bypassing formLogin's redirect everywhere, not just for
                // /api/**) -- see ExceptionHandlingConfigurer, which only
                // builds a real per-matcher DelegatingAuthenticationEntryPoint
                // once more than one mapping is registered.
                .exceptionHandling(exceptions -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                PathPatternRequestMatcher.pathPattern("/api/**"))
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/login"),
                                PathPatternRequestMatcher.pathPattern("/**")))
                .authorizeHttpRequests(auth -> auth
                        // "/login" itself is no longer backend-owned -- it's a
                        // Next.js page now (nginx routes it there), matching
                        // how "/signup" was migrated earlier. Only the actual
                        // session-establishing POST stays on this backend.
                        .requestMatchers("/css/**", "/js/**").permitAll()
                        .requestMatchers("/api/v1/auth/login").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        // Public, read-only admin-managed content (About, History, Membership
                        // explainer) for the unauthenticated marketing/public pages.
                        .requestMatchers(HttpMethod.GET, "/api/v1/content", "/api/v1/content/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/public-stats").permitAll()
                        // Needed by the signup form before an applicant has an
                        // account; reveals nothing about any person or family data.
                        .requestMatchers(HttpMethod.GET, "/api/v1/date-conversion/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/signup").permitAll()
                        // Scraped by the local Prometheus only; the app port is bound on
                        // all interfaces, so restrict the endpoint to loopback callers.
                        .requestMatchers("/actuator/prometheus").access(
                                new WebExpressionAuthorizationManager("hasIpAddress('127.0.0.1') or hasIpAddress('::1')"))
                        // More specific than the general "/persons" rule below (must come
                        // first -- authorizeHttpRequests matches in order, first match
                        // wins): POST /persons is PersonController#savePerson, a create
                        // endpoint that was previously reachable by any USER, not just
                        // ADMIN (docs/09-security-threat-model.md item 9 finding).
                        .requestMatchers(HttpMethod.POST, "/persons").hasRole("ADMIN")
                        // Granting admin access is a privilege-escalation choke point
                        // (docs/09-security-threat-model.md item 13) -- must be more
                        // specific than, and precede, the general "/api/v1/admin/**"
                        // rule below. Only ADMINISTRATOR+SUPER_ADMINISTRATOR accounts
                        // (and the legacy AppUser admin) hold ROLE_SUPER_ADMIN --
                        // see BridgingUserDetailsService.
                        .requestMatchers(HttpMethod.POST, "/api/v1/admin/admin-access-requests/*/approve").hasRole("SUPER_ADMIN")
                        .requestMatchers("/", "/persons", "/persons/*", "/relationships", "/lineage", "/lineage/tree", "/generations")
                        .hasAnyRole("ADMIN", "USER")
                        .requestMatchers(
                                "/persons/admin/**",
                                "/persons/new", "/persons/edit/**", "/persons/delete/**", "/persons/update/**",
                                "/relationships/new", "/relationships/edit/**", "/relationships/delete/**", "/relationships/update/**",
                                "/lineage/save-person",
                                "/v3/api-docs/**", "/v3/api-docs", "/swagger-ui/**", "/swagger-ui.html",
                                "/admin/**", "/api/v1/admin/**"
                        ).hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        // The page itself is Next.js-rendered now (see the
                        // banshawali.yrbhatta.com nginx vhost) -- this is only
                        // the redirect target for "you must log in first" and
                        // the base for the default failure URL
                        // ("/login?error", unchanged). The Next.js login page
                        // reads the XSRF-TOKEN cookie and does a native form
                        // POST (not fetch) to loginProcessingUrl below, so the
                        // browser's normal session-cookie/redirect handling
                        // is unaffected by which app renders the form.
                        .loginPage("/login")
                        // Can no longer be "/login" (Next.js owns that path
                        // now) -- this backend still processes the actual
                        // credential check, just at its own dedicated path
                        // under the already-proxied /api/v1/** prefix.
                        .loginProcessingUrl("/api/v1/auth/login")
                        // Not "/persons" -- everyone lands on the new
                        // dashboard after login now, admin or member;
                        // dashboard content itself is role-aware.
                        // alwaysUse=false still honors a saved request (e.g.
                        // being redirected here from a protected page) when
                        // one exists.
                        .defaultSuccessUrl("/dashboard", false)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        return http.build();
    }

}
