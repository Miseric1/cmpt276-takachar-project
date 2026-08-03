package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;


import java.util.UUID;

/**
 * One answer option on a DiagnosticNode question.
 *
 * Table: diagnostic_options
 * source_node_id (FK → diagnostic_nodes.id) is written by Hibernate via the
 * parent's @JoinColumn — this class does not expose that column.
 */
@Entity
@Table(name = "diagnostic_options")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosticOption {

    /** UUID supplied by the frontend. */
    @Id
    private UUID id;

    /** Button label shown to the user, e.g. "Both LEDs are off". */
    @Column(nullable = false, length = 500)
    private String label;

    /**
     * ID of the DiagnosticNode this option routes to.
     * Nullable — an option can be "unlinked" while the admin is building the tree.
     * Not a JPA relationship; kept as a plain UUID to avoid circular fetch issues.
     */
    @Column(name = "destination_node_id")
    private UUID destinationNodeId;

    /** Zero-based display order within its parent question. */
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
} 