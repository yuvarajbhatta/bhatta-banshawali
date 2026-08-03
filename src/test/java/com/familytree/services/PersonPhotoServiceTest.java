package com.familytree.services;

import com.familytree.entity.Person;
import com.familytree.entity.PersonPhoto;
import com.familytree.entity.UserAccount;
import com.familytree.entity.UserPersonLink;
import com.familytree.entity.UserPersonLinkStatus;
import com.familytree.repository.PersonPhotoRepository;
import com.familytree.repository.PersonRepository;
import com.familytree.repository.UserAccountRepository;
import com.familytree.repository.UserPersonLinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonPhotoServiceTest {

    @Mock
    private PersonPhotoRepository personPhotoRepository;
    @Mock
    private PersonRepository personRepository;
    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private UserPersonLinkRepository userPersonLinkRepository;
    @Mock
    private RelationshipService relationshipService;
    @Mock
    private AuditLogService auditLogService;

    @TempDir
    private Path storageDir;

    private PersonPhotoService service;

    private Person target;
    private Person uploaderPerson;
    private UserAccount uploaderAccount;

    @BeforeEach
    void setUp() {
        service = new PersonPhotoService(personPhotoRepository, personRepository, userAccountRepository,
                userPersonLinkRepository, relationshipService, new ImageReencodeService(), auditLogService,
                storageDir.toString());

        target = personWithId(1L);
        uploaderPerson = personWithId(2L);
        uploaderAccount = new UserAccount();
        ReflectionTestUtils.setField(uploaderAccount, "id", 10L);
        uploaderAccount.setEmail("uploader@example.com");

        // lenient: not every test exercises both lookups (e.g. the
        // readFile tests never call upload() at all), and strict stubbing
        // otherwise flags whichever one a given test doesn't reach.
        org.mockito.Mockito.lenient().when(personRepository.findById(1L)).thenReturn(Optional.of(target));
        org.mockito.Mockito.lenient().when(userAccountRepository.findByEmail("uploader@example.com"))
                .thenReturn(Optional.of(uploaderAccount));
    }

    @Test
    void allowsUploadingAPhotoOfYourself() {
        target = uploaderPerson; // person 2, same id as the uploader
        when(personRepository.findById(2L)).thenReturn(Optional.of(target));
        linkVerifiedPerson(uploaderAccount, uploaderPerson);
        when(personPhotoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PersonPhoto saved = service.upload(2L, validImageFile(), null, "uploader@example.com", false);

        assertThat(saved.getPerson()).isEqualTo(target);
        assertThat(saved.getMimeType()).isEqualTo(ImageReencodeService.OUTPUT_MIME_TYPE);
    }

    @Test
    void allowsUploadingAPhotoOfYourFather() {
        linkVerifiedPerson(uploaderAccount, uploaderPerson);
        stubEmptyRelationships();
        when(relationshipService.getFatherForPerson(uploaderPerson)).thenReturn(Optional.of(target));
        when(personPhotoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PersonPhoto saved = service.upload(1L, validImageFile(), "Dad in 1990", "uploader@example.com", false);

        assertThat(saved.getCaption()).isEqualTo("Dad in 1990");
    }

    @Test
    void allowsUploadingAPhotoOfASibling() {
        linkVerifiedPerson(uploaderAccount, uploaderPerson);
        when(relationshipService.getFatherForPerson(uploaderPerson)).thenReturn(Optional.empty());
        when(relationshipService.getMotherForPerson(uploaderPerson)).thenReturn(Optional.empty());
        when(relationshipService.getSpousesForPerson(uploaderPerson)).thenReturn(List.of());
        when(relationshipService.getChildrenForPerson(uploaderPerson)).thenReturn(List.of());
        when(relationshipService.getSiblingsForPerson(uploaderPerson)).thenReturn(List.of(target));
        when(personPhotoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PersonPhoto saved = service.upload(1L, validImageFile(), null, "uploader@example.com", false);

        assertThat(saved.getPerson()).isEqualTo(target);
    }

    @Test
    void rejectsUploadingAPhotoOfSomeoneOutsideImmediateFamily() {
        linkVerifiedPerson(uploaderAccount, uploaderPerson);
        stubEmptyRelationships();

        assertThatThrownBy(() -> service.upload(1L, validImageFile(), null, "uploader@example.com", false))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("immediate family");
        verifyNoInteractions(personPhotoRepository);
    }

    @Test
    void adminsCanUploadAPhotoOfAnyone() {
        // No verified link for this account at all -- admin bypasses the
        // immediate-family check entirely, so it's never even consulted.
        when(personPhotoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PersonPhoto saved = service.upload(1L, validImageFile(), null, "uploader@example.com", true);

        assertThat(saved.getPerson()).isEqualTo(target);
        verifyNoInteractions(relationshipService);
    }

    @Test
    void rejectsUploadFromAnUnlinkedNonAdminAccount() {
        when(userPersonLinkRepository.findByUserAccountId(10L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.upload(1L, validImageFile(), null, "uploader@example.com", false))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("linked");
    }

    @Test
    void rejectsUploadOnceThePersonAlreadyHasTheMaximumPhotos() {
        when(personPhotoRepository.countByPersonId(1L)).thenReturn((long) PersonPhotoService.MAX_PHOTOS_PER_PERSON);

        assertThatThrownBy(() -> service.upload(1L, validImageFile(), null, "uploader@example.com", true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maximum");
    }

    @Test
    void rejectsANonImageUpload() {
        MultipartFile notAnImage = new MockMultipartFile("file", "shell.jpg", "image/jpeg", "#!/bin/sh\n".getBytes());

        assertThatThrownBy(() -> service.upload(1L, notAnImage, null, "uploader@example.com", true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void savesTheReencodedFileToDiskUnderAServerGeneratedName() {
        when(personPhotoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PersonPhoto saved = service.upload(1L, validImageFile(), null, "uploader@example.com", true);

        assertThat(saved.getStorageKey()).endsWith(".jpg");
        assertThat(storageDir.resolve(saved.getStorageKey())).exists();
    }

    @Test
    void uploaderCanDeleteTheirOwnPhotoWithoutAnAuditLogEntry() {
        PersonPhoto photo = photoUploadedBy(uploaderAccount, "abc.jpg");
        when(personPhotoRepository.findById(5L)).thenReturn(Optional.of(photo));

        service.delete(1L, 5L, "uploader@example.com", false);

        verify(personPhotoRepository).deleteById(5L);
        verifyNoInteractions(auditLogService);
    }

    @Test
    void adminCanDeleteSomeoneElsesPhotoAndItIsAuditLogged() {
        UserAccount admin = new UserAccount();
        ReflectionTestUtils.setField(admin, "id", 99L);
        admin.setEmail("admin@example.com");
        when(userAccountRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));

        PersonPhoto photo = photoUploadedBy(uploaderAccount, "abc.jpg");
        when(personPhotoRepository.findById(5L)).thenReturn(Optional.of(photo));

        service.delete(1L, 5L, "admin@example.com", true);

        verify(personPhotoRepository).deleteById(5L);
        verify(auditLogService).record(
                org.mockito.ArgumentMatchers.eq(AuditLogService.ACTION_PHOTO_DELETED),
                org.mockito.ArgumentMatchers.eq(AuditLogService.ENTITY_PERSON_PHOTO),
                org.mockito.ArgumentMatchers.eq(5L),
                any(),
                org.mockito.ArgumentMatchers.eq("admin@example.com"));
    }

    @Test
    void rejectsDeleteFromSomeoneWhoIsNeitherTheUploaderNorAnAdmin() {
        UserAccount stranger = new UserAccount();
        ReflectionTestUtils.setField(stranger, "id", 77L);
        stranger.setEmail("stranger@example.com");
        when(userAccountRepository.findByEmail("stranger@example.com")).thenReturn(Optional.of(stranger));

        PersonPhoto photo = photoUploadedBy(uploaderAccount, "abc.jpg");
        when(personPhotoRepository.findById(5L)).thenReturn(Optional.of(photo));

        assertThatThrownBy(() -> service.delete(1L, 5L, "stranger@example.com", false))
                .isInstanceOf(ResponseStatusException.class);
        verify(personPhotoRepository, org.mockito.Mockito.never()).deleteById(any());
    }

    @Test
    void readFileReturnsTheStoredBytesAndMimeType() throws IOException {
        String storageKey = "existing.jpg";
        Files.write(storageDir.resolve(storageKey), new byte[] {1, 2, 3});
        PersonPhoto photo = photoUploadedBy(uploaderAccount, storageKey);
        when(personPhotoRepository.findById(5L)).thenReturn(Optional.of(photo));

        PersonPhotoService.StoredPhotoFile file = service.readFile(1L, 5L);

        assertThat(file.bytes()).containsExactly(1, 2, 3);
        assertThat(file.mimeType()).isEqualTo(ImageReencodeService.OUTPUT_MIME_TYPE);
    }

    @Test
    void readFileThrowsNotFoundWhenThePhotoBelongsToADifferentPerson() {
        PersonPhoto photo = photoUploadedBy(uploaderAccount, "abc.jpg");
        ReflectionTestUtils.setField(photo.getPerson(), "id", 999L);
        when(personPhotoRepository.findById(5L)).thenReturn(Optional.of(photo));

        assertThatThrownBy(() -> service.readFile(1L, 5L)).isInstanceOf(ResponseStatusException.class);
    }

    private void linkVerifiedPerson(UserAccount account, Person person) {
        UserPersonLink link = new UserPersonLink();
        link.setLinkStatus(UserPersonLinkStatus.VERIFIED);
        link.setPerson(person);
        when(userPersonLinkRepository.findByUserAccountId(account.getId())).thenReturn(List.of(link));
    }

    private void stubEmptyRelationships() {
        when(relationshipService.getFatherForPerson(uploaderPerson)).thenReturn(Optional.empty());
        when(relationshipService.getMotherForPerson(uploaderPerson)).thenReturn(Optional.empty());
        when(relationshipService.getSpousesForPerson(uploaderPerson)).thenReturn(List.of());
        when(relationshipService.getChildrenForPerson(uploaderPerson)).thenReturn(List.of());
        when(relationshipService.getSiblingsForPerson(uploaderPerson)).thenReturn(List.of());
    }

    private PersonPhoto photoUploadedBy(UserAccount uploader, String storageKey) {
        PersonPhoto photo = new PersonPhoto();
        ReflectionTestUtils.setField(photo, "id", 5L);
        photo.setPerson(target);
        photo.setUploadedBy(uploader);
        photo.setStorageKey(storageKey);
        photo.setMimeType(ImageReencodeService.OUTPUT_MIME_TYPE);
        photo.setFileSizeBytes(100);
        return photo;
    }

    private Person personWithId(long id) {
        Person person = new Person();
        ReflectionTestUtils.setField(person, "id", id);
        return person;
    }

    private MultipartFile validImageFile() {
        try {
            BufferedImage image = new BufferedImage(40, 30, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.setColor(Color.CYAN);
            g.fillRect(0, 0, 40, 30);
            g.dispose();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            ImageIO.write(image, "png", buffer);
            return new MockMultipartFile("file", "photo.png", "image/png", buffer.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
