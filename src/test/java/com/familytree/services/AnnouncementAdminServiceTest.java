package com.familytree.services;

import com.familytree.dto.AdminAnnouncementDto;
import com.familytree.dto.AdminAnnouncementRequestDto;
import com.familytree.dto.AnnouncementPhotoDto;
import com.familytree.entity.AnnouncementCategory;
import com.familytree.entity.AnnouncementPhoto;
import com.familytree.entity.AnnouncementPost;
import com.familytree.entity.AnnouncementStatus;
import com.familytree.repository.AnnouncementPhotoRepository;
import com.familytree.repository.AnnouncementPostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnnouncementAdminServiceTest {

    @Mock
    private AnnouncementPostRepository announcementPostRepository;
    @Mock
    private AnnouncementPhotoRepository announcementPhotoRepository;
    @Mock
    private AuditLogService auditLogService;

    @TempDir
    private Path storageDir;

    private AnnouncementAdminService service;

    @BeforeEach
    void setUp() {
        service = new AnnouncementAdminService(announcementPostRepository, announcementPhotoRepository,
                new ImageReencodeService(), auditLogService, storageDir.toString());
    }

    private AnnouncementPost post(long id, AnnouncementStatus status) {
        AnnouncementPost post = new AnnouncementPost();
        ReflectionTestUtils.setField(post, "id", id);
        post.setCategory(AnnouncementCategory.FAMILY_NEWS);
        post.setTitleEn("Title");
        post.setBodyEn("Body");
        post.setStatus(status);
        post.setCreatedByUsername("admin");
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        return post;
    }

    private AdminAnnouncementRequestDto request() {
        AdminAnnouncementRequestDto request = new AdminAnnouncementRequestDto();
        request.setCategory(AnnouncementCategory.CELEBRATION);
        request.setTitleEn("New Title");
        request.setBodyEn("New body text.");
        return request;
    }

    @Test
    void createSavesAsDraftAndLogsIt() {
        AdminAnnouncementDto result = service.create(request(), "admin");

        ArgumentCaptor<AnnouncementPost> captor = ArgumentCaptor.forClass(AnnouncementPost.class);
        verify(announcementPostRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(AnnouncementStatus.DRAFT);
        assertThat(captor.getValue().getCategory()).isEqualTo(AnnouncementCategory.CELEBRATION);
        assertThat(result.status()).isEqualTo(AnnouncementStatus.DRAFT);
        verify(auditLogService).record(AuditLogService.ACTION_ANNOUNCEMENT_CREATED, AuditLogService.ENTITY_ANNOUNCEMENT,
                captor.getValue().getId(), "Created announcement \"New Title\"", "admin");
    }

    @Test
    void updateEditsContentAndBlankNepaliBecomesNull() {
        AnnouncementPost existing = post(1L, AnnouncementStatus.DRAFT);
        when(announcementPostRepository.findById(1L)).thenReturn(Optional.of(existing));

        AdminAnnouncementRequestDto request = request();
        request.setTitleNe("  ");
        request.setBodyNe("");

        service.update(1L, request, "admin");

        assertThat(existing.getTitleEn()).isEqualTo("New Title");
        assertThat(existing.getTitleNe()).isNull();
        assertThat(existing.getBodyNe()).isNull();
        verify(auditLogService).record(AuditLogService.ACTION_ANNOUNCEMENT_UPDATED, AuditLogService.ENTITY_ANNOUNCEMENT,
                1L, "Edited announcement \"New Title\"", "admin");
    }

    @Test
    void publishSetsPublishedAtAndStatus() {
        AnnouncementPost existing = post(1L, AnnouncementStatus.DRAFT);
        when(announcementPostRepository.findById(1L)).thenReturn(Optional.of(existing));

        service.publish(1L, "admin");

        assertThat(existing.getStatus()).isEqualTo(AnnouncementStatus.PUBLISHED);
        assertThat(existing.getPublishedAt()).isNotNull();
        verify(auditLogService).record(AuditLogService.ACTION_ANNOUNCEMENT_STATUS_CHANGED, AuditLogService.ENTITY_ANNOUNCEMENT,
                1L, "Published announcement \"Title\"", "admin");
    }

    @Test
    void publishThrowsWhenAlreadyPublished() {
        AnnouncementPost existing = post(1L, AnnouncementStatus.PUBLISHED);
        when(announcementPostRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.publish(1L, "admin")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unpublishMovesPublishedBackToDraft() {
        AnnouncementPost existing = post(1L, AnnouncementStatus.PUBLISHED);
        when(announcementPostRepository.findById(1L)).thenReturn(Optional.of(existing));

        service.unpublish(1L, "admin");

        assertThat(existing.getStatus()).isEqualTo(AnnouncementStatus.DRAFT);
    }

    @Test
    void unpublishThrowsWhenNotPublished() {
        AnnouncementPost existing = post(1L, AnnouncementStatus.DRAFT);
        when(announcementPostRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.unpublish(1L, "admin")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deleteRemovesThePostItsPhotosAndLogsIt() throws IOException {
        AnnouncementPost existing = post(1L, AnnouncementStatus.DRAFT);
        when(announcementPostRepository.findById(1L)).thenReturn(Optional.of(existing));

        String storageKey = "photo.jpg";
        Files.write(storageDir.resolve(storageKey), new byte[] {1, 2, 3});
        AnnouncementPhoto photo = photoOn(existing, storageKey);
        when(announcementPhotoRepository.findByPostIdOrderByUploadedAtDesc(1L)).thenReturn(List.of(photo));

        service.delete(1L, "admin");

        verify(announcementPostRepository).delete(existing);
        verify(announcementPhotoRepository).deleteAll(List.of(photo));
        assertThat(storageDir.resolve(storageKey)).doesNotExist();
        verify(auditLogService).record(AuditLogService.ACTION_ANNOUNCEMENT_DELETED, AuditLogService.ENTITY_ANNOUNCEMENT,
                1L, "Deleted announcement \"Title\"", "admin");
    }

    @Test
    void getByIdThrowsWhenMissing() {
        when(announcementPostRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(99L)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void listAllOrdersByUpdatedAtDescending() {
        AnnouncementPost first = post(1L, AnnouncementStatus.DRAFT);
        AnnouncementPost second = post(2L, AnnouncementStatus.PUBLISHED);
        when(announcementPostRepository.findAllByOrderByUpdatedAtDesc()).thenReturn(List.of(second, first));

        List<AdminAnnouncementDto> result = service.listAll();

        assertThat(result).extracting(AdminAnnouncementDto::id).containsExactly(2L, 1L);
    }

    @Test
    void uploadPhotoSavesTheReencodedFileToDiskUnderAServerGeneratedName() {
        AnnouncementPost existing = post(1L, AnnouncementStatus.DRAFT);
        when(announcementPostRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(announcementPhotoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AnnouncementPhotoDto saved = service.uploadPhoto(1L, validImageFile(), "A caption", "admin");

        assertThat(saved.caption()).isEqualTo("A caption");
        ArgumentCaptor<AnnouncementPhoto> captor = ArgumentCaptor.forClass(AnnouncementPhoto.class);
        verify(announcementPhotoRepository).save(captor.capture());
        assertThat(captor.getValue().getStorageKey()).endsWith(".jpg");
        assertThat(storageDir.resolve(captor.getValue().getStorageKey())).exists();
    }

    @Test
    void uploadPhotoRejectsOnceThePostAlreadyHasTheMaximum() {
        AnnouncementPost existing = post(1L, AnnouncementStatus.DRAFT);
        when(announcementPostRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(announcementPhotoRepository.countByPostId(1L)).thenReturn((long) AnnouncementAdminService.MAX_PHOTOS_PER_POST);

        assertThatThrownBy(() -> service.uploadPhoto(1L, validImageFile(), null, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maximum");
    }

    @Test
    void uploadPhotoRejectsANonImageFile() {
        AnnouncementPost existing = post(1L, AnnouncementStatus.DRAFT);
        when(announcementPostRepository.findById(1L)).thenReturn(Optional.of(existing));
        MultipartFile notAnImage = new MockMultipartFile("file", "shell.jpg", "image/jpeg", "#!/bin/sh\n".getBytes());

        assertThatThrownBy(() -> service.uploadPhoto(1L, notAnImage, null, "admin"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deletePhotoRemovesTheFileAndRow() throws IOException {
        AnnouncementPost existing = post(1L, AnnouncementStatus.DRAFT);
        String storageKey = "photo.jpg";
        Files.write(storageDir.resolve(storageKey), new byte[] {1, 2, 3});
        AnnouncementPhoto photo = photoOn(existing, storageKey);
        when(announcementPhotoRepository.findById(5L)).thenReturn(Optional.of(photo));

        service.deletePhoto(1L, 5L);

        verify(announcementPhotoRepository).deleteById(5L);
        assertThat(storageDir.resolve(storageKey)).doesNotExist();
    }

    @Test
    void deletePhotoThrowsWhenThePhotoBelongsToADifferentPost() {
        AnnouncementPost otherPost = post(2L, AnnouncementStatus.DRAFT);
        AnnouncementPhoto otherPhoto = photoOn(otherPost, "other.jpg");
        ReflectionTestUtils.setField(otherPhoto, "id", 6L);
        when(announcementPhotoRepository.findById(6L)).thenReturn(Optional.of(otherPhoto));

        assertThatThrownBy(() -> service.deletePhoto(1L, 6L)).isInstanceOf(ResponseStatusException.class);
        verify(announcementPhotoRepository, never()).deleteById(any());
    }

    private AnnouncementPhoto photoOn(AnnouncementPost post, String storageKey) {
        AnnouncementPhoto photo = new AnnouncementPhoto();
        ReflectionTestUtils.setField(photo, "id", 5L);
        photo.setPost(post);
        photo.setUploadedByUsername("admin");
        photo.setStorageKey(storageKey);
        photo.setMimeType(ImageReencodeService.OUTPUT_MIME_TYPE);
        photo.setFileSizeBytes(3);
        return photo;
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
