package com.example.demo.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "diagnostic_questions")
public class DiagnosticQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_key", nullable = false, unique = true, length = 100)
    private String key;

    @Column(nullable = false, length = 500)
    private String prompt;

    @Column(nullable = false, length = 150)
    private String category;

    @Column(nullable = false)
    private boolean rootQuestion;

    @Column(nullable = false)
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suggested_article_id")
    private KnowledgeArticle suggestedArticle;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC, id ASC")
    private List<WorkflowDiagnosticOption> options = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public boolean isRootQuestion() { return rootQuestion; }
    public void setRootQuestion(boolean rootQuestion) { this.rootQuestion = rootQuestion; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public KnowledgeArticle getSuggestedArticle() { return suggestedArticle; }
    public void setSuggestedArticle(KnowledgeArticle suggestedArticle) { this.suggestedArticle = suggestedArticle; }
    public List<WorkflowDiagnosticOption> getOptions() { return options; }
    public void setOptions(List<WorkflowDiagnosticOption> options) { this.options = options; }
}
