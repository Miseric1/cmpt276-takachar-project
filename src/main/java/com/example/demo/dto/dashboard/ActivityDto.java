package com.example.demo.dto.dashboard;

import java.time.LocalDateTime;

/**
 * One entry in the recent-activity feed. {@code type} is a stable machine key
 * (for example FAQ_UPDATED, ARTICLE_PUBLISHED, FEEDBACK_SUBMITTED) the frontend
 * can map to an icon; {@code message} is a ready-to-display summary.
 */
public record ActivityDto(
        String type,
        String message,
        Long referenceId,
        String actor,
        LocalDateTime timestamp) {
}
