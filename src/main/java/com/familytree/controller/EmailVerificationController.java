package com.familytree.controller;

import com.familytree.dto.StatusResponseDto;
import com.familytree.dto.VerifyEmailConfirmDto;
import com.familytree.services.EmailVerificationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Confirms a signup's email is real -- see EmailVerificationService.
 * POST-only and requiring an explicit click on the frontend (never fired
 * by the verify-email page's own GET load) so an email-security scanner
 * pre-fetching the link in the email can't burn the single-use token
 * before the real user clicks it.
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
        // token doesn't exist, was already consumed, or has expired.
        emailVerificationService.confirmVerification(request.token());
        return new StatusResponseDto("EMAIL_VERIFIED");
    }
}
