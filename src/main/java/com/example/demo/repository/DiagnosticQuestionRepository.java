package com.example.demo.repository;

import com.example.demo.model.DiagnosticQuestion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DiagnosticQuestionRepository extends JpaRepository<DiagnosticQuestion, Long> {
    Optional<DiagnosticQuestion> findByKey(String key);
    boolean existsByKey(String key);
    Optional<DiagnosticQuestion> findFirstByRootQuestionTrueAndActiveTrueAndCategoryIgnoreCaseOrderByIdAsc(String category);
    Optional<DiagnosticQuestion> findFirstByRootQuestionTrueAndActiveTrueOrderByIdAsc();
    List<DiagnosticQuestion> findAllByOrderByCategoryAscIdAsc();
}
