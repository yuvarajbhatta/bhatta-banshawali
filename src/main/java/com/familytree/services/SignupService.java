package com.familytree.services;

import com.familytree.calendar.BikramSambatConverter;
import com.familytree.calendar.BikramSambatDate;
import com.familytree.dto.SignupRequestDto;
import com.familytree.entity.OtpPurpose;
import com.familytree.entity.UserAccount;
import com.familytree.entity.VerificationRequest;
import com.familytree.entity.VerificationStatus;
import com.familytree.repository.UserAccountRepository;
import com.familytree.repository.VerificationRequestRepository;
import com.familytree.services.email.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates a new signup: creates the UserAccount, runs the family
 * matcher, records a VerificationRequest, and sends a verification email
 * -- see docs/05-auth-and-verification.md.
 *
 * Email verification is deliberately decoupled from UserAccountStatus/admin
 * approval (see UserAccount.emailVerifiedAt's javadoc) -- it's an
 * admin-review signal and a place for password-reset to land, not a login
 * gate. Nothing here changes VerificationReviewService.approve()'s
 * existing, already-tested behavior of activating the account regardless
 * of email-verification state.
 */
@Service
public class SignupService {

    private static final Logger log = LoggerFactory.getLogger(SignupService.class);

    private final UserAccountRepository userAccountRepository;
    private final VerificationRequestRepository verificationRequestRepository;
    private final PasswordEncoder passwordEncoder;
    private final FamilyMatchService familyMatchService;
    private final OtpService otpService;
    private final EmailService emailService;

    public SignupService(UserAccountRepository userAccountRepository,
                         VerificationRequestRepository verificationRequestRepository,
                         PasswordEncoder passwordEncoder,
                         FamilyMatchService familyMatchService,
                         OtpService otpService,
                         EmailService emailService) {
        this.userAccountRepository = userAccountRepository;
        this.verificationRequestRepository = verificationRequestRepository;
        this.passwordEncoder = passwordEncoder;
        this.familyMatchService = familyMatchService;
        this.otpService = otpService;
        this.emailService = emailService;
    }

    /**
     * @throws EmailAlreadyRegisteredException if the email already has an
     *          account -- see the class javadoc for why this is no longer
     *          a silent no-op.
     */
    @Transactional
    public void submitSignup(SignupRequestDto request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        if (userAccountRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException("An account with this email already exists.");
        }

        UserAccount account = new UserAccount();
        account.setEmail(normalizedEmail);
        account.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        account.setPreferredLanguage(request.getPreferredLanguage());
        account = userAccountRepository.save(account);

        String code = otpService.generate(account, OtpPurpose.EMAIL_VERIFICATION);
        try {
            emailService.sendVerificationOtpEmail(account.getEmail(), code, account.getPreferredLanguage());
        } catch (Exception e) {
            // Best-effort: a transient SMTP failure must never roll back
            // the signup itself.
            log.warn("Failed to send verification email to {}", account.getEmail(), e);
        }

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
        verificationRequest.setMatchedCandidatePersonIds(CommaSeparatedIds.join(matchResult.existingPersonCandidateIds()));
        verificationRequest.setMatchedFatherCandidatePersonIds(CommaSeparatedIds.join(matchResult.newPersonFatherCandidateIds()));
        verificationRequest.setStatus(VerificationStatus.PENDING);

        return verificationRequest;
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
