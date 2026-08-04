package com.example.demo.repository;

import com.example.demo.model.Feedback;
import com.example.demo.model.SubmissionType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    List<Feedback> findByStatus(String status);

    List<Feedback> findByCreatedBy(String createdBy);

    List<Feedback> findByType(SubmissionType type, Sort sort);

    // ----- Dashboard analytics support -------------------------------------

    long countByStatus(String status);

    @Query("select f.status, count(f) from Feedback f group by f.status")
    List<Object[]> countGroupedByStatus();

    @Query("select f.category, count(f) from Feedback f group by f.category")
    List<Object[]> countGroupedByCategory();

    @Query("select f.createdAt from Feedback f where f.createdAt >= :since")
    List<LocalDateTime> findCreatedAtSince(@Param("since") LocalDateTime since);

    List<Feedback> findTop20ByOrderByCreatedAtDesc();
}
