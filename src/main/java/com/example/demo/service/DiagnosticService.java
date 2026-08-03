package com.example.demo.service;

import com.example.demo.dto.PageResponse;
import com.example.demo.dto.diagnostic.DiagnosticAnswerRequest;
import com.example.demo.dto.diagnostic.DiagnosticSessionResponse;
import com.example.demo.dto.knowledge.KnowledgeSummary;
import com.example.demo.exception.InvalidStateException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.mapper.DiagnosticMapper;
import com.example.demo.model.DiagnosticAnswer;
import com.example.demo.model.DiagnosticNode;
import com.example.demo.model.DiagnosticOption;
import com.example.demo.model.DiagnosticSession;
import com.example.demo.model.DiagnosticSessionStatus;
import com.example.demo.model.KnowledgeArticle;
import com.example.demo.model.PublicationStatus;
import com.example.demo.repository.DiagnosticNodeRepository;
import com.example.demo.repository.DiagnosticSessionRepository;
import com.example.demo.repository.KnowledgeArticleRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class DiagnosticService {

    private final DiagnosticNodeRepository nodeRepository;
    private final DiagnosticSessionRepository sessionRepository;
    private final KnowledgeArticleRepository articleRepository;
    private final KnowledgeService knowledgeService;

    public DiagnosticService(DiagnosticNodeRepository nodeRepository,
                             DiagnosticSessionRepository sessionRepository,
                             KnowledgeArticleRepository articleRepository,
                             KnowledgeService knowledgeService) {
        this.nodeRepository = nodeRepository;
        this.sessionRepository = sessionRepository;
        this.articleRepository = articleRepository;
        this.knowledgeService = knowledgeService;
    }

    @Transactional
    public DiagnosticSessionResponse start(String category, String customerEmail) {
        DiagnosticNode root = nodeRepository.findByRootTrue()
                .orElseThrow(() -> new ResourceNotFoundException("No diagnostic tree is configured."));
        if (!"question".equalsIgnoreCase(root.getType())) {
            throw new InvalidStateException("The diagnostic tree root must be a question node.");
        }

        DiagnosticSession session = new DiagnosticSession();
        session.setCustomerEmail(customerEmail);
        session.setCurrentNodeId(root.getId());
        return toResponse(sessionRepository.save(session));
    }

    @Transactional(readOnly = true)
    public DiagnosticSessionResponse get(UUID id, String actor, boolean admin) {
        return toResponse(findAccessible(id, actor, admin));
    }

    @Transactional
    public DiagnosticSessionResponse answer(UUID id, DiagnosticAnswerRequest request,
                                            String actor, boolean admin) {
        DiagnosticSession session = findAccessible(id, actor, admin);
        if (session.getStatus() != DiagnosticSessionStatus.IN_PROGRESS) {
            throw new InvalidStateException("This diagnostic session is not waiting for another answer.");
        }

        DiagnosticNode question = session.getCurrentNodeId() == null ? null
                : nodeRepository.findById(session.getCurrentNodeId()).orElse(null);
        if (question == null || !question.getId().equals(request.questionId())) {
            throw new InvalidStateException("The answer does not match the session's current question.");
        }

        DiagnosticOption selected = null;
        if (request.optionId() != null) {
            selected = question.getOptions().stream()
                    .filter(option -> option.getId().equals(request.optionId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("The selected option is not valid for this question."));
        } else if (!question.getOptions().isEmpty()) {
            throw new IllegalArgumentException("An option is required for this question.");
        }

        DiagnosticAnswer answer = new DiagnosticAnswer();
        answer.setSession(session);
        answer.setQuestionId(question.getId());
        answer.setQuestionPrompt(question.getText());
        answer.setAnswerText(trimToNull(request.answerText()));
        if (selected != null) {
            answer.setOptionId(selected.getId());
            answer.setOptionLabel(selected.getLabel());
        }
        session.getAnswers().add(answer);

        DiagnosticNode next = selected == null || selected.getDestinationNodeId() == null
                ? null
                : nodeRepository.findById(selected.getDestinationNodeId())
                    .orElseThrow(() -> new InvalidStateException("The selected option points to a missing diagnostic node."));
        if (next == null) {
            session.setCurrentNodeId(null);
            session.setStatus(DiagnosticSessionStatus.READY_FOR_TICKET);
        } else if ("resolution".equalsIgnoreCase(next.getType())) {
            suggest(session, next);
        } else if ("question".equalsIgnoreCase(next.getType())) {
            session.setCurrentNodeId(next.getId());
        } else {
            throw new InvalidStateException("Unsupported diagnostic node type: " + next.getType());
        }
        return toResponse(sessionRepository.save(session));
    }

    @Transactional
    public DiagnosticSessionResponse recordResolution(UUID id, boolean resolved, String actor, boolean admin) {
        DiagnosticSession session = findAccessible(id, actor, admin);
        if (session.getStatus() != DiagnosticSessionStatus.SOLUTION_SUGGESTED) {
            throw new InvalidStateException("This session does not have a suggested solution to confirm.");
        }
        if (resolved) {
            session.setStatus(DiagnosticSessionStatus.RESOLVED_WITH_FAQ);
            session.setCompletedAt(LocalDateTime.now());
        } else {
            session.setStatus(DiagnosticSessionStatus.READY_FOR_TICKET);
        }
        return toResponse(sessionRepository.save(session));
    }

    /** Validates ownership/state and atomically reserves a session for a ticket. */
    @Transactional
    public DiagnosticSession escalate(UUID id, String actor, boolean admin) {
        DiagnosticSession session = findAccessible(id, actor, admin);
        if (session.getStatus() != DiagnosticSessionStatus.READY_FOR_TICKET
                && session.getStatus() != DiagnosticSessionStatus.SOLUTION_SUGGESTED) {
            throw new InvalidStateException("This diagnostic session cannot be escalated to a ticket.");
        }
        session.setStatus(DiagnosticSessionStatus.ESCALATED);
        session.setCompletedAt(LocalDateTime.now());
        return sessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public PageResponse<KnowledgeSummary> suggestArticles(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("A search query is required.");
        }
        return knowledgeService.searchPublished(query, null, null,
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "updatedAt")));
    }

    private KnowledgeArticle resolveArticle(Long id) {
        if (id == null) return null;
        KnowledgeArticle article = articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Article", id));
        if (article.getStatus() != PublicationStatus.PUBLISHED) {
            throw new InvalidStateException("Diagnostic solutions must reference a published article.");
        }
        return article;
    }

    private DiagnosticSession findAccessible(UUID id, String actor, boolean admin) {
        DiagnosticSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Diagnostic session " + id + " does not exist."));
        if (!admin && !session.getCustomerEmail().equalsIgnoreCase(actor)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You cannot access another customer's diagnostic session.");
        }
        return session;
    }

    private void suggest(DiagnosticSession session, DiagnosticNode resolution) {
        session.setSuggestedArticle(resolveArticle(resolution.getKnowledgeArticleId()));
        session.setSuggestedResolution(resolution.getText());
        session.setCurrentNodeId(null);
        session.setStatus(DiagnosticSessionStatus.SOLUTION_SUGGESTED);
    }

    private DiagnosticSessionResponse toResponse(DiagnosticSession session) {
        DiagnosticNode current = session.getCurrentNodeId() == null ? null
                : nodeRepository.findById(session.getCurrentNodeId()).orElse(null);
        return DiagnosticMapper.toResponse(session, current);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
