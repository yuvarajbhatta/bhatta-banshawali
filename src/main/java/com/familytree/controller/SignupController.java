package com.familytree.controller;

import com.familytree.dto.SignupRequestDto;
import com.familytree.dto.SignupResponseDto;
import com.familytree.services.SignupService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * New UserAccount/VerificationRequest signup pipeline -- see
 * docs/05-auth-and-verification.md. Distinct from AuthController's
 * existing username/password-only signup (AppUser), which remains for
 * now per the Phase 1 cutover plan.
 */
@RestController
@RequestMapping("/api/v1/signup")
public class SignupController {

    private final SignupService signupService;

    public SignupController(SignupService signupService) {
        this.signupService = signupService;
    }

    @PostMapping
    public SignupResponseDto signup(@Valid @RequestBody SignupRequestDto request) {
        if (!request.passwordsMatch()) {
            throw new IllegalArgumentException("Passwords do not match.");
        }

        // Always the same response whether this created a new account, was
        // a silent no-op (email already registered), or matched with any
        // confidence level -- see SignupResponseDto.
        signupService.submitSignup(request);
        return SignupResponseDto.pendingReview();
    }
}
