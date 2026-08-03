package com.familytree.controller;

import com.familytree.dto.AdminAnnouncementDto;
import com.familytree.dto.AdminAnnouncementRequestDto;
import com.familytree.dto.AnnouncementPhotoDto;
import com.familytree.services.AnnouncementAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Admin authoring for News & Alerts: create/edit AnnouncementPost rows,
 * move them draft <-> published, and attach/remove photos. Admin-only
 * via "/api/v1/admin/**" (see SecurityConfig). Member-facing published
 * reads and photo file serving stay on AnnouncementController.
 */
@RestController
@RequestMapping("/api/v1/admin/announcements")
public class AdminAnnouncementController {

    private final AnnouncementAdminService announcementAdminService;

    public AdminAnnouncementController(AnnouncementAdminService announcementAdminService) {
        this.announcementAdminService = announcementAdminService;
    }

    @GetMapping
    public List<AdminAnnouncementDto> list() {
        return announcementAdminService.listAll();
    }

    @GetMapping("/{id}")
    public AdminAnnouncementDto get(@PathVariable Long id) {
        return announcementAdminService.getById(id);
    }

    @PostMapping
    public AdminAnnouncementDto create(@Valid @RequestBody AdminAnnouncementRequestDto request, Authentication authentication) {
        return announcementAdminService.create(request, authentication.getName());
    }

    @PutMapping("/{id}")
    public AdminAnnouncementDto update(@PathVariable Long id, @Valid @RequestBody AdminAnnouncementRequestDto request,
                                       Authentication authentication) {
        return announcementAdminService.update(id, request, authentication.getName());
    }

    @PostMapping("/{id}/publish")
    public AdminAnnouncementDto publish(@PathVariable Long id, Authentication authentication) {
        return announcementAdminService.publish(id, authentication.getName());
    }

    @PostMapping("/{id}/unpublish")
    public AdminAnnouncementDto unpublish(@PathVariable Long id, Authentication authentication) {
        return announcementAdminService.unpublish(id, authentication.getName());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication authentication) {
        announcementAdminService.delete(id, authentication.getName());
    }

    @PostMapping("/{id}/photos")
    public AnnouncementPhotoDto uploadPhoto(@PathVariable Long id,
                                            @RequestParam("file") MultipartFile file,
                                            @RequestParam(value = "caption", required = false) String caption,
                                            Authentication authentication) {
        return announcementAdminService.uploadPhoto(id, file, caption, authentication.getName());
    }

    @DeleteMapping("/{id}/photos/{photoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePhoto(@PathVariable Long id, @PathVariable Long photoId) {
        announcementAdminService.deletePhoto(id, photoId);
    }
}
