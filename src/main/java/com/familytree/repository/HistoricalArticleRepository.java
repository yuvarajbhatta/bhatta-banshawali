package com.familytree.repository;

import com.familytree.entity.ArticleStatus;
import com.familytree.entity.HistoricalArticle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HistoricalArticleRepository extends JpaRepository<HistoricalArticle, Long> {
    Optional<HistoricalArticle> findBySlugAndStatus(String slug, ArticleStatus status);
    List<HistoricalArticle> findAllByStatusOrderByPublishedAtDesc(ArticleStatus status);
}
