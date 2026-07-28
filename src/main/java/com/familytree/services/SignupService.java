package com.familytree.services;

import com.familytree.calendar.BikramSambatConverter;
import com.familytree.calendar.BikramSambatDate;
import com.familytree.dto.SignupRequestDto;
import com.familytree.entity.UserAccount;
import com.familytree.entity.VerificationRequest;
import com.familytree.entity.VerificationStatus;
import com.familytree.repository.UserAccountRepository;
import com.familytree.repository.VerificationRequestRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

/**
 * Orchestrates a new signup: creates the UserAccount, runs the family
 * matcher, and records a VerificationRequest -- see
 * docs/05-auth-and-verification.md.
 *
 * Known gap: no email-sending infrastructure exists yet, so
 * UserAccountStatus stays PENDING_EMAIL_VERIFICATION indefinitely after
 * this runs -- nothing currently moves an account past it. Building that
 * (token generation, SMTP, a confirmation endpoint) is separate work that
 * needs real mail-delivery configuration decisions from the user.
 */
@Service
public class SignupService {

    private final UserAccountRepository userAccountRepository;
    private final VerificationRequestRepository verificationRequestRepository;
    private final PasswordEncoder passwordEncoder;
    private final FamilyMatchService familyMatchService;

    public SignupService(UserAccountRepository userAccountRepository,
                         VerificationRequestRepository verificationRequestRepository,
                         PasswordEncoder passwordEncoder,
                         FamilyMatchService familyMatchService) {
        this.userAccountRepository = userAccountRepository;
        this.verificationRequestRepository = verificationRequestRepository;
        this.passwordEncoder = passwordEncoder;
        this.familyMatchService = familyMatchService;
    }

    /**
     * Always completes without throwing for well-formed input, even if
     * the email is already registered -- callers must return the same
     * response either way (see SignupController). If the email already
     * exists, this is a deliberate, silent no-op: no duplicate account or
     * verification request is created.
     */
    @Transactional
    public void submitSignup(SignupRequestDto request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        if (userAccountRepository.existsByEmail(normalizedEmail)) {
            return;
        }

        UserAccount account = new UserAccount();
        account.setEmail(normalizedEmail);
        account.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        account.setPreferredLanguage(request.getPreferredLanguage());
        account = userAccountRepository.save(account);

        FamilyMatchResult matchResult = familyMatchService.evaluateMatch(new FamilyMatchRequest(
                request.getFullName(), request.getFatherName(), request.getGrandfatherName(), request.getDobAd()));

        VerificationRequest verificationRequest = buildVerificationRequest(account, request, matchResult);
        verificationRequestRepository.save(verificationRequest);
    }

    private VerificationRequest buildVerificationRequest(UserAccount account, SignupRequestDto request,
                                                          FamilyMatchResult matchResult) {
        VerificationRequest verificationRequest = new VerificationRequest();
        verificationRequest.setUserAccount(account);
        verificationRequest.setSubmittedFullName(request.getFullName());
        verificationRequest.setSubmittedFatherName(request.getFatherName());
        verificationRequest.setSubmittedGrandfatherName(request.getGrandfatherName());
        verificationRequest.setSubmittedDobAd(request.getDobAd());

        if (BikramSambatConverter.isSupported(request.getDobAd())) {
            BikramSambatDate bs = BikramSambatConverter.toBs(request.getDobAd());
            verificationRequest.setSubmittedDobBsYear(bs.year());
            verificationRequest.setSubmittedDobBsMonth(bs.month());
            verificationRequest.setSubmittedDobBsDay(bs.day());
        }

        verificationRequest.setMotherName(request.getMotherName());
        verificationRequest.setPlaceOfBirth(request.getPlaceOfBirth());
        verificationRequest.setAncestralVillage(request.getAncestralVillage());
        verificationRequest.setFamilyBranch(request.getFamilyBranch());
        verificationRequest.setKnownRelativeName(request.getKnownRelativeName());
        verificationRequest.setInvitationCode(request.getInvitationCode());
        verificationRequest.setApplicantNote(request.getApplicantNote());
        verificationRequest.setMatchConfidence(matchResult.confidence());
        verificationRequest.setMatchedCandidatePersonIds(
                matchResult.candidatePersonIds().stream().map(String::valueOf).collect(Collectors.joining(",")));
        verificationRequest.setStatus(VerificationStatus.PENDING);

        return verificationRequest;
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
