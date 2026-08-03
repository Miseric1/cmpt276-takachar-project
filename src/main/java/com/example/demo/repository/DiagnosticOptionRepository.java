package com.example.demo.repository;

import com.example.demo.model.DiagnosticOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DiagnosticOptionRepository extends JpaRepository<DiagnosticOption, UUID> {

    /**
     * Bulk-delete every option row before wiping nodes.
     * Must run before deleteAllNodes() to satisfy the FK constraint on
     * source_node_id (diagnostic_options → diagnostic_nodes).
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM DiagnosticOption o")
    void deleteAllOptions();
}