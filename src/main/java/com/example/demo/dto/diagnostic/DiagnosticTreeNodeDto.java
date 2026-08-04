package com.example.demo.dto.diagnostic;

import java.util.List;
import java.util.UUID;

public record DiagnosticTreeNodeDto(
        UUID id,
        String type,
        String text,
        Long knowledgeArticleId,
        List<DiagnosticTreeOptionDto> options) {
}
