package com.familytree.controller;

import com.familytree.dto.PersonPhotoDto;
import com.familytree.entity.Person;
import com.familytree.entity.PersonPhoto;
import com.familytree.entity.UserAccount;
import com.familytree.entity.UserPersonLinkStatus;
import com.familytree.repository.UserAccountRepository;
import com.familytree.repository.UserPersonLinkRepository;
import com.familytree.services.PersonPhotoService;
import com.familytree.services.ViewerContextResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonPhotoControllerTest {

    @Mock
    private PersonPhotoService personPhotoService;
    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private UserPersonLinkRepository userPersonLinkRepository;
    @Mock
    private Authentication authentication;

    private PersonPhotoController controller() {
        return new PersonPhotoController(personPhotoService, userAccountRepository,
                new ViewerContextResolver(userAccountRepository, userPersonLinkRepository));
    }

    @Test
    void listMarksPhotosUploadedByTheCurrentViewerAsDeletable() {
        UserAccount viewer = accountWithId(10L, "member@example.com");
        authenticateAsNonAdmin("member@example.com", viewer);

        PersonPhoto ownPhoto = photo(1L, accountWithId(10L, "member@example.com"));
        PersonPhoto othersPhoto = photo(2L, accountWithId(20L, "other@example.com"));
        when(personPhotoService.list(5L)).thenReturn(List.of(ownPhoto, othersPhoto));

        List<PersonPhotoDto> result = controller().list(5L, authentication);

        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).canDelete()).isTrue();
        assertThat(result.get(1).id()).isEqualTo(2L);
        assertThat(result.get(1).canDelete()).isFalse();
    }

    @Test
    void listMarksEveryPhotoAsDeletableForAnAdminViewer() {
        authenticateAsAdmin("admin@example.com");
        PersonPhoto othersPhoto = photo(2L, accountWithId(20L, "other@example.com"));
        when(personPhotoService.list(5L)).thenReturn(List.of(othersPhoto));

        List<PersonPhotoDto> result = controller().list(5L, authentication);

        assertThat(result.get(0).canDelete()).isTrue();
    }

    @Test
    void uploadDelegatesToServiceAndReturns201WithTheMappedDto() {
        UserAccount viewer = accountWithId(10L, "member@example.com");
        authenticateAsNonAdmin("member@example.com", viewer);

        PersonPhoto saved = photo(1L, viewer);
        when(personPhotoService.upload(5L, null, "A caption", "member@example.com", false)).thenReturn(saved);

        ResponseEntity<PersonPhotoDto> response = controller().upload(5L, null, "A caption", authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().id()).isEqualTo(1L);
        assertThat(response.getBody().canDelete()).isTrue();
    }

    @Test
    void deleteDelegatesToServiceWithTheResolvedAdminFlag() {
        authenticateAsAdmin("admin@example.com");

        ResponseEntity<Void> response = controller().delete(5L, 1L, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(personPhotoService).delete(5L, 1L, "admin@example.com", true);
    }

    @Test
    void fileReturnsTheStoredBytesWithThePhotosContentTypeAndAPrivateCacheHeader() {
        when(personPhotoService.readFile(5L, 1L))
                .thenReturn(new PersonPhotoService.StoredPhotoFile(new byte[] {9, 8, 7}, "image/jpeg"));

        ResponseEntity<byte[]> response = controller().file(5L, 1L);

        assertThat(response.getBody()).containsExactly(9, 8, 7);
        assertThat(response.getHeaders().getContentType().toString()).isEqualTo("image/jpeg");
        assertThat(response.getHeaders().getCacheControl()).contains("private");
    }

    private void authenticateAsNonAdmin(String email, UserAccount account) {
        when(authentication.getName()).thenReturn(email);
        doReturn(List.<GrantedAuthority>of(new SimpleGrantedAuthority("ROLE_USER"))).when(authentication).getAuthorities();
        when(userAccountRepository.findByEmail(email)).thenReturn(Optional.of(account));
        when(userPersonLinkRepository.findByUserAccountId(account.getId())).thenReturn(List.of());
    }

    private void authenticateAsAdmin(String email) {
        when(authentication.getName()).thenReturn(email);
        doReturn(List.<GrantedAuthority>of(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(authentication).getAuthorities();
    }

    private PersonPhoto photo(Long id, UserAccount uploader) {
        PersonPhoto photo = new PersonPhoto();
        ReflectionTestUtils.setField(photo, "id", id);
        photo.setPerson(new Person());
        photo.setUploadedBy(uploader);
        photo.setStorageKey(id + ".jpg");
        photo.setMimeType("image/jpeg");
        photo.setFileSizeBytes(100);
        photo.setUploadedAt(LocalDateTime.now());
        return photo;
    }

    private UserAccount accountWithId(long id, String email) {
        UserAccount account = new UserAccount();
        ReflectionTestUtils.setField(account, "id", id);
        account.setEmail(email);
        return account;
    }
}
