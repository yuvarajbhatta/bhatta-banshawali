package com.familytree.controller;

import com.familytree.dto.PersonPhotoDto;
import com.familytree.entity.PersonPhoto;
import com.familytree.entity.UserAccount;
import com.familytree.repository.UserAccountRepository;
import com.familytree.services.PersonPhotoService;
import com.familytree.services.ViewerContext;
import com.familytree.services.ViewerContextResolver;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.List;

/**
 * A person's picture album (docs/06-ui-ux-specification.md dashboard).
 * Authenticated-only, same as PersonCorrectionController -- viewing is
 * open to any authenticated member, upload/delete authorization is
 * enforced inside PersonPhotoService (self/immediate family, or admin).
 */
@RestController
@RequestMapping("/api/v1/persons/{personId}/photos")
public class PersonPhotoController {

    private final PersonPhotoService personPhotoService;
    private final UserAccountRepository userAccountRepository;
    private final ViewerContextResolver viewerContextResolver;

    public PersonPhotoController(PersonPhotoService personPhotoService, UserAccountRepository userAccountRepository,
                                 ViewerContextResolver viewerContextResolver) {
        this.personPhotoService = personPhotoService;
        this.userAccountRepository = userAccountRepository;
        this.viewerContextResolver = viewerContextResolver;
    }

    @GetMapping
    public List<PersonPhotoDto> list(@PathVariable Long personId, Authentication authentication) {
        ViewerContext viewer = viewerContextResolver.resolve(authentication);
        Long viewerAccountId = currentAccountId(authentication);
        return personPhotoService.list(personId).stream()
                .map(photo -> toDto(photo, viewer.isAdmin(), viewerAccountId))
                .toList();
    }

    @PostMapping
    public ResponseEntity<PersonPhotoDto> upload(@PathVariable Long personId,
                                                 @RequestParam("file") MultipartFile file,
                                                 @RequestParam(value = "caption", required = false) String caption,
                                                 Authentication authentication) {
        ViewerContext viewer = viewerContextResolver.resolve(authentication);
        PersonPhoto saved = personPhotoService.upload(personId, file, caption, authentication.getName(), viewer.isAdmin());
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved, viewer.isAdmin(), currentAccountId(authentication)));
    }

    @GetMapping("/{photoId}/file")
    public ResponseEntity<byte[]> file(@PathVariable Long personId, @PathVariable Long photoId) {
        PersonPhotoService.StoredPhotoFile storedFile = personPhotoService.readFile(personId, photoId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(storedFile.mimeType()))
                // "private" (not "public"): this app sits behind a shared
                // Cloudflare Tunnel edge, and these photos are only visible
                // to authenticated members -- a shared/CDN cache honoring
                // "public" could serve one member's family photo back to a
                // different, unauthorized caller.
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePrivate())
                .body(storedFile.bytes());
    }

    @DeleteMapping("/{photoId}")
    public ResponseEntity<Void> delete(@PathVariable Long personId, @PathVariable Long photoId, Authentication authentication) {
        ViewerContext viewer = viewerContextResolver.resolve(authentication);
        personPhotoService.delete(personId, photoId, authentication.getName(), viewer.isAdmin());
        return ResponseEntity.noContent().build();
    }

    private Long currentAccountId(Authentication authentication) {
        return userAccountRepository.findByEmail(authentication.getName()).map(UserAccount::getId).orElse(null);
    }

    private PersonPhotoDto toDto(PersonPhoto photo, boolean viewerIsAdmin, Long viewerAccountId) {
        boolean canDelete = viewerIsAdmin || (viewerAccountId != null && photo.getUploadedBy().getId().equals(viewerAccountId));
        return new PersonPhotoDto(photo.getId(), photo.getCaption(), photo.getUploadedAt(), canDelete);
    }
}
