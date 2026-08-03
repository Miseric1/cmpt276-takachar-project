package com.example.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Ticket-workflow metadata retained for historical diagnostic sessions.
 * PR #17 owns the primary {@link DiagnosticOption} tree model.
 */
@Entity
@Table(name = "workflow_diagnostic_options")
public class WorkflowDiagnosticOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private DiagnosticQuestion question;

    @Column(nullable = false, length = 250)
    private String label;

    @Column(name = "option_value", nullable = false, length = 100)
    private String value;

    @Column(nullable = false)
    private int displayOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "next_question_id")
    private DiagnosticQuestion nextQuestion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suggested_article_id")
    private KnowledgeArticle suggestedArticle;

    public Long getId() { return id; }
    public DiagnosticQuestion getQuestion() { return question; }
    public void setQuestion(DiagnosticQuestion question) { this.question = question; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
    public DiagnosticQuestion getNextQuestion() { return nextQuestion; }
    public void setNextQuestion(DiagnosticQuestion nextQuestion) { this.nextQuestion = nextQuestion; }
    public KnowledgeArticle getSuggestedArticle() { return suggestedArticle; }
    public void setSuggestedArticle(KnowledgeArticle suggestedArticle) { this.suggestedArticle = suggestedArticle; }
}
