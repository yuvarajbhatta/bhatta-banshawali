package com.familytree.services;

import com.familytree.entity.Person;
import com.familytree.entity.RelationshipType;
import com.familytree.entity.Role;
import com.familytree.entity.UserAccount;
import com.familytree.entity.UserAccountStatus;
import com.familytree.entity.VerificationRequest;
import com.familytree.entity.VerificationStatus;
import com.familytree.repository.PersonRepository;
import com.familytree.repository.RoleRepository;
import com.familytree.repository.UserAccountRepository;
import com.familytree.repository.VerificationRequestRepository;
import com.familytree.web.PersonDisplayHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificationReviewServiceTest {

    @Mock
    private VerificationRequestRepository verificationRequestRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PersonRepository personRepository;

    @Mock
    private RelationshipService relationshipService;

    @Mock
    private UserPersonLinkService userPersonLinkService;

    @Mock
    private PersonDisplayHelper personDisplay;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private FamilyMatchService familyMatchService;

    @Mock
    private com.familytree.repository.PersonPhotoRepository personPhotoRepository;

    @Mock
    private PhotoStorageService photoStorageService;

    @InjectMocks
    private VerificationReviewService verificationReviewService;

    @Test
    void approveMarksRequestApprovedAndActivatesAccount() {
        UserAccount account = new UserAccount();
        account.setStatus(UserAccountStatus.PENDING_EMAIL_VERIFICATION);
        VerificationRequest request = new VerificationRequest();
        request.setUserAccount(account);
        when(verificationRequestRepository.findById(1L)).thenReturn(Optional.of(request));

        verificationReviewService.approve(1L, "admin", "looks good", null, null, null);

        assertThat(request.getStatus()).isEqualTo(VerificationStatus.APPROVED);
        assertThat(request.getReviewedByUsername()).isEqualTo("admin");
        assertThat(request.getDecisionNote()).isEqualTo("looks good");
        assertThat(request.getReviewedAt()).isNotNull();
        assertThat(account.getStatus()).isEqualTo(UserAccountStatus.ACTIVE);

        verify(verificationRequestRepository).save(request);
        verify(userAccountRepository).save(account);
    }

    @Test
    void approveGrantsVerifiedMemberRoleSoTheAccountCanActuallyLogInWithAccess() {
        Role verifiedMember = new Role();
        verifiedMember.setName("VERIFIED_MEMBER");
        when(roleRepository.findByName("VERIFIED_MEMBER")).thenReturn(Optional.of(verifiedMember));

        UserAccount account = new UserAccount();
        VerificationRequest request = new VerificationRequest();
        request.setUserAccount(account);
        when(verificationRequestRepository.findById(10L)).thenReturn(Optional.of(request));

        verificationReviewService.approve(10L, "admin", null, null, null, null);

        assertThat(account.getRoles()).contains(verifiedMember);
    }

    @Test
    void approveDoesNotFailWhenVerifiedMemberRoleIsMissing() {
        when(roleRepository.findByName("VERIFIED_MEMBER")).thenReturn(Optional.empty());
        UserAccount account = new UserAccount();
        VerificationRequest request = new VerificationRequest();
        request.setUserAccount(account);
        when(verificationRequestRepository.findById(11L)).thenReturn(Optional.of(request));

        verificationReviewService.approve(11L, "admin", null, null, null, null);

        assertThat(account.getStatus()).isEqualTo(UserAccountStatus.ACTIVE);
        assertThat(account.getRoles()).isEmpty();
    }

    @Test
    void approveWithLinkedPersonIdCreatesAVerifiedUserPersonLink() {
        UserAccount account = new UserAccount();
        VerificationRequest request = new VerificationRequest();
        request.setUserAccount(account);
        when(verificationRequestRepository.findById(20L)).thenReturn(Optional.of(request));

        Person person = new Person();
        person.setId(77L);
        when(personRepository.findById(77L)).thenReturn(Optional.of(person));

        verificationReviewService.approve(20L, "admin", null, 77L, null, null);

        verify(userPersonLinkService).createVerifiedLink(account, person);
    }

    @Test
    void approveWithLinkedPersonIdBackfillsAMissingBirthDateFromTheSubmittedSignup() {
        // The common case for a large pre-populated tree: the matched
        // person is an existing, sparse seed-data record with no
        // birthDate ever entered. The applicant's own verified submission
        // is as authoritative a source as any for filling that gap.
        UserAccount account = new UserAccount();
        VerificationRequest request = new VerificationRequest();
        request.setUserAccount(account);
        request.setSubmittedDobAd(LocalDate.of(1995, 6, 15));
        when(verificationRequestRepository.findById(22L)).thenReturn(Optional.of(request));

        Person person = new Person();
        person.setId(78L);
        person.setBirthDate(null);
        when(personRepository.findById(78L)).thenReturn(Optional.of(person));

        verificationReviewService.approve(22L, "admin", null, 78L, null, null);

        assertThat(person.getBirthDate()).isEqualTo(LocalDate.of(1995, 6, 15));
        verify(personRepository).save(person);
    }

    @Test
    void approveWithLinkedPersonIdNeverOverwritesAnExistingBirthDate() {
        UserAccount account = new UserAccount();
        VerificationRequest request = new VerificationRequest();
        request.setUserAccount(account);
        request.setSubmittedDobAd(LocalDate.of(1995, 6, 15));
        when(verificationRequestRepository.findById(23L)).thenReturn(Optional.of(request));

        Person person = new Person();
        person.setId(79L);
        person.setBirthDate(LocalDate.of(1940, 1, 1));
        when(personRepository.findById(79L)).thenReturn(Optional.of(person));

        verificationReviewService.approve(23L, "admin", null, 79L, null, null);

        assertThat(person.getBirthDate()).isEqualTo(LocalDate.of(1940, 1, 1));
        verify(personRepository, never()).save(person);
    }

    @Test
    void approveWithoutLinkedPersonIdNeverTouchesUserPersonLinkService() {
        UserAccount account = new UserAccount();
        VerificationRequest request = new VerificationRequest();
        request.setUserAccount(account);
        when(verificationRequestRepository.findById(21L)).thenReturn(Optional.of(request));

        verificationReviewService.approve(21L, "admin", null, null, null, null);

        verifyNoInteractions(userPersonLinkService);
    }

    @Test
    void approveThrowsWhenLinkedPersonIdDoesNotExist() {
        UserAccount account = new UserAccount();
        VerificationRequest request = new VerificationRequest();
        request.setUserAccount(account);
        when(verificationRequestRepository.findById(22L)).thenReturn(Optional.of(request));
        when(personRepository.findById(999L)).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> verificationReviewService.approve(22L, "admin", null, 999L, null, null));
    }

    @Test
    void approveThrowsWhenBothLinkedPersonIdAndCreateAsChildOfFatherIdProvided() {
        assertThatThrownBy(() -> verificationReviewService.approve(1L, "admin", null, 5L, 6L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot both be provided");
    }

    @Test
    void approveWithCreateAsChildOfFatherIdCreatesNewPersonWithDerivedGeneration() {
        UserAccount account = new UserAccount();
        VerificationRequest request = new VerificationRequest();
        request.setUserAccount(account);
        request.setSubmittedFullName("Yuva Raj Bhatta");
        request.setSubmittedDobAd(LocalDate.of(1995, 6, 15));
        when(verificationRequestRepository.findById(30L)).thenReturn(Optional.of(request));

        Person father = new Person();
        father.setId(2L);
        father.setGenerationNumber(3);
        when(personRepository.findById(2L)).thenReturn(Optional.of(father));
        when(personRepository.save(any(Person.class))).thenAnswer(invocation -> invocation.getArgument(0));

        verificationReviewService.approve(30L, "admin", null, null, 2L, null);

        ArgumentCaptor<Person> personCaptor = ArgumentCaptor.forClass(Person.class);
        verify(personRepository).save(personCaptor.capture());
        Person savedPerson = personCaptor.getValue();
        assertThat(savedPerson.getFirstName()).isEqualTo("Yuva");
        assertThat(savedPerson.getMiddleName()).isEqualTo("Raj");
        assertThat(savedPerson.getLastName()).isEqualTo("Bhatta");
        assertThat(savedPerson.getBirthDate()).isEqualTo(LocalDate.of(1995, 6, 15));
        assertThat(savedPerson.getGenerationNumber()).isEqualTo(4);
    }

    @Test
    void approveWithCreateAsChildOfFatherIdWhenFatherHasNoGenerationNumberLeavesItNull() {
        UserAccount account = new UserAccount();
        VerificationRequest request = new VerificationRequest();
        request.setUserAccount(account);
        request.setSubmittedFullName("Yuva Bhatta");
        when(verificationRequestRepository.findById(31L)).thenReturn(Optional.of(request));

        Person father = new Person();
        father.setId(3L);
        // generationNumber deliberately left null
        when(personRepository.findById(3L)).thenReturn(Optional.of(father));
        when(personRepository.save(any(Person.class))).thenAnswer(invocation -> invocation.getArgument(0));

        verificationReviewService.approve(31L, "admin", null, null, 3L, null);

        ArgumentCaptor<Person> personCaptor = ArgumentCaptor.forClass(Person.class);
        verify(personRepository).save(personCaptor.capture());
        assertThat(personCaptor.getValue().getGenerationNumber()).isNull();
    }

    @Test
    void approveWithCreateAsChildOfFatherIdCallsSaveRelationshipWithAutoLinksWithFatherRelationshipType() {
        UserAccount account = new UserAccount();
        VerificationRequest request = new VerificationRequest();
        request.setUserAccount(account);
        request.setSubmittedFullName("Yuva Bhatta");
        when(verificationRequestRepository.findById(32L)).thenReturn(Optional.of(request));

        Person father = new Person();
        father.setId(4L);
        when(personRepository.findById(4L)).thenReturn(Optional.of(father));
        Person savedNewPerson = new Person();
        savedNewPerson.setId(500L);
        when(personRepository.save(any(Person.class))).thenReturn(savedNewPerson);

        verificationReviewService.approve(32L, "admin", null, null, 4L, null);

        verify(relationshipService).saveRelationshipWithAutoLinks(savedNewPerson, father, RelationshipType.FATHER);
    }

    @Test
    void approveWithCreateAsChildOfFatherIdAlsoLinksTheMatchedMotherByDefault() {
        UserAccount account = new UserAccount();
        VerificationRequest request = new VerificationRequest();
        request.setUserAccount(account);
        request.setSubmittedFullName("Yuva Bhatta");
        request.setMotherName("Sita Bhatta");
        when(verificationRequestRepository.findById(40L)).thenReturn(Optional.of(request));

        Person father = new Person();
        father.setId(6L);
        when(personRepository.findById(6L)).thenReturn(Optional.of(father));
        Person savedNewPerson = new Person();
        savedNewPerson.setId(510L);
        when(personRepository.save(any(Person.class))).thenReturn(savedNewPerson);

        Person mother = new Person();
        mother.setId(741L);
        when(familyMatchService.findSpouseMatchingName(father, "Sita Bhatta")).thenReturn(Optional.of(mother));

        // linkMatchedMother omitted (null) -- defaults to linking the match.
        verificationReviewService.approve(40L, "admin", null, null, 6L, null);

        verify(relationshipService).saveRelationshipWithAutoLinks(savedNewPerson, father, RelationshipType.FATHER);
        verify(relationshipService).saveRelationshipWithAutoLinks(savedNewPerson, mother, RelationshipType.MOTHER);
    }

    @Test
    void approveWithLinkMatchedMotherFalseSkipsTheMotherLinkEvenWhenAMatchExists() {
        UserAccount account = new UserAccount();
        VerificationRequest request = new VerificationRequest();
        request.setUserAccount(account);
        request.setSubmittedFullName("Yuva Bhatta");
        request.setMotherName("Sita Bhatta");
        when(verificationRequestRepository.findById(41L)).thenReturn(Optional.of(request));

        Person father = new Person();
        father.setId(7L);
        when(personRepository.findById(7L)).thenReturn(Optional.of(father));
        Person savedNewPerson = new Person();
        savedNewPerson.setId(511L);
        when(personRepository.save(any(Person.class))).thenReturn(savedNewPerson);

        verificationReviewService.approve(41L, "admin", null, null, 7L, false);

        verify(relationshipService).saveRelationshipWithAutoLinks(savedNewPerson, father, RelationshipType.FATHER);
        verify(relationshipService, never()).saveRelationshipWithAutoLinks(any(), any(), org.mockito.ArgumentMatchers.eq(RelationshipType.MOTHER));
        verifyNoInteractions(familyMatchService);
    }

    @Test
    void approveWithCreateAsChildOfFatherIdSkipsTheMotherLinkWhenNoSpouseMatches() {
        UserAccount account = new UserAccount();
        VerificationRequest request = new VerificationRequest();
        request.setUserAccount(account);
        request.setSubmittedFullName("Yuva Bhatta");
        request.setMotherName("Sita Bhatta");
        when(verificationRequestRepository.findById(42L)).thenReturn(Optional.of(request));

        Person father = new Person();
        father.setId(8L);
        when(personRepository.findById(8L)).thenReturn(Optional.of(father));
        Person savedNewPerson = new Person();
        savedNewPerson.setId(512L);
        when(personRepository.save(any(Person.class))).thenReturn(savedNewPerson);
        when(familyMatchService.findSpouseMatchingName(father, "Sita Bhatta")).thenReturn(Optional.empty());

        verificationReviewService.approve(42L, "admin", null, null, 8L, true);

        verify(relationshipService, never()).saveRelationshipWithAutoLinks(any(), any(), org.mockito.ArgumentMatchers.eq(RelationshipType.MOTHER));
    }

    @Test
    void approveWithCreateAsChildOfFatherIdCreatesVerifiedLinkToTheNewPerson() {
        UserAccount account = new UserAccount();
        VerificationRequest request = new VerificationRequest();
        request.setUserAccount(account);
        request.setSubmittedFullName("Yuva Bhatta");
        when(verificationRequestRepository.findById(33L)).thenReturn(Optional.of(request));

        Person father = new Person();
        father.setId(5L);
        when(personRepository.findById(5L)).thenReturn(Optional.of(father));
        Person savedNewPerson = new Person();
        savedNewPerson.setId(501L);
        when(personRepository.save(any(Person.class))).thenReturn(savedNewPerson);

        verificationReviewService.approve(33L, "admin", null, null, 5L, null);

        verify(userPersonLinkService).createVerifiedLink(account, savedNewPerson);
    }

    @Test
    void approveThrowsWhenCreateAsChildOfFatherIdDoesNotExist() {
        UserAccount account = new UserAccount();
        VerificationRequest request = new VerificationRequest();
        request.setUserAccount(account);
        when(verificationRequestRepository.findById(34L)).thenReturn(Optional.of(request));
        when(personRepository.findById(9999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> verificationReviewService.approve(34L, "admin", null, null, 9999L, null))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void approveSurfacesIllegalArgumentExceptionFromUserPersonLinkServiceGuard() {
        UserAccount account = new UserAccount();
        VerificationRequest request = new VerificationRequest();
        request.setUserAccount(account);
        when(verificationRequestRepository.findById(35L)).thenReturn(Optional.of(request));

        Person person = new Person();
        person.setId(78L);
        when(personRepository.findById(78L)).thenReturn(Optional.of(person));
        when(userPersonLinkService.createVerifiedLink(account, person))
                .thenThrow(new IllegalArgumentException("This account is already linked to a person. Unlink it first."));

        assertThatThrownBy(() -> verificationReviewService.approve(35L, "admin", null, 78L, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already linked to a person");
    }

    @Test
    void rejectMarksRequestRejectedAndDisablesAccount() {
        UserAccount account = new UserAccount();
        VerificationRequest request = new VerificationRequest();
        request.setUserAccount(account);
        when(verificationRequestRepository.findById(2L)).thenReturn(Optional.of(request));

        verificationReviewService.reject(2L, "admin", "no lineage match");

        assertThat(request.getStatus()).isEqualTo(VerificationStatus.REJECTED);
        assertThat(account.getStatus()).isEqualTo(UserAccountStatus.DISABLED);
    }

    @Test
    void requestMoreInfoLeavesAccountStatusUnchanged() {
        UserAccount account = new UserAccount();
        account.setStatus(UserAccountStatus.PENDING_EMAIL_VERIFICATION);
        VerificationRequest request = new VerificationRequest();
        request.setUserAccount(account);
        when(verificationRequestRepository.findById(3L)).thenReturn(Optional.of(request));

        verificationReviewService.requestMoreInfo(3L, "admin", "please clarify father's name");

        assertThat(request.getStatus()).isEqualTo(VerificationStatus.NEEDS_MORE_INFO);
        assertThat(account.getStatus()).isEqualTo(UserAccountStatus.PENDING_EMAIL_VERIFICATION);
        verify(userAccountRepository, never()).save(any());
    }

    @Test
    void throwsWhenVerificationRequestNotFound() {
        when(verificationRequestRepository.findById(99L)).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> verificationReviewService.approve(99L, "admin", null, null, null, null));
    }

    @Test
    void capturesReviewerAndTimestampConsistentlyAcrossActions() {
        UserAccount account = new UserAccount();
        VerificationRequest request = new VerificationRequest();
        request.setUserAccount(account);
        when(verificationRequestRepository.findById(4L)).thenReturn(Optional.of(request));

        verificationReviewService.reject(4L, "reviewer-x", null);

        ArgumentCaptor<VerificationRequest> captor = ArgumentCaptor.forClass(VerificationRequest.class);
        verify(verificationRequestRepository).save(captor.capture());
        assertThat(captor.getValue().getReviewedByUsername()).isEqualTo("reviewer-x");
        assertThat(captor.getValue().getReviewedAt()).isNotNull();
    }

    @Test
    void approveWithLinkedPersonIdTransfersAPendingPhotoToARealPersonPhoto() {
        UserAccount account = new UserAccount();
        VerificationRequest request = new VerificationRequest();
        request.setUserAccount(account);
        request.setPendingPhotoStorageKey("pending-abc.jpg");
        when(verificationRequestRepository.findById(30L)).thenReturn(Optional.of(request));

        Person person = new Person();
        person.setId(88L);
        when(personRepository.findById(88L)).thenReturn(Optional.of(person));
        when(photoStorageService.sizeOf("pending-abc.jpg")).thenReturn(12345L);

        verificationReviewService.approve(30L, "admin", null, 88L, null, null);

        ArgumentCaptor<com.familytree.entity.PersonPhoto> captor =
                ArgumentCaptor.forClass(com.familytree.entity.PersonPhoto.class);
        verify(personPhotoRepository).save(captor.capture());
        assertThat(captor.getValue().getPerson()).isEqualTo(person);
        assertThat(captor.getValue().getUploadedBy()).isEqualTo(account);
        assertThat(captor.getValue().getStorageKey()).isEqualTo("pending-abc.jpg");
        assertThat(captor.getValue().getFileSizeBytes()).isEqualTo(12345L);
        assertThat(captor.getValue().getMimeType()).isEqualTo(ImageReencodeService.OUTPUT_MIME_TYPE);
        verify(photoStorageService, never()).delete(any());
        // Otherwise a later reject() on this same (already-approved)
        // request would have a live storageKey to delete out from under
        // the PersonPhoto that now independently owns that file.
        assertThat(request.getPendingPhotoStorageKey()).isNull();
    }

    @Test
    void approveWithNoMatchAndAPendingPhotoDeletesTheOrphanedFileRatherThanKeepingIt() {
        // No linkedPersonId, no createAsChildOfFatherId -- nobody to
        // attach the photo to.
        UserAccount account = new UserAccount();
        VerificationRequest request = new VerificationRequest();
        request.setUserAccount(account);
        request.setPendingPhotoStorageKey("pending-orphan.jpg");
        when(verificationRequestRepository.findById(31L)).thenReturn(Optional.of(request));

        verificationReviewService.approve(31L, "admin", null, null, null, null);

        verify(photoStorageService).delete("pending-orphan.jpg");
        verify(personPhotoRepository, never()).save(any());
        assertThat(request.getPendingPhotoStorageKey()).isNull();
    }

    @Test
    void approveWithNoPendingPhotoTouchesNeitherStorageNorThePersonPhotoRepository() {
        UserAccount account = new UserAccount();
        VerificationRequest request = new VerificationRequest();
        request.setUserAccount(account);
        when(verificationRequestRepository.findById(32L)).thenReturn(Optional.of(request));

        verificationReviewService.approve(32L, "admin", null, null, null, null);

        verifyNoInteractions(photoStorageService, personPhotoRepository);
    }

    @Test
    void rejectDeletesAnOrphanedPendingPhotoSinceNoPersonWillEverExistToOwnIt() {
        UserAccount account = new UserAccount();
        VerificationRequest request = new VerificationRequest();
        request.setUserAccount(account);
        request.setPendingPhotoStorageKey("pending-rejected.jpg");
        when(verificationRequestRepository.findById(33L)).thenReturn(Optional.of(request));

        verificationReviewService.reject(33L, "admin", "not a match");

        verify(photoStorageService).delete("pending-rejected.jpg");
    }

    @Test
    void rejectWithNoPendingPhotoNeverTouchesStorage() {
        UserAccount account = new UserAccount();
        VerificationRequest request = new VerificationRequest();
        request.setUserAccount(account);
        when(verificationRequestRepository.findById(34L)).thenReturn(Optional.of(request));

        verificationReviewService.reject(34L, "admin", null);

        verifyNoInteractions(photoStorageService);
    }

}
