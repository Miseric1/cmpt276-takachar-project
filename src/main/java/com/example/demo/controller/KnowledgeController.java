package com.example.demo.controller;

import com.example.demo.dto.PageResponse;
import com.example.demo.dto.knowledge.KnowledgeRequest;
import com.example.demo.dto.knowledge.KnowledgeResponse;
import com.example.demo.dto.knowledge.KnowledgeSummary;
import com.example.demo.dto.knowledge.KnowledgeVersionDto;
import com.example.demo.model.PublicationStatus;
import com.example.demo.service.KnowledgeService;

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
 * REST API for Knowledge Base articles.
 *
 * Public endpoints expose only PUBLISHED articles. Admin-scoped endpoints
 * (create/update/status/delete, "/admin" search, and version history) are
 * restricted to staff by the security configuration.
 */
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    // ----- Public reads ----------------------------------------------------

    @GetMapping
    public PageResponse<KnowledgeSummary> listPublished(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tag,
            @PageableDefault(size = 20, sort = "updatedAt") Pageable pageable) {
        return knowledgeService.searchPublished(keyword, category, tag, pageable);
    }

    @GetMapping("/{id}")
    public KnowledgeResponse getPublished(@PathVariable Long id) {
        return knowledgeService.getPublishedByIdAndCountView(id);
    }

    @PostMapping("/{id}/helpful")
    public ResponseEntity<Void> markHelpful(@PathVariable Long id) {
        knowledgeService.markHelpful(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/not-helpful")
    public ResponseEntity<Void> markNotHelpful(@PathVariable Long id) {
        knowledgeService.markNotHelpful(id);
        return ResponseEntity.noContent().build();
    }

    // ----- Admin reads -----------------------------------------------------

    @GetMapping("/admin")
    public PageResponse<KnowledgeSummary> listAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) PublicationStatus status,
            @PageableDefault(size = 20, sort = "updatedAt") Pageable pageable) {
        return knowledgeService.search(keyword, category, tag, author, status, pageable);
    }

    @GetMapping("/admin/{id}")
    public KnowledgeResponse getForAdmin(@PathVariable Long id) {
        return knowledgeService.getById(id);
    }

    @GetMapping("/{id}/versions")
    public List<KnowledgeVersionDto> getVersions(@PathVariable Long id) {
        return knowledgeService.getVersions(id);
    }

    @GetMapping("/{id}/versions/{versionNumber}")
    public KnowledgeVersionDto getVersion(@PathVariable Long id, @PathVariable int versionNumber) {
        return knowledgeService.getVersion(id, versionNumber);
    }

    // ----- Admin writes ----------------------------------------------------

    @PostMapping
    public ResponseEntity<KnowledgeResponse> create(@Valid @RequestBody KnowledgeRequest request, Authentication auth) {
        KnowledgeResponse created = knowledgeService.create(request, actor(auth));
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public KnowledgeResponse update(@PathVariable Long id, @Valid @RequestBody KnowledgeRequest request,
                                    Authentication auth) {
        return knowledgeService.update(id, request, actor(auth));
    }

    @PatchMapping("/{id}/status")
    public KnowledgeResponse changeStatus(@PathVariable Long id, @RequestParam PublicationStatus status,
                                          Authentication auth) {
        return knowledgeService.changeStatus(id, status, actor(auth));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        knowledgeService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private String actor(Authentication auth) {
        return auth != null ? auth.getName() : "system";
    }
}
