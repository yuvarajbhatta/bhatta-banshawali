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
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

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

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, RateLimiter rateLimiter) throws Exception {
        http
                // CSRF protects state-changing requests made using an existing
                // authenticated session cookie; an anonymous signup POST has no
                // session to hijack, and there is no same-origin HTML form here
                // to carry a CSRF token (this is a JSON API called directly by
                // the Next.js frontend). CSRF stays enabled for everything else,
                // including the session-authenticated Thymeleaf admin forms.
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/v1/signup"))
                .addFilterBefore(new RateLimitFilter(rateLimiter), UsernamePasswordAuthenticationFilter.class)
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
                        .requestMatchers("/login", "/css/**", "/js/**").permitAll()
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
                        .requestMatchers("/", "/persons", "/persons/*", "/relationships", "/lineage", "/lineage/tree", "/generations")
                        .hasAnyRole("ADMIN", "USER")
                        .requestMatchers(
                                "/persons/admin/**",
                                "/persons/new", "/persons/edit/**", "/persons/delete/**", "/persons/update/**",
                                "/relationships/new", "/relationships/edit/**", "/relationships/delete/**", "/relationships/update/**",
                                "/lineage/save-person",
                                "/v3/api-docs/**", "/v3/api-docs", "/swagger-ui/**", "/swagger-ui.html",
                                "/admin/**"
                        ).hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        // Not "/" -- nginx routes that path to the Next.js
                        // public landing page now (banshawali.yrbhatta.com
                        // vhost), not this backend, so a login with no
                        // saved request (e.g. navigating straight to
                        // /login) must not fall back to it. alwaysUse=false
                        // still honors a saved request (e.g. being
                        // redirected here from /persons) when one exists.
                        .defaultSuccessUrl("/persons", false)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        return http.build();
    }

}
