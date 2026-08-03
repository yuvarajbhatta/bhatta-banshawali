package com.familytree.services;

import com.familytree.dto.AdminAnnouncementDto;
import com.familytree.dto.AdminAnnouncementRequestDto;
import com.familytree.dto.AnnouncementPhotoDto;
import com.familytree.entity.AnnouncementPhoto;
import com.familytree.entity.AnnouncementPost;
import com.familytree.entity.AnnouncementStatus;
import com.familytree.repository.AnnouncementPhotoRepository;
import com.familytree.repository.AnnouncementPostRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Admin CRUD + draft/publish workflow for AnnouncementPost, and the
 * photo attach/detach that goes with it -- the write side of News &
 * Alerts (see AnnouncementService for the member-facing read side).
 * Every mutation here is admin-only by construction (this service is
 * only ever reached via AdminAnnouncementController, itself gated by
 * SecurityConfig's blanket "/api/v1/admin/**" -> ROLE_ADMIN rule), so
 * unlike PersonPhotoService there's no self/immediate-family carve-out
 * to check.
 */
@Service
public class AnnouncementAdminService {

    static final int MAX_PHOTOS_PER_POST = 10;

    private final AnnouncementPostRepository announcementPostRepository;
    private final AnnouncementPhotoRepository announcementPhotoRepository;
    private final ImageReencodeService imageReencodeService;
    private final AuditLogService auditLogService;
    private final Path uploadsDirectory;

    public AnnouncementAdminService(AnnouncementPostRepository announcementPostRepository,
                                    AnnouncementPhotoRepository announcementPhotoRepository,
                                    ImageReencodeService imageReencodeService,
                                    AuditLogService auditLogService,
                                    @Value("${app.uploads.directory}") String uploadsDirectory) {
        this.announcementPostRepository = announcementPostRepository;
        this.announcementPhotoRepository = announcementPhotoRepository;
        this.imageReencodeService = imageReencodeService;
        this.auditLogService = auditLogService;
        this.uploadsDirectory = Path.of(uploadsDirectory).normalize();
    }

    public List<AdminAnnouncementDto> listAll() {
        return announcementPostRepository.findAllByOrderByUpdatedAtDesc().stream()
                .map(this::toDto)
                .toList();
    }

    public AdminAnnouncementDto getById(Long id) {
        return toDto(getOrThrow(id));
    }

    @Transactional
    public AdminAnnouncementDto create(AdminAnnouncementRequestDto request, String actorUsername) {
        AnnouncementPost post = new AnnouncementPost();
        applyContent(post, request);
        post.setStatus(AnnouncementStatus.DRAFT);
        post.setCreatedByUsername(actorUsername);
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        announcementPostRepository.save(post);

        auditLogService.record(AuditLogService.ACTION_ANNOUNCEMENT_CREATED, AuditLogService.ENTITY_ANNOUNCEMENT,
                post.getId(), "Created announcement \"" + post.getTitleEn() + "\"", actorUsername);

        return toDto(post);
    }

    @Transactional
    public AdminAnnouncementDto update(Long id, AdminAnnouncementRequestDto request, String actorUsername) {
        AnnouncementPost post = getOrThrow(id);
        applyContent(post, request);
        post.setUpdatedAt(LocalDateTime.now());
        announcementPostRepository.save(post);

        auditLogService.record(AuditLogService.ACTION_ANNOUNCEMENT_UPDATED, AuditLogService.ENTITY_ANNOUNCEMENT,
                id, "Edited announcement \"" + post.getTitleEn() + "\"", actorUsername);

        return toDto(post);
    }

    /** @throws IllegalArgumentException if the post is already published */
    @Transactional
    public AdminAnnouncementDto publish(Long id, String actorUsername) {
        AnnouncementPost post = getOrThrow(id);
        if (post.getStatus() == AnnouncementStatus.PUBLISHED) {
            throw new IllegalArgumentException("This announcement is already published.");
        }
        post.setStatus(AnnouncementStatus.PUBLISHED);
        post.setPublishedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        announcementPostRepository.save(post);

        auditLogService.record(AuditLogService.ACTION_ANNOUNCEMENT_STATUS_CHANGED, AuditLogService.ENTITY_ANNOUNCEMENT,
                id, "Published announcement \"" + post.getTitleEn() + "\"", actorUsername);

        return toDto(post);
    }

    /** @throws IllegalArgumentException if the post isn't published */
    @Transactional
    public AdminAnnouncementDto unpublish(Long id, String actorUsername) {
        AnnouncementPost post = getOrThrow(id);
        if (post.getStatus() != AnnouncementStatus.PUBLISHED) {
            throw new IllegalArgumentException("This announcement isn't published.");
        }
        post.setStatus(AnnouncementStatus.DRAFT);
        post.setUpdatedAt(LocalDateTime.now());
        announcementPostRepository.save(post);

        auditLogService.record(AuditLogService.ACTION_ANNOUNCEMENT_STATUS_CHANGED, AuditLogService.ENTITY_ANNOUNCEMENT,
                id, "Unpublished announcement \"" + post.getTitleEn() + "\"", actorUsername);

        return toDto(post);
    }

    @Transactional
    public void delete(Long id, String actorUsername) {
        AnnouncementPost post = getOrThrow(id);
        String title = post.getTitleEn();

        List<AnnouncementPhoto> photos = announcementPhotoRepository.findByPostIdOrderByUploadedAtDesc(id);
        photos.forEach(photo -> deleteFromDisk(photo.getStorageKey()));
        announcementPhotoRepository.deleteAll(photos);
        announcementPostRepository.delete(post);

        auditLogService.record(AuditLogService.ACTION_ANNOUNCEMENT_DELETED, AuditLogService.ENTITY_ANNOUNCEMENT,
                id, "Deleted announcement \"" + title + "\"", actorUsername);
    }

    @Transactional
    public AnnouncementPhotoDto uploadPhoto(Long postId, MultipartFile file, String caption, String actorUsername) {
        AnnouncementPost post = getOrThrow(postId);

        if (announcementPhotoRepository.countByPostId(postId) >= MAX_PHOTOS_PER_POST) {
            throw new IllegalArgumentException("This post already has the maximum of " + MAX_PHOTOS_PER_POST + " photos.");
        }

        byte[] original;
        try {
            original = file.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read the uploaded file.");
        }
        byte[] reencoded = imageReencodeService.reencode(original);

        String storageKey = UUID.randomUUID() + ".jpg";
        writeToDisk(storageKey, reencoded);

        AnnouncementPhoto photo = new AnnouncementPhoto();
        photo.setPost(post);
        photo.setUploadedByUsername(actorUsername);
        photo.setStorageKey(storageKey);
        photo.setMimeType(ImageReencodeService.OUTPUT_MIME_TYPE);
        photo.setFileSizeBytes(reencoded.length);
        photo.setCaption(blankToNull(caption));
        photo.setUploadedAt(LocalDateTime.now());
        announcementPhotoRepository.save(photo);

        return AnnouncementService.toPhotoDto(photo);
    }

    @Transactional
    public void deletePhoto(Long postId, Long photoId) {
        AnnouncementPhoto photo = announcementPhotoRepository.findById(photoId)
                .filter(candidate -> candidate.getPost().getId().equals(postId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo not found with id: " + photoId));

        deleteFromDisk(photo.getStorageKey());
        announcementPhotoRepository.deleteById(photoId);
    }

    private void applyContent(AnnouncementPost post, AdminAnnouncementRequestDto request) {
        post.setCategory(request.getCategory());
        post.setTitleEn(request.getTitleEn());
        post.setTitleNe(blankToNull(request.getTitleNe()));
        post.setBodyEn(request.getBodyEn());
        post.setBodyNe(blankToNull(request.getBodyNe()));
        post.setPinned(request.isPinned());
    }

    private AnnouncementPost getOrThrow(Long id) {
        return announcementPostRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Announcement not found with id: " + id));
    }

    private AdminAnnouncementDto toDto(AnnouncementPost post) {
        List<AnnouncementPhotoDto> photos = announcementPhotoRepository.findByPostIdOrderByUploadedAtDesc(post.getId())
                .stream()
                .map(AnnouncementService::toPhotoDto)
                .toList();
        return new AdminAnnouncementDto(
                post.getId(),
                post.getCategory(),
                post.getTitleEn(),
                post.getTitleNe(),
                post.getBodyEn(),
                post.getBodyNe(),
                post.getStatus(),
                post.isPinned(),
                post.getPublishedAt(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                photos
        );
    }

    private void writeToDisk(String storageKey, byte[] bytes) {
        try {
            Files.createDirectories(uploadsDirectory);
            Files.write(resolveSafely(storageKey), bytes);
        } catch (IOException e) {
            throw new IllegalStateException("Could not save the uploaded photo.", e);
        }
    }

    private void deleteFromDisk(String storageKey) {
        try {
            Files.deleteIfExists(resolveSafely(storageKey));
        } catch (IOException e) {
            // Best-effort -- an orphaned file left on disk is a cleanup
            // nuisance, not a reason to fail the delete the caller asked for.
        }
    }

    private Path resolveSafely(String storageKey) {
        Path resolved = uploadsDirectory.resolve(storageKey).normalize();
        if (!resolved.startsWith(uploadsDirectory)) {
            throw new IllegalArgumentException("Invalid storage key.");
        }
        return resolved;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
