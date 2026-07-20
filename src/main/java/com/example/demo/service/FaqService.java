package com.example.demo.service;

import com.example.demo.dto.PageResponse;
import com.example.demo.dto.faq.FaqRequest;
import com.example.demo.dto.faq.FaqResponse;
import com.example.demo.dto.faq.FaqSummary;
import com.example.demo.dto.faq.FaqVersionDto;
import com.example.demo.exception.DuplicateResourceException;
import com.example.demo.exception.InvalidStateException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.mapper.FaqMapper;
import com.example.demo.model.Category;
import com.example.demo.model.Faq;
import com.example.demo.model.FaqVersion;
import com.example.demo.model.PublicationStatus;
import com.example.demo.repository.FaqRepository;
import com.example.demo.repository.FaqSpecifications;
import com.example.demo.repository.FaqVersionRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business logic for the FAQ module: authoring, the publication workflow,
 * version history, search, and engagement tracking. Controllers stay thin and
 * delegate every rule to this service.
 */
@Service
public class FaqService {

    private static final Logger log = LoggerFactory.getLogger(FaqService.class);

    private final FaqRepository faqRepository;
    private final FaqVersionRepository faqVersionRepository;
    private final CategoryService categoryService;
    private final TagService tagService;

    public FaqService(FaqRepository faqRepository,
                      FaqVersionRepository faqVersionRepository,
                      CategoryService categoryService,
                      TagService tagService) {
        this.faqRepository = faqRepository;
        this.faqVersionRepository = faqVersionRepository;
        this.categoryService = categoryService;
        this.tagService = tagService;
    }

    // ----- Reads (admin: any status) ---------------------------------------

    @Transactional(readOnly = true)
    public FaqResponse getById(Long id) {
        return FaqMapper.toResponse(findEntity(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<FaqSummary> search(String keyword, String category, String tag,
                                           PublicationStatus status, Pageable pageable) {
        Specification<Faq> spec = Specification.allOf(
                FaqSpecifications.keyword(keyword),
                FaqSpecifications.categoryNameIs(category),
                FaqSpecifications.hasTag(tag),
                FaqSpecifications.statusIs(status));
        Page<Faq> page = faqRepository.findAll(spec, pageable);
        return PageResponse.of(page, FaqMapper::toSummary);
    }

    // ----- Reads (public: published only) ----------------------------------

    /** Public detail view. Increments the view counter as a side effect. */
    @Transactional
    public FaqResponse getPublishedByIdAndCountView(Long id) {
        Faq faq = findEntity(id);
        if (!faq.getStatus().isPubliclyVisible()) {
            throw new ResourceNotFoundException("FAQ", id);
        }
        faqRepository.incrementViewCount(id);
        return FaqMapper.toResponse(findEntity(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<FaqSummary> searchPublished(String keyword, String category, String tag, Pageable pageable) {
        return search(keyword, category, tag, PublicationStatus.PUBLISHED, pageable);
    }

    // ----- Writes ----------------------------------------------------------

    @Transactional
    public FaqResponse create(FaqRequest request, String actor) {
        String question = request.question().trim();
        if (faqRepository.existsByQuestionIgnoreCase(question)) {
            throw new DuplicateResourceException("An FAQ with this question already exists.");
        }
        Faq faq = new Faq();
        faq.setQuestion(question);
        faq.setAnswer(request.answer());
        faq.setCategory(resolveCategory(request.category()));
        faq.setTags(tagService.resolveTags(request.tags()));
        faq.setDisplayOrder(request.displayOrder() == null ? 0 : request.displayOrder());
        faq.setStatus(request.status() == null ? PublicationStatus.DRAFT : request.status());
        faq.setCreatedBy(actor);
        faq.setLastModifiedBy(actor);
        if (faq.getStatus() == PublicationStatus.PUBLISHED) {
            faq.setPublishedAt(java.time.LocalDateTime.now());
        }
        Faq saved = faqRepository.save(faq);
        log.info("FAQ {} created by {}", saved.getId(), actor);
        return FaqMapper.toResponse(saved);
    }

    @Transactional
    public FaqResponse update(Long id, FaqRequest request, String actor) {
        Faq faq = findEntity(id);

        // Reject a duplicate question that belongs to a different FAQ.
        String question = request.question().trim();
        faqRepository.findByQuestionIgnoreCase(question)
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new DuplicateResourceException("An FAQ with this question already exists.");
                });

        // Snapshot the state being replaced, then advance the version.
        faqVersionRepository.save(FaqVersion.snapshotOf(faq));
        faq.setVersion(faq.getVersion() + 1);

        faq.setQuestion(question);
        faq.setAnswer(request.answer());
        faq.setCategory(resolveCategory(request.category()));
        faq.setTags(tagService.resolveTags(request.tags()));
        if (request.displayOrder() != null) {
            faq.setDisplayOrder(request.displayOrder());
        }
        if (request.status() != null) {
            applyStatus(faq, request.status());
        }
        faq.setLastModifiedBy(actor);
        Faq saved = faqRepository.save(faq);
        log.info("FAQ {} updated to version {} by {}", saved.getId(), saved.getVersion(), actor);
        return FaqMapper.toResponse(saved);
    }

    @Transactional
    public FaqResponse changeStatus(Long id, PublicationStatus target, String actor) {
        Faq faq = findEntity(id);
        applyStatus(faq, target);
        faq.setLastModifiedBy(actor);
        log.info("FAQ {} status changed to {} by {}", id, target, actor);
        return FaqMapper.toResponse(faqRepository.save(faq));
    }

    @Transactional
    public void delete(Long id) {
        Faq faq = findEntity(id);
        faqRepository.delete(faq);
        log.info("FAQ {} deleted", id);
    }

    // ----- Engagement ------------------------------------------------------

    @Transactional
    public void markHelpful(Long id) {
        requireExists(id);
        faqRepository.incrementHelpfulCount(id);
    }

    @Transactional
    public void markNotHelpful(Long id) {
        requireExists(id);
        faqRepository.incrementNotHelpfulCount(id);
    }

    // ----- Version history -------------------------------------------------

    @Transactional(readOnly = true)
    public List<FaqVersionDto> getVersions(Long id) {
        requireExists(id);
        return faqVersionRepository.findByFaqIdOrderByVersionNumberDesc(id)
                .stream().map(FaqVersionDto::from).toList();
    }

    @Transactional(readOnly = true)
    public FaqVersionDto getVersion(Long id, int versionNumber) {
        return faqVersionRepository.findByFaqIdAndVersionNumber(id, versionNumber)
                .map(FaqVersionDto::from)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Version " + versionNumber + " of FAQ " + id + " does not exist."));
    }

    // ----- Helpers ---------------------------------------------------------

    private Faq findEntity(Long id) {
        return faqRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FAQ", id));
    }

    private void requireExists(Long id) {
        if (!faqRepository.existsById(id)) {
            throw new ResourceNotFoundException("FAQ", id);
        }
    }

    private Category resolveCategory(String name) {
        return categoryService.getOrCreateByName(name);
    }

    /** Validate and apply a publication transition, stamping publishedAt once. */
    private void applyStatus(Faq faq, PublicationStatus target) {
        PublicationStatus current = faq.getStatus();
        if (!current.canTransitionTo(target)) {
            throw new InvalidStateException(
                    "Cannot change FAQ status from " + current + " to " + target + ".");
        }
        faq.setStatus(target);
        if (target == PublicationStatus.PUBLISHED && faq.getPublishedAt() == null) {
            faq.setPublishedAt(java.time.LocalDateTime.now());
        }
    }
}
