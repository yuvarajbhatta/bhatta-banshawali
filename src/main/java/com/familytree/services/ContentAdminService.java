package com.familytree.services;

import com.familytree.dto.AdminArticleDto;
import com.familytree.dto.AdminArticleRequestDto;
import com.familytree.entity.ArticleStatus;
import com.familytree.entity.HistoricalArticle;
import com.familytree.repository.HistoricalArticleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Admin CRUD + draft/review/publish workflow for HistoricalArticle --
 * the content backing the public About/History/Membership pages (see
 * docs/08-implementation-roadmap.md Phase 2/6 "content management").
 * ContentService remains the public, published-only read path; this is
 * the admin-only write side.
 */
@Service
public class ContentAdminService {

    private final HistoricalArticleRepository historicalArticleRepository;
    private final AuditLogService auditLogService;

    public ContentAdminService(HistoricalArticleRepository historicalArticleRepository, AuditLogService auditLogService) {
        this.historicalArticleRepository = historicalArticleRepository;
        this.auditLogService = auditLogService;
    }

    public List<AdminArticleDto> listAll() {
        return historicalArticleRepository.findAll().stream()
                .sorted(Comparator.comparing(HistoricalArticle::getUpdatedAt).reversed())
                .map(ContentAdminService::toDto)
                .toList();
    }

    public AdminArticleDto getById(Long id) {
        return toDto(getOrThrow(id));
    }

    /** @throws IllegalArgumentException if the slug is already used by another article */
    @Transactional
    public AdminArticleDto create(AdminArticleRequestDto request, String actorUsername) {
        if (historicalArticleRepository.existsBySlug(request.getSlug())) {
            throw new IllegalArgumentException("An article with slug \"" + request.getSlug() + "\" already exists.");
        }

        HistoricalArticle article = new HistoricalArticle();
        article.setSlug(request.getSlug());
        applyContent(article, request);
        article.setStatus(ArticleStatus.DRAFT);
        article.setCreatedAt(LocalDateTime.now());
        article.setUpdatedAt(LocalDateTime.now());
        historicalArticleRepository.save(article);

        auditLogService.record(AuditLogService.ACTION_CONTENT_CREATED, AuditLogService.ENTITY_CONTENT, article.getId(),
                "Created content \"" + article.getSlug() + "\"", actorUsername);

        return toDto(article);
    }

    /** @throws IllegalArgumentException if the new slug is already used by a different article */
    @Transactional
    public AdminArticleDto update(Long id, AdminArticleRequestDto request, String actorUsername) {
        HistoricalArticle article = getOrThrow(id);
        if (historicalArticleRepository.existsBySlugAndIdNot(request.getSlug(), id)) {
            throw new IllegalArgumentException("An article with slug \"" + request.getSlug() + "\" already exists.");
        }

        article.setSlug(request.getSlug());
        applyContent(article, request);
        article.setUpdatedAt(LocalDateTime.now());
        historicalArticleRepository.save(article);

        auditLogService.record(AuditLogService.ACTION_CONTENT_UPDATED, AuditLogService.ENTITY_CONTENT, id,
                "Edited content \"" + article.getSlug() + "\"", actorUsername);

        return toDto(article);
    }

    /** @throws IllegalArgumentException if the article isn't DRAFT */
    @Transactional
    public AdminArticleDto submitForReview(Long id, String actorUsername) {
        return transitionStatus(id, ArticleStatus.DRAFT, ArticleStatus.IN_REVIEW, actorUsername);
    }

    /** @throws IllegalArgumentException if the article is already PUBLISHED */
    @Transactional
    public AdminArticleDto publish(Long id, String actorUsername) {
        HistoricalArticle article = getOrThrow(id);
        if (article.getStatus() == ArticleStatus.PUBLISHED) {
            throw new IllegalArgumentException("This article is already published.");
        }
        ArticleStatus from = article.getStatus();
        article.setStatus(ArticleStatus.PUBLISHED);
        article.setPublishedAt(LocalDateTime.now());
        article.setUpdatedAt(LocalDateTime.now());
        historicalArticleRepository.save(article);

        auditLogService.record(AuditLogService.ACTION_CONTENT_STATUS_CHANGED, AuditLogService.ENTITY_CONTENT, id,
                "Published content \"" + article.getSlug() + "\" (was " + from + ")", actorUsername);

        return toDto(article);
    }

    /** @throws IllegalArgumentException if the article isn't PUBLISHED */
    @Transactional
    public AdminArticleDto unpublish(Long id, String actorUsername) {
        return transitionStatus(id, ArticleStatus.PUBLISHED, ArticleStatus.UNPUBLISHED, actorUsername);
    }

    /** @throws IllegalArgumentException if the article is already a DRAFT */
    @Transactional
    public AdminArticleDto revertToDraft(Long id, String actorUsername) {
        HistoricalArticle article = getOrThrow(id);
        if (article.getStatus() == ArticleStatus.DRAFT) {
            throw new IllegalArgumentException("This article is already a draft.");
        }
        ArticleStatus from = article.getStatus();
        article.setStatus(ArticleStatus.DRAFT);
        article.setUpdatedAt(LocalDateTime.now());
        historicalArticleRepository.save(article);

        auditLogService.record(AuditLogService.ACTION_CONTENT_STATUS_CHANGED, AuditLogService.ENTITY_CONTENT, id,
                "Reverted content \"" + article.getSlug() + "\" to draft (was " + from + ")", actorUsername);

        return toDto(article);
    }

    @Transactional
    public void delete(Long id, String actorUsername) {
        HistoricalArticle article = getOrThrow(id);
        String slug = article.getSlug();
        historicalArticleRepository.delete(article);

        auditLogService.record(AuditLogService.ACTION_CONTENT_DELETED, AuditLogService.ENTITY_CONTENT, id,
                "Deleted content \"" + slug + "\"", actorUsername);
    }

    private AdminArticleDto transitionStatus(Long id, ArticleStatus from, ArticleStatus to, String actorUsername) {
        HistoricalArticle article = getOrThrow(id);
        if (article.getStatus() != from) {
            throw new IllegalArgumentException("This article must be " + from + " to do that (it's currently " + article.getStatus() + ").");
        }
        article.setStatus(to);
        article.setUpdatedAt(LocalDateTime.now());
        historicalArticleRepository.save(article);

        auditLogService.record(AuditLogService.ACTION_CONTENT_STATUS_CHANGED, AuditLogService.ENTITY_CONTENT, id,
                "Moved content \"" + article.getSlug() + "\" from " + from + " to " + to, actorUsername);

        return toDto(article);
    }

    private void applyContent(HistoricalArticle article, AdminArticleRequestDto request) {
        article.setTitleEn(request.getTitleEn());
        article.setTitleNe(blankToNull(request.getTitleNe()));
        article.setBodyEn(request.getBodyEn());
        article.setBodyNe(blankToNull(request.getBodyNe()));
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private static AdminArticleDto toDto(HistoricalArticle article) {
        return new AdminArticleDto(
                article.getId(),
                article.getSlug(),
                article.getTitleEn(),
                article.getTitleNe(),
                article.getBodyEn(),
                article.getBodyNe(),
                article.getStatus(),
                article.getPublishedAt(),
                article.getCreatedAt(),
                article.getUpdatedAt()
        );
    }

    private HistoricalArticle getOrThrow(Long id) {
        return historicalArticleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found with id: " + id));
    }
}
