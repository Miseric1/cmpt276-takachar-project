package com.example.demo.service;

import com.example.demo.dto.PageResponse;
import com.example.demo.dto.diagnostic.DiagnosticAnswerRequest;
import com.example.demo.dto.diagnostic.DiagnosticOptionRequest;
import com.example.demo.dto.diagnostic.DiagnosticQuestionDto;
import com.example.demo.dto.diagnostic.DiagnosticQuestionRequest;
import com.example.demo.dto.diagnostic.DiagnosticSessionResponse;
import com.example.demo.dto.knowledge.KnowledgeSummary;
import com.example.demo.exception.DuplicateResourceException;
import com.example.demo.exception.InvalidStateException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.mapper.DiagnosticMapper;
import com.example.demo.model.DiagnosticAnswer;
import com.example.demo.model.WorkflowDiagnosticOption;
import com.example.demo.model.DiagnosticQuestion;
import com.example.demo.model.DiagnosticSession;
import com.example.demo.model.DiagnosticSessionStatus;
import com.example.demo.model.KnowledgeArticle;
import com.example.demo.model.PublicationStatus;
import com.example.demo.repository.DiagnosticQuestionRepository;
import com.example.demo.repository.DiagnosticSessionRepository;
import com.example.demo.repository.KnowledgeArticleRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class DiagnosticService {

    private final DiagnosticQuestionRepository questionRepository;
    private final DiagnosticSessionRepository sessionRepository;
    private final KnowledgeArticleRepository articleRepository;
    private final KnowledgeService knowledgeService;

    public DiagnosticService(DiagnosticQuestionRepository questionRepository,
                             DiagnosticSessionRepository sessionRepository,
                             KnowledgeArticleRepository articleRepository,
                             KnowledgeService knowledgeService) {
        this.questionRepository = questionRepository;
        this.sessionRepository = sessionRepository;
        this.articleRepository = articleRepository;
        this.knowledgeService = knowledgeService;
    }

    @Transactional
    public DiagnosticSessionResponse start(String category, String customerEmail) {
        DiagnosticQuestion root = category == null || category.isBlank()
                ? questionRepository.findFirstByRootQuestionTrueAndActiveTrueOrderByIdAsc()
                    .orElseThrow(() -> new ResourceNotFoundException("No active diagnostic tree is configured."))
                : questionRepository.findFirstByRootQuestionTrueAndActiveTrueAndCategoryIgnoreCaseOrderByIdAsc(category)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "No active diagnostic tree is configured for category " + category + "."));

        DiagnosticSession session = new DiagnosticSession();
        session.setCustomerEmail(customerEmail);
        session.setCurrentQuestion(root);
        return DiagnosticMapper.toResponse(sessionRepository.save(session));
    }

    @Transactional(readOnly = true)
    public DiagnosticSessionResponse get(UUID id, String actor, boolean admin) {
        return DiagnosticMapper.toResponse(findAccessible(id, actor, admin));
    }

    @Transactional
    public DiagnosticSessionResponse answer(UUID id, DiagnosticAnswerRequest request,
                                            String actor, boolean admin) {
        DiagnosticSession session = findAccessible(id, actor, admin);
        if (session.getStatus() != DiagnosticSessionStatus.IN_PROGRESS) {
            throw new InvalidStateException("This diagnostic session is not waiting for another answer.");
        }

        DiagnosticQuestion question = session.getCurrentQuestion();
        if (question == null || !question.getId().equals(request.questionId())) {
            throw new InvalidStateException("The answer does not match the session's current question.");
        }

        WorkflowDiagnosticOption selected = null;
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
        answer.setQuestionPrompt(question.getPrompt());
        answer.setAnswerText(trimToNull(request.answerText()));
        if (selected != null) {
            answer.setOptionId(selected.getId());
            answer.setOptionLabel(selected.getLabel());
        }
        session.getAnswers().add(answer);

        KnowledgeArticle suggestion = selected != null ? selected.getSuggestedArticle() : null;
        DiagnosticQuestion next = selected != null ? selected.getNextQuestion() : null;
        if (suggestion != null) {
            suggest(session, suggestion);
        } else if (next != null && next.isActive()) {
            session.setCurrentQuestion(next);
        } else if (question.getSuggestedArticle() != null) {
            suggest(session, question.getSuggestedArticle());
        } else {
            session.setCurrentQuestion(null);
            session.setStatus(DiagnosticSessionStatus.READY_FOR_TICKET);
        }
        return DiagnosticMapper.toResponse(sessionRepository.save(session));
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
        return DiagnosticMapper.toResponse(sessionRepository.save(session));
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

    @Transactional(readOnly = true)
    public List<DiagnosticQuestionDto> listQuestions() {
        return questionRepository.findAllByOrderByCategoryAscIdAsc().stream()
                .map(DiagnosticMapper::toQuestion).toList();
    }

    @Transactional
    public DiagnosticQuestionDto createQuestion(DiagnosticQuestionRequest request) {
        String key = request.key().trim();
        if (questionRepository.existsByKey(key)) {
            throw new DuplicateResourceException("A diagnostic question with this key already exists.");
        }
        DiagnosticQuestion question = new DiagnosticQuestion();
        apply(question, request);
        return DiagnosticMapper.toQuestion(questionRepository.save(question));
    }

    @Transactional
    public DiagnosticQuestionDto updateQuestion(Long id, DiagnosticQuestionRequest request) {
        DiagnosticQuestion question = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Diagnostic question", id));
        questionRepository.findByKey(request.key().trim())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> { throw new DuplicateResourceException(
                        "A diagnostic question with this key already exists."); });
        apply(question, request);
        return DiagnosticMapper.toQuestion(questionRepository.save(question));
    }

    /** Questions are deactivated rather than deleted so historical trails stay valid. */
    @Transactional
    public void deactivateQuestion(Long id) {
        DiagnosticQuestion question = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Diagnostic question", id));
        question.setActive(false);
        question.setRootQuestion(false);
        questionRepository.save(question);
    }

    private void apply(DiagnosticQuestion question, DiagnosticQuestionRequest request) {
        question.setKey(request.key().trim());
        question.setPrompt(request.prompt().trim());
        question.setCategory(request.category().trim());
        question.setRootQuestion(request.rootQuestion());
        question.setActive(request.active());
        question.setSuggestedArticle(resolveArticle(request.suggestedArticleId()));

        questionRepository.save(question);
        question.getOptions().clear();
        List<WorkflowDiagnosticOption> options = new ArrayList<>();
        if (request.options() != null) {
            for (DiagnosticOptionRequest optionRequest : request.options()) {
                WorkflowDiagnosticOption option = new WorkflowDiagnosticOption();
                option.setQuestion(question);
                option.setLabel(optionRequest.label().trim());
                option.setValue(optionRequest.value().trim());
                option.setDisplayOrder(optionRequest.displayOrder());
                option.setSuggestedArticle(resolveArticle(optionRequest.suggestedArticleId()));
                if (optionRequest.nextQuestionKey() != null && !optionRequest.nextQuestionKey().isBlank()) {
                    DiagnosticQuestion next = questionRepository.findByKey(optionRequest.nextQuestionKey().trim())
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Next diagnostic question with key '" + optionRequest.nextQuestionKey() + "' does not exist."));
                    if (next.getId().equals(question.getId())) {
                        throw new IllegalArgumentException("A diagnostic option cannot loop to its own question.");
                    }
                    option.setNextQuestion(next);
                }
                options.add(option);
            }
        }
        question.getOptions().addAll(options);
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

    private void suggest(DiagnosticSession session, KnowledgeArticle article) {
        session.setSuggestedArticle(article);
        session.setCurrentQuestion(null);
        session.setStatus(DiagnosticSessionStatus.SOLUTION_SUGGESTED);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
