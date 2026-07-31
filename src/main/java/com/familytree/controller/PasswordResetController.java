package com.familytree.controller;

import com.familytree.dto.PasswordResetConfirmDto;
import com.familytree.dto.PasswordResetRequestDto;
import com.familytree.dto.StatusResponseDto;
import com.familytree.services.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * "Forgot password" for UserAccount logins -- see PasswordResetService.
 * Both endpoints are anonymous/pre-session (no login yet), so they're
 * CSRF-exempt and permitAll in SecurityConfig, matching signup.
 */
@RestController
@RequestMapping("/api/v1/password-reset")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/request")
    public StatusResponseDto request(@Valid @RequestBody PasswordResetRequestDto request) {
        // Throws AccountNotFoundException (mapped to 404 by
        // ApiExceptionHandler) if no account matches -- see that
        // exception's javadoc for why this is a clear error, not a
        // silent anti-enumeration-safe response.
        passwordResetService.requestReset(request.email());
        return new StatusResponseDto("RESET_EMAIL_SENT");
    }

    @PostMapping("/confirm")
    public StatusResponseDto confirm(@Valid @RequestBody PasswordResetConfirmDto request) {
        if (!request.passwordsMatch()) {
            throw new IllegalArgumentException("Passwords do not match.");
        }

        // Throws InvalidOrExpiredTokenException (mapped to 400) if the
        // token doesn't exist, was already consumed, or has expired.
        passwordResetService.confirmReset(request.token(), request.newPassword());
        return new StatusResponseDto("PASSWORD_RESET");
    }
}
