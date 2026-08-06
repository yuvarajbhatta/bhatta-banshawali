package com.familytree.services;

import com.familytree.dto.SignupRequestDto;
import com.familytree.entity.MatchConfidence;
import com.familytree.entity.OtpPurpose;
import com.familytree.entity.Person;
import com.familytree.entity.UserAccount;
import com.familytree.entity.VerificationRequest;
import com.familytree.entity.VerificationStatus;
import com.familytree.repository.UserAccountRepository;
import com.familytree.repository.VerificationRequestRepository;
import com.familytree.services.email.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SignupServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private VerificationRequestRepository verificationRequestRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private FamilyMatchService familyMatchService;

    @Mock
    private OtpService otpService;

    @Mock
    private EmailService emailService;

    @Mock
    private ImageReencodeService imageReencodeService;

    @Mock
    private PhotoStorageService photoStorageService;

    @InjectMocks
    private SignupService signupService;

    @Test
    void createsAccountAndVerificationRequestForNewEmail() {
        SignupRequestDto request = validRequest();
        when(userAccountRepository.existsByEmail("yuva@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("{bcrypt}hashed");
        when(userAccountRepository.save(any())).thenAnswer(invocation -> {
            UserAccount account = invocation.getArgument(0);
            account.setEmail(account.getEmail());
            return account;
        });
        Person candidate = new Person();
        candidate.setId(42L);
        when(familyMatchService.evaluateMatch(any())).thenReturn(
                new FamilyMatchResult(MatchConfidence.HIGH,
                        List.of(new CandidateEvaluation(candidate, true, true, false, false)), List.of()));

        signupService.submitSignup(request);

        ArgumentCaptor<UserAccount> accountCaptor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountRepository).save(accountCaptor.capture());
        assertThat(accountCaptor.getValue().getEmail()).isEqualTo("yuva@example.com");
        assertThat(accountCaptor.getValue().getPasswordHash()).isEqualTo("{bcrypt}hashed");

        ArgumentCaptor<VerificationRequest> verificationCaptor = ArgumentCaptor.forClass(VerificationRequest.class);
        verify(verificationRequestRepository).save(verificationCaptor.capture());
        VerificationRequest saved = verificationCaptor.getValue();
        assertThat(saved.getMatchConfidence()).isEqualTo(MatchConfidence.HIGH);
        assertThat(saved.getMatchedCandidatePersonIds()).isEqualTo("42");
        assertThat(saved.getMatchedFatherCandidatePersonIds()).isEmpty();
        assertThat(saved.getStatus()).isEqualTo(VerificationStatus.PENDING);
    }

    @Test
    void savesFatherCandidateIdsWhenTheNewPersonStrategyFindsMatches() {
        SignupRequestDto request = validRequest();
        when(passwordEncoder.encode(any())).thenReturn("{bcrypt}hashed");
        when(userAccountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        Person father = new Person();
        father.setId(99L);
        when(familyMatchService.evaluateMatch(any())).thenReturn(
                new FamilyMatchResult(MatchConfidence.HIGH, List.of(),
                        List.of(new NewPersonCandidateEvaluation(father, true, false))));

        signupService.submitSignup(request);

        ArgumentCaptor<VerificationRequest> verificationCaptor = ArgumentCaptor.forClass(VerificationRequest.class);
        verify(verificationRequestRepository).save(verificationCaptor.capture());
        assertThat(verificationCaptor.getValue().getMatchedFatherCandidatePersonIds()).isEqualTo("99");
        assertThat(verificationCaptor.getValue().getMatchedCandidatePersonIds()).isEmpty();
    }

    @Test
    void normalizesEmailToLowercaseAndTrimmed() {
        SignupRequestDto request = validRequest();
        request.setEmail("  Yuva@Example.com  ");
        when(passwordEncoder.encode(any())).thenReturn("{bcrypt}hashed");
        when(userAccountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(familyMatchService.evaluateMatch(any())).thenReturn(new FamilyMatchResult(MatchConfidence.LOW, List.of(), List.of()));

        signupService.submitSignup(request);

        verify(userAccountRepository).existsByEmail("yuva@example.com");
        ArgumentCaptor<UserAccount> accountCaptor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountRepository).save(accountCaptor.capture());
        assertThat(accountCaptor.getValue().getEmail()).isEqualTo("yuva@example.com");
    }

    @Test
    void throwsEmailAlreadyRegisteredExceptionWhenEmailExists() {
        SignupRequestDto request = validRequest();
        when(userAccountRepository.existsByEmail("yuva@example.com")).thenReturn(true);

        assertThatThrownBy(() -> signupService.submitSignup(request))
                .isInstanceOf(EmailAlreadyRegisteredException.class);

        verify(userAccountRepository, never()).save(any());
        verify(verificationRequestRepository, never()).save(any());
    }

    @Test
    void issuesEmailVerificationOtpAndSendsVerificationEmail() {
        SignupRequestDto request = validRequest();
        when(passwordEncoder.encode(any())).thenReturn("{bcrypt}hashed");
        when(userAccountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(familyMatchService.evaluateMatch(any())).thenReturn(new FamilyMatchResult(MatchConfidence.LOW, List.of(), List.of()));
        when(otpService.generate(any(), eq(OtpPurpose.EMAIL_VERIFICATION))).thenReturn("123456");

        signupService.submitSignup(request);

        verify(otpService).generate(any(UserAccount.class), eq(OtpPurpose.EMAIL_VERIFICATION));
        verify(emailService).sendVerificationOtpEmail("yuva@example.com", "123456", "en");
    }

    @Test
    void signupStillSavesEverythingWhenSendingTheVerificationEmailFails() {
        SignupRequestDto request = validRequest();
        when(passwordEncoder.encode(any())).thenReturn("{bcrypt}hashed");
        when(userAccountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(familyMatchService.evaluateMatch(any())).thenReturn(new FamilyMatchResult(MatchConfidence.LOW, List.of(), List.of()));
        when(otpService.generate(any(), eq(OtpPurpose.EMAIL_VERIFICATION))).thenReturn("123456");
        doThrow(new RuntimeException("SMTP down")).when(emailService)
                .sendVerificationOtpEmail(any(), any(), any());

        signupService.submitSignup(request);

        verify(userAccountRepository).save(any(UserAccount.class));
        verify(verificationRequestRepository).save(any(VerificationRequest.class));
    }

    @Test
    void computesBsDateWhenDobIsWithinSupportedRange() {
        SignupRequestDto request = validRequest();
        request.setDobAd(LocalDate.of(2000, 1, 1));
        when(passwordEncoder.encode(any())).thenReturn("{bcrypt}hashed");
        when(userAccountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(familyMatchService.evaluateMatch(any())).thenReturn(new FamilyMatchResult(MatchConfidence.LOW, List.of(), List.of()));

        signupService.submitSignup(request);

        ArgumentCaptor<VerificationRequest> captor = ArgumentCaptor.forClass(VerificationRequest.class);
        verify(verificationRequestRepository).save(captor.capture());
        assertThat(captor.getValue().getSubmittedDobBsYear()).isNotNull();
    }

    @Test
    void leavesBsDateNullWhenDobIsOutsideSupportedRange() {
        SignupRequestDto request = validRequest();
        request.setDobAd(LocalDate.of(1800, 1, 1));
        when(passwordEncoder.encode(any())).thenReturn("{bcrypt}hashed");
        when(userAccountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(familyMatchService.evaluateMatch(any())).thenReturn(new FamilyMatchResult(MatchConfidence.LOW, List.of(), List.of()));

        signupService.submitSignup(request);

        ArgumentCaptor<VerificationRequest> captor = ArgumentCaptor.forClass(VerificationRequest.class);
        verify(verificationRequestRepository).save(captor.capture());
        assertThat(captor.getValue().getSubmittedDobBsYear()).isNull();
    }

    @Test
    void submitSignupReturnsThePhotoUploadTokenSavedOnTheVerificationRequest() {
        SignupRequestDto request = validRequest();
        when(passwordEncoder.encode(any())).thenReturn("{bcrypt}hashed");
        when(userAccountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(familyMatchService.evaluateMatch(any())).thenReturn(new FamilyMatchResult(MatchConfidence.LOW, List.of(), List.of()));

        String token = signupService.submitSignup(request);

        ArgumentCaptor<VerificationRequest> captor = ArgumentCaptor.forClass(VerificationRequest.class);
        verify(verificationRequestRepository).save(captor.capture());
        assertThat(token).isNotBlank().isEqualTo(captor.getValue().getPhotoUploadToken());
    }

    @Test
    void uploadPendingPhotoReencodesAndStoresTheFileThenSavesTheStorageKey() {
        VerificationRequest pending = new VerificationRequest();
        pending.setStatus(VerificationStatus.PENDING);
        when(verificationRequestRepository.findByPhotoUploadToken("tok-1")).thenReturn(Optional.of(pending));

        MultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[] {1, 2, 3});
        byte[] reencoded = new byte[] {9, 9, 9};
        when(imageReencodeService.reencode(new byte[] {1, 2, 3})).thenReturn(reencoded);
        when(photoStorageService.store(reencoded)).thenReturn("new-key.jpg");

        signupService.uploadPendingPhoto("tok-1", file);

        ArgumentCaptor<VerificationRequest> captor = ArgumentCaptor.forClass(VerificationRequest.class);
        verify(verificationRequestRepository).save(captor.capture());
        assertThat(captor.getValue().getPendingPhotoStorageKey()).isEqualTo("new-key.jpg");
    }

    @Test
    void uploadPendingPhotoDeletesThePreviousFileWhenReplacingAnEarlierUpload() {
        VerificationRequest pending = new VerificationRequest();
        pending.setStatus(VerificationStatus.PENDING);
        pending.setPendingPhotoStorageKey("old-key.jpg");
        when(verificationRequestRepository.findByPhotoUploadToken("tok-2")).thenReturn(Optional.of(pending));

        MultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[] {1});
        when(imageReencodeService.reencode(any())).thenReturn(new byte[] {2});
        when(photoStorageService.store(any())).thenReturn("new-key.jpg");

        signupService.uploadPendingPhoto("tok-2", file);

        verify(photoStorageService).delete("old-key.jpg");
    }

    @Test
    void uploadPendingPhotoRejectsAnUnknownToken() {
        when(verificationRequestRepository.findByPhotoUploadToken("bogus")).thenReturn(Optional.empty());
        MultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[] {1});

        assertThatThrownBy(() -> signupService.uploadPendingPhoto("bogus", file))
                .isInstanceOf(ResponseStatusException.class);
        verify(photoStorageService, never()).store(any());
    }

    @Test
    void uploadPendingPhotoRejectsATokenForARequestThatsAlreadyBeenDecided() {
        VerificationRequest decided = new VerificationRequest();
        decided.setStatus(VerificationStatus.APPROVED);
        when(verificationRequestRepository.findByPhotoUploadToken("tok-3")).thenReturn(Optional.of(decided));
        MultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[] {1});

        assertThatThrownBy(() -> signupService.uploadPendingPhoto("tok-3", file))
                .isInstanceOf(ResponseStatusException.class);
        verify(photoStorageService, never()).store(any());
    }

    private SignupRequestDto validRequest() {
        SignupRequestDto request = new SignupRequestDto();
        request.setEmail("yuva@example.com");
        request.setFullName("Yuva Bhatta");
        request.setDobAd(LocalDate.of(1995, 6, 15));
        request.setFatherName("Bhoj Bhatta");
        request.setGrandfatherName("Jhanka Bhatta");
        request.setPassword("password123");
        request.setConfirmPassword("password123");
        request.setPreferredLanguage("en");
        request.setAgreedToTerms(true);
        return request;
    }
}
