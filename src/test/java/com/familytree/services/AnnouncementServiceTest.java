package com.familytree.services;

import com.familytree.dto.AnnouncementDto;
import com.familytree.entity.AnnouncementCategory;
import com.familytree.entity.AnnouncementPhoto;
import com.familytree.entity.AnnouncementPost;
import com.familytree.entity.AnnouncementStatus;
import com.familytree.entity.AppUser;
import com.familytree.entity.UserAccount;
import com.familytree.repository.AnnouncementPhotoRepository;
import com.familytree.repository.AnnouncementPostRepository;
import com.familytree.repository.AppUserRepository;
import com.familytree.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnnouncementServiceTest {

    @Mock
    private AnnouncementPostRepository announcementPostRepository;
    @Mock
    private AnnouncementPhotoRepository announcementPhotoRepository;
    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private AppUserRepository appUserRepository;

    @TempDir
    private Path storageDir;

    private AnnouncementService service;

    @BeforeEach
    void setUp() {
        service = new AnnouncementService(announcementPostRepository, announcementPhotoRepository,
                userAccountRepository, appUserRepository, storageDir.toString());
    }

    private AnnouncementPost post(long id, AnnouncementStatus status) {
        AnnouncementPost post = new AnnouncementPost();
        ReflectionTestUtils.setField(post, "id", id);
        post.setCategory(AnnouncementCategory.OBITUARY);
        post.setTitleEn("Title");
        post.setBodyEn("Body");
        post.setStatus(status);
        post.setPinned(false);
        post.setCreatedByUsername("admin");
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        if (status == AnnouncementStatus.PUBLISHED) {
            post.setPublishedAt(LocalDateTime.now());
        }
        return post;
    }

    @Test
    void listPublishedReturnsOnlyPublishedPostsWithTheirPhotos() {
        AnnouncementPost published = post(1L, AnnouncementStatus.PUBLISHED);
        when(announcementPostRepository.findAllByStatusOrderByPinnedDescPublishedAtDesc(AnnouncementStatus.PUBLISHED))
                .thenReturn(List.of(published));
        when(announcementPhotoRepository.findByPostIdOrderByUploadedAtDesc(1L)).thenReturn(List.of());

        List<AnnouncementDto> result = service.listPublished();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).category()).isEqualTo(AnnouncementCategory.OBITUARY);
    }

    @Test
    void unreadCountReturnsEveryPublishedPostWhenNeverSeenBefore() {
        UserAccount account = new UserAccount();
        account.setEmail("member@example.com");
        when(userAccountRepository.findByEmail("member@example.com")).thenReturn(Optional.of(account));
        when(announcementPostRepository.countByStatus(AnnouncementStatus.PUBLISHED)).thenReturn(3L);

        int count = service.unreadCount("member@example.com");

        assertThat(count).isEqualTo(3);
    }

    @Test
    void unreadCountOnlyCountsPostsPublishedAfterTheWatermark() {
        UserAccount account = new UserAccount();
        account.setEmail("member@example.com");
        LocalDateTime lastSeen = LocalDateTime.now().minusDays(1);
        account.setLastSeenAnnouncementsAt(lastSeen);
        when(userAccountRepository.findByEmail("member@example.com")).thenReturn(Optional.of(account));
        when(announcementPostRepository.countByStatusAndPublishedAtAfter(AnnouncementStatus.PUBLISHED, lastSeen)).thenReturn(2L);

        int count = service.unreadCount("member@example.com");

        assertThat(count).isEqualTo(2);
    }

    @Test
    void unreadCountIsZeroWhenNeitherUserAccountNorAppUserMatches() {
        when(userAccountRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());
        when(appUserRepository.findByUsername("nobody@example.com")).thenReturn(Optional.empty());

        assertThat(service.unreadCount("nobody@example.com")).isZero();
    }

    @Test
    void unreadCountFallsBackToAppUserWhenNoUserAccountRow() {
        // Today's admin logins are AppUser rows (no UserAccount at all) --
        // this is the exact case that produced a "badge never shows" bug.
        AppUser appUser = new AppUser();
        appUser.setUsername("admin");
        when(userAccountRepository.findByEmail("admin")).thenReturn(Optional.empty());
        when(appUserRepository.findByUsername("admin")).thenReturn(Optional.of(appUser));
        when(announcementPostRepository.countByStatus(AnnouncementStatus.PUBLISHED)).thenReturn(2L);

        assertThat(service.unreadCount("admin")).isEqualTo(2);
    }

    @Test
    void markSeenSetsTheWatermarkToNow() {
        UserAccount account = new UserAccount();
        account.setEmail("member@example.com");
        when(userAccountRepository.findByEmail("member@example.com")).thenReturn(Optional.of(account));

        service.markSeen("member@example.com");

        ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountRepository).save(captor.capture());
        assertThat(captor.getValue().getLastSeenAnnouncementsAt()).isNotNull();
    }

    @Test
    void markSeenIsANoOpWhenNeitherUserAccountNorAppUserMatches() {
        when(userAccountRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());
        when(appUserRepository.findByUsername("nobody@example.com")).thenReturn(Optional.empty());

        service.markSeen("nobody@example.com");

        verify(userAccountRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(appUserRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void markSeenFallsBackToAppUserWhenNoUserAccountRow() {
        AppUser appUser = new AppUser();
        appUser.setUsername("admin");
        when(userAccountRepository.findByEmail("admin")).thenReturn(Optional.empty());
        when(appUserRepository.findByUsername("admin")).thenReturn(Optional.of(appUser));

        service.markSeen("admin");

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(captor.capture());
        assertThat(captor.getValue().getLastSeenAnnouncementsAt()).isNotNull();
    }

    @Test
    void readPhotoFileReturnsBytesForAPublishedPost() throws Exception {
        AnnouncementPost published = post(1L, AnnouncementStatus.PUBLISHED);
        String storageKey = "photo.jpg";
        Files.write(storageDir.resolve(storageKey), new byte[] {1, 2, 3});
        AnnouncementPhoto photo = photoOn(published, storageKey);
        when(announcementPhotoRepository.findById(5L)).thenReturn(Optional.of(photo));

        AnnouncementService.StoredPhotoFile file = service.readPhotoFile(1L, 5L, false);

        assertThat(file.bytes()).containsExactly(1, 2, 3);
    }

    @Test
    void readPhotoFileHidesADraftPostsPhotoFromANonAdmin() {
        AnnouncementPost draft = post(1L, AnnouncementStatus.DRAFT);
        AnnouncementPhoto photo = photoOn(draft, "photo.jpg");
        when(announcementPhotoRepository.findById(5L)).thenReturn(Optional.of(photo));

        assertThatThrownBy(() -> service.readPhotoFile(1L, 5L, false)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void readPhotoFileAllowsAnAdminToSeeADraftPostsPhoto() throws Exception {
        AnnouncementPost draft = post(1L, AnnouncementStatus.DRAFT);
        String storageKey = "photo.jpg";
        Files.write(storageDir.resolve(storageKey), new byte[] {9});
        AnnouncementPhoto photo = photoOn(draft, storageKey);
        when(announcementPhotoRepository.findById(5L)).thenReturn(Optional.of(photo));

        AnnouncementService.StoredPhotoFile file = service.readPhotoFile(1L, 5L, true);

        assertThat(file.bytes()).containsExactly(9);
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
}
