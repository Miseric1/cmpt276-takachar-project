package com.example.demo.repository;

import com.example.demo.model.DiagnosticSession;
import com.example.demo.model.DiagnosticSessionStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DiagnosticSessionRepository extends JpaRepository<DiagnosticSession, UUID> {
    long countByStatus(DiagnosticSessionStatus status);
}
