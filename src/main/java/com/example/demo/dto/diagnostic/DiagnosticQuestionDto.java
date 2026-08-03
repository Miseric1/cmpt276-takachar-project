package com.example.demo.dto.diagnostic;

import java.util.List;

public record DiagnosticQuestionDto(
        Long id,
        String key,
        String prompt,
        String category,
        boolean rootQuestion,
        boolean active,
        Long suggestedArticleId,
        List<DiagnosticOptionDto> options) {
}
