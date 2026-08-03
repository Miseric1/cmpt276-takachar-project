package com.example.demo.dto.diagnostic;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DiagnosticOptionRequest(
        @NotBlank @Size(max = 250) String label,
        @NotBlank @Size(max = 100) String value,
        int displayOrder,
        String nextQuestionKey,
        Long suggestedArticleId) {
}
