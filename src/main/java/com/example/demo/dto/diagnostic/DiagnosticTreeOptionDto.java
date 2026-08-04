package com.example.demo.dto.diagnostic;

import java.util.UUID;

public record DiagnosticTreeOptionDto(
        UUID id,
        String label,
        UUID nextId) {
}
