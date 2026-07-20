package com.example.demo.repository;

import com.example.demo.model.Faq;
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
 * Persistence for {@link Faq}. Dynamic multi-filter search is handled through
 * {@link JpaSpecificationExecutor}; the explicit queries below cover the
 * uniqueness checks, atomic counter bumps, and the aggregates the dashboard
 * needs, all pushed down to the database rather than computed in memory.
 */
@Repository
public interface FaqRepository extends JpaRepository<Faq, Long>, JpaSpecificationExecutor<Faq> {

    Optional<Faq> findByQuestionIgnoreCase(String question);

    boolean existsByQuestionIgnoreCase(String question);

    Page<Faq> findByStatus(PublicationStatus status, Pageable pageable);

    long countByStatus(PublicationStatus status);

    List<Faq> findTop5ByStatusOrderByViewCountDesc(PublicationStatus status);

    List<Faq> findTop5ByStatusOrderByViewCountAsc(PublicationStatus status);

    List<Faq> findTop5ByStatusOrderByHelpfulCountDesc(PublicationStatus status);

    List<Faq> findTop20ByOrderByUpdatedAtDesc();

    @Query("select coalesce(sum(f.viewCount), 0) from Faq f")
    long sumViewCount();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Faq f set f.viewCount = f.viewCount + 1 where f.id = :id")
    int incrementViewCount(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Faq f set f.helpfulCount = f.helpfulCount + 1 where f.id = :id")
    int incrementHelpfulCount(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Faq f set f.notHelpfulCount = f.notHelpfulCount + 1 where f.id = :id")
    int incrementNotHelpfulCount(@Param("id") Long id);
}
