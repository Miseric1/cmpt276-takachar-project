package com.example.demo.dto.diagnostic;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DiagnosticAnswerRequest(
        @NotNull Long questionId,
        Long optionId,
        @Size(max = 1000) String answerText) {
}
