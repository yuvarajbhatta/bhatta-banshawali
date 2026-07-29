package com.familytree.controller;

import com.familytree.dto.AdminArticleDto;
import com.familytree.dto.AdminArticleRequestDto;
import com.familytree.services.ContentAdminService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin content management: create/edit HistoricalArticle rows and move
 * them through draft -> in review -> published (or unpublish/revert).
 * Admin-only via "/api/v1/admin/**". Public, published-only reads stay
 * on PublicContentController/ContentService.
 */
@RestController
@RequestMapping("/api/v1/admin/content")
public class AdminContentApiController {

    private final ContentAdminService contentAdminService;

    public AdminContentApiController(ContentAdminService contentAdminService) {
        this.contentAdminService = contentAdminService;
    }

    @GetMapping
    public List<AdminArticleDto> list() {
        return contentAdminService.listAll();
    }

    @GetMapping("/{id}")
    public AdminArticleDto get(@PathVariable Long id) {
        return contentAdminService.getById(id);
    }

    @PostMapping
    public AdminArticleDto create(@Valid @RequestBody AdminArticleRequestDto request, Authentication authentication) {
        return contentAdminService.create(request, authentication.getName());
    }

    @PutMapping("/{id}")
    public AdminArticleDto update(@PathVariable Long id, @Valid @RequestBody AdminArticleRequestDto request,
                                  Authentication authentication) {
        return contentAdminService.update(id, request, authentication.getName());
    }

    @PostMapping("/{id}/submit-for-review")
    public AdminArticleDto submitForReview(@PathVariable Long id, Authentication authentication) {
        return contentAdminService.submitForReview(id, authentication.getName());
    }

    @PostMapping("/{id}/publish")
    public AdminArticleDto publish(@PathVariable Long id, Authentication authentication) {
        return contentAdminService.publish(id, authentication.getName());
    }

    @PostMapping("/{id}/unpublish")
    public AdminArticleDto unpublish(@PathVariable Long id, Authentication authentication) {
        return contentAdminService.unpublish(id, authentication.getName());
    }

    @PostMapping("/{id}/revert-to-draft")
    public AdminArticleDto revertToDraft(@PathVariable Long id, Authentication authentication) {
        return contentAdminService.revertToDraft(id, authentication.getName());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication authentication) {
        contentAdminService.delete(id, authentication.getName());
    }
}
