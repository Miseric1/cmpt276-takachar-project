package com.example.demo.mapper;

import com.example.demo.dto.diagnostic.DiagnosticOptionDto;
import com.example.demo.dto.diagnostic.DiagnosticQuestionDto;
import com.example.demo.dto.diagnostic.DiagnosticSessionResponse;
import com.example.demo.dto.diagnostic.DiagnosticTrailDto;
import com.example.demo.dto.knowledge.ArticleReference;
import com.example.demo.model.DiagnosticAnswer;
import com.example.demo.model.DiagnosticQuestion;
import com.example.demo.model.DiagnosticSession;

public final class DiagnosticMapper {
    private DiagnosticMapper() {
    }

    public static DiagnosticQuestionDto toQuestion(DiagnosticQuestion question) {
        if (question == null) return null;
        return new DiagnosticQuestionDto(
                question.getId(),
                question.getKey(),
                question.getPrompt(),
                question.getCategory(),
                question.isRootQuestion(),
                question.isActive(),
                question.getSuggestedArticle() == null ? null : question.getSuggestedArticle().getId(),
                question.getOptions().stream()
                        .map(option -> new DiagnosticOptionDto(option.getId(), option.getLabel(), option.getValue(),
                                option.getNextQuestion() == null ? null : option.getNextQuestion().getKey(),
                                option.getSuggestedArticle() == null ? null : option.getSuggestedArticle().getId()))
                        .toList());
    }

    public static DiagnosticTrailDto toTrail(DiagnosticAnswer answer) {
        return new DiagnosticTrailDto(answer.getQuestionId(), answer.getQuestionPrompt(),
                answer.getOptionId(), answer.getOptionLabel(), answer.getAnswerText(), answer.getAnsweredAt());
    }

    public static DiagnosticSessionResponse toResponse(DiagnosticSession session) {
        return new DiagnosticSessionResponse(
                session.getId(),
                session.getStatus(),
                toQuestion(session.getCurrentQuestion()),
                session.getSuggestedArticle() == null ? null : ArticleReference.from(session.getSuggestedArticle()),
                session.getAnswers().stream().map(DiagnosticMapper::toTrail).toList(),
                session.getCreatedAt(), session.getUpdatedAt(), session.getCompletedAt());
    }
}
