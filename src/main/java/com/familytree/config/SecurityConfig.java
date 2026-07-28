package com.familytree.config;

import com.familytree.repository.AppUserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(AppUserRepository appUserRepository) {
        return username -> appUserRepository.findByUsername(username)
                .map(user -> User
                        .withUsername(user.getUsername())
                        .password(user.getPassword())
                        .roles(user.getRole().replace("ROLE_", ""))
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF protects state-changing requests made using an existing
                // authenticated session cookie; an anonymous signup POST has no
                // session to hijack, and there is no same-origin HTML form here
                // to carry a CSRF token (this is a JSON API called directly by
                // the Next.js frontend). CSRF stays enabled for everything else,
                // including the session-authenticated Thymeleaf admin forms.
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/v1/signup"))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/signup", "/css/**", "/js/**").permitAll()
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
