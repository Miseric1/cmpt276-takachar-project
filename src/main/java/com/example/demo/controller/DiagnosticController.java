package com.example.demo.controller;

import com.example.demo.dto.PageResponse;
import com.example.demo.dto.diagnostic.DiagnosticAnswerRequest;
import com.example.demo.dto.diagnostic.DiagnosticResolutionRequest;
import com.example.demo.dto.diagnostic.DiagnosticSessionResponse;
import com.example.demo.dto.knowledge.KnowledgeSummary;
import com.example.demo.service.DiagnosticService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/diagnostics")
public class DiagnosticController {
    private final DiagnosticService diagnosticService;

    public DiagnosticController(DiagnosticService diagnosticService) {
        this.diagnosticService = diagnosticService;
    }

    @PostMapping("/sessions")
    public ResponseEntity<DiagnosticSessionResponse> start(
            @RequestParam(required = false) String category, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(diagnosticService.start(category, auth.getName()));
    }

    @GetMapping("/sessions/{id}")
    public DiagnosticSessionResponse get(@PathVariable UUID id, Authentication auth) {
        return diagnosticService.get(id, auth.getName(), isAdmin(auth));
    }

    @PostMapping("/sessions/{id}/answers")
    public DiagnosticSessionResponse answer(@PathVariable UUID id,
                                            @Valid @RequestBody DiagnosticAnswerRequest request,
                                            Authentication auth) {
        return diagnosticService.answer(id, request, auth.getName(), isAdmin(auth));
    }

    @PostMapping("/sessions/{id}/resolution")
    public DiagnosticSessionResponse recordResolution(@PathVariable UUID id,
                                                       @RequestBody DiagnosticResolutionRequest request,
                                                       Authentication auth) {
        return diagnosticService.recordResolution(id, request.resolved(), auth.getName(), isAdmin(auth));
    }

    @GetMapping("/suggestions")
    public PageResponse<KnowledgeSummary> suggestions(@RequestParam String query) {
        return diagnosticService.suggestArticles(query);
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }
}
