package com.familytree.services;

import com.familytree.dto.AnnouncementDto;
import com.familytree.dto.AnnouncementPhotoDto;
import com.familytree.entity.AnnouncementPhoto;
import com.familytree.entity.AnnouncementPost;
import com.familytree.entity.AnnouncementStatus;
import com.familytree.entity.AppUser;
import com.familytree.entity.UserAccount;
import com.familytree.repository.AnnouncementPhotoRepository;
import com.familytree.repository.AnnouncementPostRepository;
import com.familytree.repository.AppUserRepository;
import com.familytree.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Member-facing read side of News & Alerts -- published posts, the
 * unread-badge count, and marking the feed seen. Admin authoring lives
 * in AnnouncementAdminService.
 */
@Service
public class AnnouncementService {

    private final AnnouncementPostRepository announcementPostRepository;
    private final AnnouncementPhotoRepository announcementPhotoRepository;
    private final UserAccountRepository userAccountRepository;
    private final AppUserRepository appUserRepository;
    private final Path uploadsDirectory;

    public AnnouncementService(AnnouncementPostRepository announcementPostRepository,
                               AnnouncementPhotoRepository announcementPhotoRepository,
                               UserAccountRepository userAccountRepository,
                               AppUserRepository appUserRepository,
                               @Value("${app.uploads.directory}") String uploadsDirectory) {
        this.announcementPostRepository = announcementPostRepository;
        this.announcementPhotoRepository = announcementPhotoRepository;
        this.userAccountRepository = userAccountRepository;
        this.appUserRepository = appUserRepository;
        this.uploadsDirectory = Path.of(uploadsDirectory).normalize();
    }

    public List<AnnouncementDto> listPublished() {
        return announcementPostRepository.findAllByStatusOrderByPinnedDescPublishedAtDesc(AnnouncementStatus.PUBLISHED)
                .stream()
                .map(post -> toDto(post, announcementPhotoRepository.findByPostIdOrderByUploadedAtDesc(post.getId())))
                .toList();
    }

    /**
     * Accounts with no watermark yet (never opened the tab) see every
     * published post as unread. Checks UserAccount first, then AppUser --
     * today's admin accounts are AppUser rows (see the legacy Thymeleaf
     * login), which have no UserAccount at all, so without this fallback
     * the badge could never track a watermark for them.
     */
    public int unreadCount(String viewerEmail) {
        Optional<UserAccount> userAccount = userAccountRepository.findByEmail(viewerEmail);
        if (userAccount.isPresent()) {
            return countUnreadSince(userAccount.get().getLastSeenAnnouncementsAt());
        }
        Optional<AppUser> appUser = appUserRepository.findByUsername(viewerEmail);
        if (appUser.isPresent()) {
            return countUnreadSince(appUser.get().getLastSeenAnnouncementsAt());
        }
        return 0;
    }

    private int countUnreadSince(LocalDateTime lastSeen) {
        long count = lastSeen == null
                ? announcementPostRepository.countByStatus(AnnouncementStatus.PUBLISHED)
                : announcementPostRepository.countByStatusAndPublishedAtAfter(AnnouncementStatus.PUBLISHED, lastSeen);
        return (int) count;
    }

    @Transactional
    public void markSeen(String viewerEmail) {
        Optional<UserAccount> userAccount = userAccountRepository.findByEmail(viewerEmail);
        if (userAccount.isPresent()) {
            userAccount.get().setLastSeenAnnouncementsAt(LocalDateTime.now());
            userAccountRepository.save(userAccount.get());
            return;
        }
        appUserRepository.findByUsername(viewerEmail).ifPresent(appUser -> {
            appUser.setLastSeenAnnouncementsAt(LocalDateTime.now());
            appUserRepository.save(appUser);
        });
    }

    public record StoredPhotoFile(byte[] bytes, String mimeType) {
    }

    /**
     * Draft posts' photos are only visible to admins (e.g. previewing
     * before publishing) -- everyone else gets 404 rather than being able
     * to probe photo IDs on unpublished posts.
     */
    public StoredPhotoFile readPhotoFile(Long postId, Long photoId, boolean viewerIsAdmin) {
        AnnouncementPhoto photo = announcementPhotoRepository.findById(photoId)
                .filter(candidate -> candidate.getPost().getId().equals(postId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo not found with id: " + photoId));

        if (photo.getPost().getStatus() != AnnouncementStatus.PUBLISHED && !viewerIsAdmin) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo not found with id: " + photoId);
        }

        return new StoredPhotoFile(readFromDisk(photo.getStorageKey()), photo.getMimeType());
    }

    private byte[] readFromDisk(String storageKey) {
        try {
            return Files.readAllBytes(resolveSafely(storageKey));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo file is missing.");
        }
    }

    // storageKey is always server-generated, never client-controlled --
    // defense-in-depth, same as PersonPhotoService.
    private Path resolveSafely(String storageKey) {
        Path resolved = uploadsDirectory.resolve(storageKey).normalize();
        if (!resolved.startsWith(uploadsDirectory)) {
            throw new IllegalArgumentException("Invalid storage key.");
        }
        return resolved;
    }

    private static AnnouncementDto toDto(AnnouncementPost post, List<AnnouncementPhoto> photos) {
        return new AnnouncementDto(
                post.getId(),
                post.getCategory(),
                post.getTitleEn(),
                post.getTitleNe(),
                post.getBodyEn(),
                post.getBodyNe(),
                post.isPinned(),
                post.getPublishedAt(),
                photos.stream().map(AnnouncementService::toPhotoDto).toList()
        );
    }

    static AnnouncementPhotoDto toPhotoDto(AnnouncementPhoto photo) {
        return new AnnouncementPhotoDto(photo.getId(), photo.getCaption(), photo.getUploadedAt());
    }
}
