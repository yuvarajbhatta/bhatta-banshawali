package com.familytree.config;

import com.familytree.repository.AppUserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

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
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/signup", "/css/**", "/js/**").permitAll()
                        .requestMatchers("/", "/persons", "/persons/*", "/relationships", "/lineage", "/lineage/tree", "/generations")
                        .hasAnyRole("ADMIN", "USER")
                        .requestMatchers(
                                "/persons/new", "/persons/edit/**", "/persons/delete/**", "/persons/update/**",
                                "/relationships/new", "/relationships/edit/**", "/relationships/delete/**", "/relationships/update/**",
                                "/lineage/save-person"
                        ).hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        return http.build();
    }

}
