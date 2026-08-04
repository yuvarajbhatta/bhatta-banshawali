package com.familytree.controller;

import com.familytree.dto.StatusResponseDto;
import com.familytree.dto.VerifyEmailConfirmDto;
import com.familytree.dto.VerifyEmailResendDto;
import com.familytree.services.EmailVerificationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Confirms a signup's email is real via a 6-digit OTP -- see
 * EmailVerificationService.
 */
@RestController
@RequestMapping("/api/v1/verify-email")
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    public EmailVerificationController(EmailVerificationService emailVerificationService) {
        this.emailVerificationService = emailVerificationService;
    }

    @PostMapping("/confirm")
    public StatusResponseDto confirm(@Valid @RequestBody VerifyEmailConfirmDto request) {
        // Throws InvalidOrExpiredTokenException (mapped to 400) if the
        // code doesn't match, was already consumed, has expired, or has
        // been guessed wrong too many times.
        emailVerificationService.confirmVerification(request.email(), request.code());
        return new StatusResponseDto("EMAIL_VERIFIED");
    }

    @PostMapping("/resend")
    public StatusResponseDto resend(@Valid @RequestBody VerifyEmailResendDto request) {
        emailVerificationService.resendVerification(request.email());
        return new StatusResponseDto("CODE_SENT");
    }
}
