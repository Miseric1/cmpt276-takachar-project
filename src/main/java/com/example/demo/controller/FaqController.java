package com.example.demo.controller;

import com.example.demo.dto.PageResponse;
import com.example.demo.dto.faq.FaqRequest;
import com.example.demo.dto.faq.FaqResponse;
import com.example.demo.dto.faq.FaqSummary;
import com.example.demo.dto.faq.FaqVersionDto;
import com.example.demo.model.PublicationStatus;
import com.example.demo.service.FaqService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST API for FAQs.
 *
 * Public (customer-facing) endpoints expose only PUBLISHED content. The
 * admin-scoped endpoints (create/update/status/delete, "/admin" search, and
 * version history) are restricted to staff by the security configuration.
 */
@RestController
@RequestMapping("/api/faqs")
public class FaqController {

    private final FaqService faqService;

    public FaqController(FaqService faqService) {
        this.faqService = faqService;
    }

    // ----- Public reads ----------------------------------------------------

    /** Paginated list/search over published FAQs. */
    @GetMapping
    public PageResponse<FaqSummary> listPublished(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tag,
            @PageableDefault(size = 20, sort = "displayOrder") Pageable pageable) {
        return faqService.searchPublished(keyword, category, tag, pageable);
    }

    /** Published FAQ detail. Counts as a view. */
    @GetMapping("/{id}")
    public FaqResponse getPublished(@PathVariable Long id) {
        return faqService.getPublishedByIdAndCountView(id);
    }

    @PostMapping("/{id}/helpful")
    public ResponseEntity<Void> markHelpful(@PathVariable Long id) {
        faqService.markHelpful(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/not-helpful")
    public ResponseEntity<Void> markNotHelpful(@PathVariable Long id) {
        faqService.markNotHelpful(id);
        return ResponseEntity.noContent().build();
    }

    // ----- Admin reads -----------------------------------------------------

    /** Admin list/search across every status. */
    @GetMapping("/admin")
    public PageResponse<FaqSummary> listAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) PublicationStatus status,
            @PageableDefault(size = 20, sort = "updatedAt") Pageable pageable) {
        return faqService.search(keyword, category, tag, status, pageable);
    }

    /** Admin detail (any status), does not count as a public view. */
    @GetMapping("/admin/{id}")
    public FaqResponse getForAdmin(@PathVariable Long id) {
        return faqService.getById(id);
    }

    @GetMapping("/{id}/versions")
    public List<FaqVersionDto> getVersions(@PathVariable Long id) {
        return faqService.getVersions(id);
    }

    @GetMapping("/{id}/versions/{versionNumber}")
    public FaqVersionDto getVersion(@PathVariable Long id, @PathVariable int versionNumber) {
        return faqService.getVersion(id, versionNumber);
    }

    // ----- Admin writes ----------------------------------------------------

    @PostMapping
    public ResponseEntity<FaqResponse> create(@Valid @RequestBody FaqRequest request, Authentication auth) {
        FaqResponse created = faqService.create(request, actor(auth));
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public FaqResponse update(@PathVariable Long id, @Valid @RequestBody FaqRequest request, Authentication auth) {
        return faqService.update(id, request, actor(auth));
    }

    @PatchMapping("/{id}/status")
    public FaqResponse changeStatus(@PathVariable Long id, @RequestParam PublicationStatus status, Authentication auth) {
        return faqService.changeStatus(id, status, actor(auth));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        faqService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private String actor(Authentication auth) {
        return auth != null ? auth.getName() : "system";
    }
}
