package com.example.demo.mapper;

import com.example.demo.dto.diagnostic.DiagnosticSessionResponse;
import com.example.demo.dto.diagnostic.DiagnosticTrailDto;
import com.example.demo.dto.diagnostic.DiagnosticTreeNodeDto;
import com.example.demo.dto.diagnostic.DiagnosticTreeOptionDto;
import com.example.demo.dto.knowledge.ArticleReference;
import com.example.demo.model.DiagnosticAnswer;
import com.example.demo.model.DiagnosticNode;
import com.example.demo.model.DiagnosticSession;

public final class DiagnosticMapper {
    private DiagnosticMapper() {
    }

    public static DiagnosticTrailDto toTrail(DiagnosticAnswer answer) {
        return new DiagnosticTrailDto(answer.getQuestionId(), answer.getQuestionPrompt(),
                answer.getOptionId(), answer.getOptionLabel(), answer.getAnswerText(), answer.getAnsweredAt());
    }

    public static DiagnosticTreeNodeDto toTreeNode(DiagnosticNode node) {
        if (node == null) return null;
        return new DiagnosticTreeNodeDto(
                node.getId(), node.getType(), node.getText(), node.getKnowledgeArticleId(),
                node.getOptions().stream()
                        .map(option -> new DiagnosticTreeOptionDto(
                                option.getId(), option.getLabel(), option.getDestinationNodeId()))
                        .toList());
    }

    public static DiagnosticSessionResponse toResponse(DiagnosticSession session, DiagnosticNode currentNode) {
        return new DiagnosticSessionResponse(
                session.getId(),
                session.getStatus(),
                toTreeNode(currentNode),
                session.getSuggestedArticle() == null ? null : ArticleReference.from(session.getSuggestedArticle()),
                session.getSuggestedResolution(),
                session.getAnswers().stream().map(DiagnosticMapper::toTrail).toList(),
                session.getCreatedAt(), session.getUpdatedAt(), session.getCompletedAt());
    }
}
