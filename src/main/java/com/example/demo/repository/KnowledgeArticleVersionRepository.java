package com.example.demo.repository;

import com.example.demo.model.KnowledgeArticleVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KnowledgeArticleVersionRepository extends JpaRepository<KnowledgeArticleVersion, Long> {

    List<KnowledgeArticleVersion> findByArticleIdOrderByVersionNumberDesc(Long articleId);

    Optional<KnowledgeArticleVersion> findByArticleIdAndVersionNumber(Long articleId, int versionNumber);
}
