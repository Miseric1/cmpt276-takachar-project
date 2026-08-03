package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One node in the diagnostic tree — either a "question" or a "resolution".
 *
 * Table: diagnostic_nodes
 * H2 + ddl-auto=update → table is created automatically.
 */
@Entity
@Table(name = "diagnostic_nodes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosticNode {

    /** UUID supplied by the frontend (crypto.randomUUID()). */
    @Id
    private UUID id;

    /**
     * "question" or "resolution".
     * 20 chars is plenty; using a CHECK constraint would need DB-level support.
     */
    @Column(nullable = false, length = 20)
    private String type;

    /** The question text or resolution body — up to 2 000 chars. */
    @Column(nullable = false, length = 2000)
    private String text;

    /**
     * Marks the single entry-point node for the tree.
     * Column named is_root to avoid clashing with any SQL reserved word.
     */
    @Column(name = "is_root", nullable = false)
    private boolean root;

    /**
     * Ordered list of answer options.
     *
     * Unidirectional @OneToMany with @JoinColumn:
     *  - Hibernate writes source_node_id on the options rows.
     *  - cascade ALL  → options are persisted / deleted with their parent node.
     *  - orphanRemoval → removing an option from this list deletes it from DB.
     *  - EAGER         → options are always loaded with the node (tree is small).
     *
     * Trade-off: Hibernate issues one extra UPDATE per option on insert to set
     * source_node_id (the known cost of unidirectional @OneToMany + @JoinColumn).
     * Acceptable for a small diagnostic tree.
     */
    @OneToMany(
        cascade       = CascadeType.ALL,
        orphanRemoval = true,
        fetch         = FetchType.EAGER
    )
    @JoinColumn(name = "source_node_id")  // FK column lives on diagnostic_options
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<DiagnosticOption> options = new ArrayList<>();


    // Code below for adding matching FAQs in the future
    @Column(name = "knowledge_article_id")
    private Long knowledgeArticleId;

    public Long getKnowledgeArticleId() {
        return knowledgeArticleId;
    }
    
    public void setKnowledgeArticleId(Long knowledgeArticleId) {
        this.knowledgeArticleId = knowledgeArticleId;
    }
}