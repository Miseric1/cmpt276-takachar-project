package com.example.demo.repository;

import com.example.demo.model.KnowledgeArticle;
import com.example.demo.model.PublicationStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Persistence for {@link KnowledgeArticle}. Dynamic multi-filter search runs
 * through {@link JpaSpecificationExecutor}; the explicit queries cover
 * uniqueness checks, atomic counter bumps, and dashboard aggregates.
 */
@Repository
public interface KnowledgeArticleRepository
        extends JpaRepository<KnowledgeArticle, Long>, JpaSpecificationExecutor<KnowledgeArticle> {

    Optional<KnowledgeArticle> findByTitleIgnoreCase(String title);

    boolean existsByTitleIgnoreCase(String title);

    long countByStatus(PublicationStatus status);

    Page<KnowledgeArticle> findByStatus(PublicationStatus status, Pageable pageable);

    List<KnowledgeArticle> findTop5ByStatusOrderByViewCountDesc(PublicationStatus status);

    List<KnowledgeArticle> findTop5ByStatusOrderByUpdatedAtDesc(PublicationStatus status);

    List<KnowledgeArticle> findTop20ByOrderByUpdatedAtDesc();

    @Query("select coalesce(sum(a.viewCount), 0) from KnowledgeArticle a")
    long sumViewCount();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update KnowledgeArticle a set a.viewCount = a.viewCount + 1 where a.id = :id")
    int incrementViewCount(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update KnowledgeArticle a set a.helpfulCount = a.helpfulCount + 1 where a.id = :id")
    int incrementHelpfulCount(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update KnowledgeArticle a set a.notHelpfulCount = a.notHelpfulCount + 1 where a.id = :id")
    int incrementNotHelpfulCount(@Param("id") Long id);
}
