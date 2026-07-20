package com.example.demo.dto.knowledge;

import com.example.demo.model.KnowledgeArticle;

/**
 * Minimal pointer to another article, used for related-article links so a
 * response never has to embed (or recurse into) a full article.
 */
public record ArticleReference(Long id, String title) {

    public static ArticleReference from(KnowledgeArticle article) {
        return new ArticleReference(article.getId(), article.getTitle());
    }
}
