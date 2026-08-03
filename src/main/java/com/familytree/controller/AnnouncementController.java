package com.familytree.controller;

import com.familytree.dto.AnnouncementDto;
import com.familytree.dto.AnnouncementUnreadCountDto;
import com.familytree.services.AnnouncementService;
import com.familytree.services.ViewerContext;
import com.familytree.services.ViewerContextResolver;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

/**
 * Member-facing read side of News & Alerts -- authenticated (any
 * member, no admin requirement) via SecurityConfig's default
 * "anyRequest().authenticated()" rule. Admin authoring lives on
 * AdminAnnouncementController under "/api/v1/admin/announcements".
 */
@RestController
@RequestMapping("/api/v1/announcements")
public class AnnouncementController {

    private final AnnouncementService announcementService;
    private final ViewerContextResolver viewerContextResolver;

    public AnnouncementController(AnnouncementService announcementService, ViewerContextResolver viewerContextResolver) {
        this.announcementService = announcementService;
        this.viewerContextResolver = viewerContextResolver;
    }

    @GetMapping
    public List<AnnouncementDto> list() {
        return announcementService.listPublished();
    }

    @GetMapping("/unread-count")
    public AnnouncementUnreadCountDto unreadCount(Authentication authentication) {
        return new AnnouncementUnreadCountDto(announcementService.unreadCount(authentication.getName()));
    }

    @PostMapping("/mark-seen")
    public void markSeen(Authentication authentication) {
        announcementService.markSeen(authentication.getName());
    }

    @GetMapping("/{postId}/photos/{photoId}/file")
    public ResponseEntity<byte[]> photoFile(@PathVariable Long postId, @PathVariable Long photoId, Authentication authentication) {
        ViewerContext viewer = viewerContextResolver.resolve(authentication);
        AnnouncementService.StoredPhotoFile storedFile = announcementService.readPhotoFile(postId, photoId, viewer.isAdmin());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(storedFile.mimeType()))
                // "private": same reasoning as PersonPhotoController -- this
                // app sits behind a shared Cloudflare Tunnel edge, and a
                // shared/CDN cache honoring "public" could serve a photo
                // back to a caller who was never authenticated at all.
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePrivate())
                .body(storedFile.bytes());
    }
}
