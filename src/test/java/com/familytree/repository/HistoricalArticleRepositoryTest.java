package com.familytree.repository;

import com.familytree.entity.ArticleStatus;
import com.familytree.entity.HistoricalArticle;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:historical-article-repo;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ImportAutoConfiguration(exclude = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
})
@Transactional
class HistoricalArticleRepositoryTest {

    @Autowired
    private HistoricalArticleRepository historicalArticleRepository;

    @Test
    void findBySlugAndStatusReturnsOnlyMatchingStatus() {
        historicalArticleRepository.save(article("about-banshawali", ArticleStatus.PUBLISHED));
        historicalArticleRepository.save(article("draft-only", ArticleStatus.DRAFT));

        assertThat(historicalArticleRepository.findBySlugAndStatus("about-banshawali", ArticleStatus.PUBLISHED))
                .isPresent();
        assertThat(historicalArticleRepository.findBySlugAndStatus("draft-only", ArticleStatus.PUBLISHED))
                .isEmpty();
    }

    @Test
    void findAllByStatusOrderByPublishedAtDescExcludesNonPublished() {
        HistoricalArticle older = article("older", ArticleStatus.PUBLISHED);
        older.setPublishedAt(LocalDateTime.now().minusDays(1));
        HistoricalArticle newer = article("newer", ArticleStatus.PUBLISHED);
        newer.setPublishedAt(LocalDateTime.now());
        historicalArticleRepository.save(older);
        historicalArticleRepository.save(newer);
        historicalArticleRepository.save(article("unpublished", ArticleStatus.UNPUBLISHED));

        List<HistoricalArticle> published = historicalArticleRepository
                .findAllByStatusOrderByPublishedAtDesc(ArticleStatus.PUBLISHED);

        assertThat(published).extracting(HistoricalArticle::getSlug).containsExactly("newer", "older");
    }

    private HistoricalArticle article(String slug, ArticleStatus status) {
        HistoricalArticle article = new HistoricalArticle();
        article.setSlug(slug);
        article.setTitleEn("Title for " + slug);
        article.setBodyEn("Body for " + slug);
        article.setStatus(status);
        return article;
    }
}
