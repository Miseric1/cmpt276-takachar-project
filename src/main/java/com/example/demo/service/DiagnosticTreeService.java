package com.example.demo.service;

import com.example.demo.exception.InvalidStateException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.DiagnosticNode;
import com.example.demo.model.DiagnosticOption;
import com.example.demo.model.KnowledgeArticle;
import com.example.demo.model.PublicationStatus;
import com.example.demo.repository.DiagnosticNodeRepository;
import com.example.demo.repository.DiagnosticOptionRepository;
import com.example.demo.repository.KnowledgeArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DiagnosticTreeService {

    private final DiagnosticNodeRepository nodeRepo;
    private final DiagnosticOptionRepository optionRepo;
    private final KnowledgeArticleRepository articleRepo;

    // ── DTOs ─────────────────────────────────────────────────────────────────

    /** Outbound: one option as seen by the frontend. */
    public record OptionDTO(UUID id, String label, UUID nextId) {}

    /** Outbound: one node as seen by the frontend. */
    public record NodeDTO(
        UUID id,
        String type,
        String text,
        boolean isRoot,
        Long knowledgeArticleId,
        List<OptionDTO> options
    ) {}

    /**
     * Outbound: full tree payload.
     * nodes is a flat map so the frontend can look up any node by ID in O(1).
     */
    public record TreeResponse(UUID rootId, Map<UUID, NodeDTO> nodes) {}

    /** Inbound: one option sent by the frontend on save. */
    public record OptionRequest(UUID id, String label, UUID nextId) {}

    /** Inbound: one node sent by the frontend on save. */
    public record NodeRequest(String type, String text, Long knowledgeArticleId,
                              List<OptionRequest> options) {}

    /**
     * Inbound: full save payload.
     * JSON map keys are strings, so the Map key type is String here;
     * the service converts them to UUID internally.
     */
    public record SaveTreeRequest(UUID rootId, Map<String, NodeRequest> nodes) {}

    // ── READ ─────────────────────────────────────────────────────────────────

    /**
     * Returns the full tree or null if the tree has not been saved yet.
     * All nodes are loaded eagerly (options fetch = EAGER on the entity),
     * so open-in-view=false causes no issues.
     */
    @Transactional(readOnly = true)
    public TreeResponse getTree() {
        List<DiagnosticNode> all = nodeRepo.findAll();
        if (all.isEmpty()) return null;

        UUID rootId = all.stream()
            .filter(DiagnosticNode::isRoot)
            .map(DiagnosticNode::getId)
            .findFirst()
            .orElse(null);

        Map<UUID, NodeDTO> nodeMap = all.stream().collect(Collectors.toMap(
            DiagnosticNode::getId,
            n -> new NodeDTO(
                n.getId(),
                n.getType(),
                n.getText(),
                n.isRoot(),
                n.getKnowledgeArticleId(),
                n.getOptions().stream()
                    .map(o -> new OptionDTO(o.getId(), o.getLabel(), o.getDestinationNodeId()))
                    .toList()
            )
        ));

        return new TreeResponse(rootId, nodeMap);
    }

    // ── WRITE ────────────────────────────────────────────────────────────────

    /**
     * Full replace: deletes the existing tree and persists the new one.
     *
     * Delete order:
     *   1. diagnostic_options  (has FK → diagnostic_nodes, so must go first)
     *   2. diagnostic_nodes
     *
     * Both @Modifying queries use clearAutomatically = true, which flushes the
     * SQL statement and wipes the L1 cache.  The subsequent persist() calls
     * therefore start with a clean slate and always produce INSERTs.
     */
    @Transactional
    public TreeResponse saveTree(SaveTreeRequest req) {
        optionRepo.deleteAllOptions();  // FK child first
        nodeRepo.deleteAllNodes();      // FK parent second

        req.nodes().forEach((idStr, nodeReq) -> {
            UUID nodeId = UUID.fromString(idStr);
            boolean isRoot = nodeId.equals(req.rootId());

            List<DiagnosticOption> opts = new ArrayList<>();
            if (nodeReq.options() != null) {
                for (int i = 0; i < nodeReq.options().size(); i++) {
                    OptionRequest o = nodeReq.options().get(i);
                    opts.add(DiagnosticOption.builder()
                        .id(o.id() != null ? o.id() : UUID.randomUUID())
                        .label(o.label())
                        .destinationNodeId(o.nextId())
                        .sortOrder(i)
                        .build());
                }
            }

            // save() on a non-existing ID → Hibernate does SELECT (not found) → INSERT
            nodeRepo.save(DiagnosticNode.builder()
                .id(nodeId)
                .type(nodeReq.type())
                .text(nodeReq.text())
                .root(isRoot)
                .knowledgeArticleId(resolveArticleId(nodeReq.knowledgeArticleId()))
                .options(opts)
                .build());
        });

        return getTree();
    }

    private Long resolveArticleId(Long id) {
        if (id == null) return null;
        KnowledgeArticle article = articleRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Article", id));
        if (article.getStatus() != PublicationStatus.PUBLISHED) {
            throw new InvalidStateException("Diagnostic resolutions can reference only published FAQ articles.");
        }
        return article.getId();
    }
}
