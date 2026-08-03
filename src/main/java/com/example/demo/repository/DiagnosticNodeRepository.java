package com.example.demo.repository;

import com.example.demo.model.DiagnosticNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DiagnosticNodeRepository extends JpaRepository<DiagnosticNode, UUID> {

    /** Finds the single root node (is_root = true). */
    Optional<DiagnosticNode> findByRootTrue();

    /**
     * Bulk-delete every node via JPQL.
     * clearAutomatically = true flushes the statement then wipes the L1 cache,
     * so the service can persist new entities in the same transaction without
     * Hibernate confusing "just deleted" state with "to be updated" state.
     *
     * NOTE: call deleteAllOptions() on DiagnosticOptionRepository FIRST —
     * diagnostic_options.source_node_id is a nullable FK, but clearing options
     * first is the correct order regardless.
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM DiagnosticNode n")
    void deleteAllNodes();
}