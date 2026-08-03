package com.example.demo.dto.diagnostic;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record DiagnosticQuestionRequest(
        @NotBlank @Size(max = 100) String key,
        @NotBlank @Size(max = 500) String prompt,
        @NotBlank @Size(max = 150) String category,
        boolean rootQuestion,
        boolean active,
        Long suggestedArticleId,
        List<@Valid DiagnosticOptionRequest> options) {
}
