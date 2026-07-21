package com.example.demo.service;

import com.example.demo.dto.PageResponse;
import com.example.demo.dto.knowledge.KnowledgeRequest;
import com.example.demo.dto.knowledge.KnowledgeResponse;
import com.example.demo.dto.knowledge.KnowledgeSummary;
import com.example.demo.dto.knowledge.KnowledgeVersionDto;
import com.example.demo.exception.DuplicateResourceException;
import com.example.demo.exception.InvalidStateException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.mapper.KnowledgeMapper;
import com.example.demo.model.KnowledgeArticle;
import com.example.demo.model.KnowledgeArticleVersion;
import com.example.demo.model.PublicationStatus;
import com.example.demo.repository.KnowledgeArticleRepository;
import com.example.demo.repository.KnowledgeArticleVersionRepository;
import com.example.demo.repository.KnowledgeSpecifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Business logic for the Knowledge Base: authoring, publication workflow,
 * version history, related-article links, reading-time estimation, search, and
 * engagement tracking.
 */
@Service
public class KnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeService.class);
    private static final int WORDS_PER_MINUTE = 200;

    private final KnowledgeArticleRepository articleRepository;
    private final KnowledgeArticleVersionRepository versionRepository;
    private final CategoryService categoryService;
    private final TagService tagService;

    public KnowledgeService(KnowledgeArticleRepository articleRepository,
                            KnowledgeArticleVersionRepository versionRepository,
                            CategoryService categoryService,
                            TagService tagService) {
        this.articleRepository = articleRepository;
        this.versionRepository = versionRepository;
        this.categoryService = categoryService;
        this.tagService = tagService;
    }

    // ----- Reads (admin) ---------------------------------------------------

    @Transactional(readOnly = true)
    public KnowledgeResponse getById(Long id) {
        return KnowledgeMapper.toResponse(findEntity(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<KnowledgeSummary> search(String keyword, String category, String tag,
                                                 String author, PublicationStatus status, Pageable pageable) {
        Specification<KnowledgeArticle> spec = Specification.allOf(
                KnowledgeSpecifications.keyword(keyword),
                KnowledgeSpecifications.categoryNameIs(category),
                KnowledgeSpecifications.hasTag(tag),
                KnowledgeSpecifications.authorIs(author),
                KnowledgeSpecifications.statusIs(status));
        Page<KnowledgeArticle> page = articleRepository.findAll(spec, pageable);
        return PageResponse.of(page, KnowledgeMapper::toSummary);
    }

    // ----- Reads (public) --------------------------------------------------

    @Transactional
    public KnowledgeResponse getPublishedByIdAndCountView(Long id) {
        KnowledgeArticle article = findEntity(id);
        if (!article.getStatus().isPubliclyVisible()) {
            throw new ResourceNotFoundException("Article", id);
        }
        articleRepository.incrementViewCount(id);
        return KnowledgeMapper.toResponse(findEntity(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<KnowledgeSummary> searchPublished(String keyword, String category, String tag,
                                                          Pageable pageable) {
        return search(keyword, category, tag, null, PublicationStatus.PUBLISHED, pageable);
    }

    // ----- Writes ----------------------------------------------------------

    @Transactional
    public KnowledgeResponse create(KnowledgeRequest request, String actor) {
        String title = request.title().trim();
        if (articleRepository.existsByTitleIgnoreCase(title)) {
            throw new DuplicateResourceException("An article with this title already exists.");
        }
        KnowledgeArticle article = new KnowledgeArticle();
        article.setTitle(title);
        article.setSummary(request.summary());
        article.setBody(request.body());
        article.setEstimatedReadingTimeMinutes(estimateReadingTime(request.body()));
        article.setCategory(categoryService.getOrCreateByName(request.category()));
        article.setTags(tagService.resolveTags(request.tags()));
        article.setStatus(request.status() == null ? PublicationStatus.DRAFT : request.status());

        String author = (request.author() == null || request.author().isBlank()) ? actor : request.author().trim();
        article.setAuthor(author);
        article.setLastModifiedBy(actor);
        Set<String> contributors = new LinkedHashSet<>();
        contributors.add(author);
        if (actor != null) {
            contributors.add(actor);
        }
        article.setContributors(contributors);

        if (article.getStatus() == PublicationStatus.PUBLISHED) {
            article.setPublishedAt(LocalDateTime.now());
        }

        // Persist first so related-article links can reference a managed entity.
        KnowledgeArticle saved = articleRepository.save(article);
        saved.setRelatedArticles(resolveRelated(request.relatedArticleIds(), saved.getId()));
        saved = articleRepository.save(saved);
        log.info("Article {} created by {}", saved.getId(), actor);
        return KnowledgeMapper.toResponse(saved);
    }

    @Transactional
    public KnowledgeResponse update(Long id, KnowledgeRequest request, String actor) {
        KnowledgeArticle article = findEntity(id);

        String title = request.title().trim();
        articleRepository.findByTitleIgnoreCase(title)
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new DuplicateResourceException("An article with this title already exists.");
                });

        // Snapshot the state being replaced, then advance the version.
        versionRepository.save(KnowledgeArticleVersion.snapshotOf(article));
        article.setVersion(article.getVersion() + 1);

        article.setTitle(title);
        article.setSummary(request.summary());
        article.setBody(request.body());
        article.setEstimatedReadingTimeMinutes(estimateReadingTime(request.body()));
        article.setCategory(categoryService.getOrCreateByName(request.category()));
        article.setTags(tagService.resolveTags(request.tags()));
        article.setRelatedArticles(resolveRelated(request.relatedArticleIds(), id));
        if (request.author() != null && !request.author().isBlank()) {
            article.setAuthor(request.author().trim());
        }
        if (request.status() != null) {
            applyStatus(article, request.status());
        }
        article.setLastModifiedBy(actor);
        if (actor != null) {
            article.getContributors().add(actor);
        }
        KnowledgeArticle saved = articleRepository.save(article);
        log.info("Article {} updated to version {} by {}", saved.getId(), saved.getVersion(), actor);
        return KnowledgeMapper.toResponse(saved);
    }

    @Transactional
    public KnowledgeResponse changeStatus(Long id, PublicationStatus target, String actor) {
        KnowledgeArticle article = findEntity(id);
        applyStatus(article, target);
        article.setLastModifiedBy(actor);
        log.info("Article {} status changed to {} by {}", id, target, actor);
        return KnowledgeMapper.toResponse(articleRepository.save(article));
    }

    @Transactional
    public void delete(Long id) {
        KnowledgeArticle article = findEntity(id);
        // Detach this article's own outgoing related-article links.
        article.getRelatedArticles().clear();
        articleRepository.save(article);

        // The relation is unidirectional, so other articles that list this one as
        // related are unaware of the deletion -- detach their incoming links too,
        // or they would be left pointing at a now-nonexistent id.
        for (KnowledgeArticle referencing : articleRepository.findByRelatedArticlesId(id)) {
            referencing.getRelatedArticles().removeIf(related -> related.getId().equals(id));
            articleRepository.save(referencing);
        }

        articleRepository.delete(article);
        log.info("Article {} deleted", id);
    }

    // ----- Engagement ------------------------------------------------------

    @Transactional
    public void markHelpful(Long id) {
        requireExists(id);
        articleRepository.incrementHelpfulCount(id);
    }

    @Transactional
    public void markNotHelpful(Long id) {
        requireExists(id);
        articleRepository.incrementNotHelpfulCount(id);
    }

    // ----- Version history -------------------------------------------------

    @Transactional(readOnly = true)
    public List<KnowledgeVersionDto> getVersions(Long id) {
        requireExists(id);
        return versionRepository.findByArticleIdOrderByVersionNumberDesc(id)
                .stream().map(KnowledgeVersionDto::from).toList();
    }

    @Transactional(readOnly = true)
    public KnowledgeVersionDto getVersion(Long id, int versionNumber) {
        return versionRepository.findByArticleIdAndVersionNumber(id, versionNumber)
                .map(KnowledgeVersionDto::from)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Version " + versionNumber + " of article " + id + " does not exist."));
    }

    // ----- Helpers ---------------------------------------------------------

    private KnowledgeArticle findEntity(Long id) {
        return articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Article", id));
    }

    private void requireExists(Long id) {
        if (!articleRepository.existsById(id)) {
            throw new ResourceNotFoundException("Article", id);
        }
    }

    /** Resolve related-article ids to managed entities, excluding self and any
     * ids that do not exist. */
    private Set<KnowledgeArticle> resolveRelated(Set<Long> ids, Long selfId) {
        Set<KnowledgeArticle> related = new LinkedHashSet<>();
        if (ids == null || ids.isEmpty()) {
            return related;
        }
        for (Long relatedId : ids) {
            if (relatedId == null || relatedId.equals(selfId)) {
                continue;
            }
            articleRepository.findById(relatedId).ifPresent(related::add);
        }
        return related;
    }

    private int estimateReadingTime(String body) {
        if (body == null || body.isBlank()) {
            return 1;
        }
        int words = body.trim().split("\\s+").length;
        return Math.max(1, (int) Math.ceil((double) words / WORDS_PER_MINUTE));
    }

    private void applyStatus(KnowledgeArticle article, PublicationStatus target) {
        PublicationStatus current = article.getStatus();
        if (!current.canTransitionTo(target)) {
            throw new InvalidStateException(
                    "Cannot change article status from " + current + " to " + target + ".");
        }
        article.setStatus(target);
        if (target == PublicationStatus.PUBLISHED && article.getPublishedAt() == null) {
            article.setPublishedAt(LocalDateTime.now());
        }
    }
}
