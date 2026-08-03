package com.example.demo.controller;


import com.example.demo.service.DiagnosticTreeService;
import com.example.demo.service.DiagnosticTreeService.SaveTreeRequest;
import com.example.demo.service.DiagnosticTreeService.TreeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller used exclusively by the admin diagnostic-tree page.
 *
 * Two endpoints:
 *   GET  /api/tree  → load the full tree (flat node map + rootId)
 *   PUT  /api/tree  → full replace (delete all, re-insert from payload)
 *
 * The frontend (admin-tree.html) keeps all edits in local JS state and only
 * calls PUT when the admin clicks "Save changes", so every PUT is authoritative.
 *
 * ── Adjusting to your service ───────────────────────────────────────────────
 * This controller assumes the service exposes:
 *
 *   TreeResponse getTree()
 *   TreeResponse saveTree(SaveTreeRequest req)
 *
 * where TreeResponse is:  { UUID rootId, Map<UUID, NodeDTO> nodes }
 * and   NodeDTO is:       { UUID id, String type, String text,
 *                           boolean isRoot, List<OptionDTO> options }
 * and   OptionDTO is:     { UUID id, String label, UUID nextId }
 *
 * If your service uses different method names or DTOs, update the calls in
 * getTree() and saveTree() below — the controller logic itself does not change.
 * ────────────────────────────────────────────────────────────────────────────
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DiagnosticApiController {

    private final DiagnosticTreeService treeService;

    // ── GET /api/tree ────────────────────────────────────────────────────────
    /**
     * Returns the full tree as a flat node map.
     * 204 No Content when no tree exists yet — the frontend will build a
     * default tree from the SERVER_CATEGORIES list and prompt the admin to save.
     */
    @GetMapping("/tree")
    public ResponseEntity<TreeResponse> getTree() {
        TreeResponse tree = treeService.getTree();
        if (tree == null) {
            return ResponseEntity.noContent().build();   // 204
        }
        return ResponseEntity.ok(tree);
    }

    // ── PUT /api/tree ────────────────────────────────────────────────────────
    /**
     * Full replace: the frontend sends every node and option in the tree.
     * The service deletes the existing tree and re-inserts everything.
     *
     * Request body shape (mirrors the JS payload in admin-tree.html):
     * {
     *   "rootId": "uuid",
     *   "nodes": {
     *     "uuid": {
     *       "type":    "question" | "resolution",
     *       "text":    "...",
     *       "options": [
     *         { "id": "uuid", "label": "...", "nextId": "uuid | null" }
     *       ]
     *     }
     *   }
     * }
     *
     * Returns the saved tree (as read back from the database) so the frontend
     * can update its own savedTree snapshot with server-assigned values.
     */
    @PutMapping("/tree")
    public ResponseEntity<TreeResponse> saveTree(@RequestBody SaveTreeRequest req) {
        TreeResponse saved = treeService.saveTree(req);
        return ResponseEntity.ok(saved);
    }
}