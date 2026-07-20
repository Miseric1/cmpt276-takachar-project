package com.example.demo.dto.dashboard;

/**
 * A compact "top content" row used in leaderboards (most viewed, most helpful,
 * recently updated) for Knowledge articles.
 */
public record PopularContent(Long id, String title, long viewCount, long helpfulCount) {
}
