package com.familytree.controller;

import com.familytree.dto.ArticleDto;
import com.familytree.dto.ArticleSummaryDto;
import com.familytree.services.ContentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public, unauthenticated read access to published admin-managed content
 * (About, History, Membership explainer, etc.). Never exposes draft,
 * in-review, or unpublished articles -- see ContentService.
 */
@RestController
@RequestMapping("/api/v1/content")
public class PublicContentController {

    private final ContentService contentService;

    public PublicContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    @GetMapping
    public List<ArticleSummaryDto> listPublished() {
        return contentService.listPublishedArticles();
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ArticleDto> getPublished(@PathVariable String slug) {
        return contentService.getPublishedArticle(slug)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
