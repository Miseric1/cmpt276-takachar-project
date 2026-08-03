package com.example.demo.dto.diagnostic;

import java.time.LocalDateTime;

public record DiagnosticTrailDto(
        Long questionId,
        String question,
        Long optionId,
        String selectedOption,
        String answerText,
        LocalDateTime answeredAt) {
}
