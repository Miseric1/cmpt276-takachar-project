package com.example.demo.dto.diagnostic;

import java.time.LocalDateTime;
import java.util.UUID;

public record DiagnosticTrailDto(
        UUID questionId,
        String question,
        UUID optionId,
        String selectedOption,
        String answerText,
        LocalDateTime answeredAt) {
}
