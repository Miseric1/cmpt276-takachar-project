package com.example.demo.dto.diagnostic;

public record DiagnosticOptionDto(
        Long id,
        String label,
        String value,
        String nextQuestionKey,
        Long suggestedArticleId) {
}
