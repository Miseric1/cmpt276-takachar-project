package com.example.demo.dto.diagnostic;

import com.example.demo.dto.knowledge.ArticleReference;
import com.example.demo.model.DiagnosticSessionStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record DiagnosticSessionResponse(
        UUID id,
        DiagnosticSessionStatus status,
        DiagnosticTreeNodeDto currentQuestion,
        ArticleReference suggestedArticle,
        String suggestedResolution,
        List<DiagnosticTrailDto> answers,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime completedAt) {
}
