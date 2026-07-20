package com.example.demo.repository;

import com.example.demo.model.FaqVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FaqVersionRepository extends JpaRepository<FaqVersion, Long> {

    List<FaqVersion> findByFaqIdOrderByVersionNumberDesc(Long faqId);

    Optional<FaqVersion> findByFaqIdAndVersionNumber(Long faqId, int versionNumber);
}
