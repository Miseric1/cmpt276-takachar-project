package com.example.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "diagnostic_answers")
public class DiagnosticAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private DiagnosticSession session;

    @Column(nullable = false)
    private UUID questionId;

    @Column(nullable = false, length = 500)
    private String questionPrompt;

    private UUID optionId;

    @Column(length = 250)
    private String optionLabel;

    @Column(length = 1000)
    private String answerText;

    @Column(nullable = false, updatable = false)
    private LocalDateTime answeredAt;

    @PrePersist
    void onCreate() { answeredAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public DiagnosticSession getSession() { return session; }
    public void setSession(DiagnosticSession session) { this.session = session; }
    public UUID getQuestionId() { return questionId; }
    public void setQuestionId(UUID questionId) { this.questionId = questionId; }
    public String getQuestionPrompt() { return questionPrompt; }
    public void setQuestionPrompt(String questionPrompt) { this.questionPrompt = questionPrompt; }
    public UUID getOptionId() { return optionId; }
    public void setOptionId(UUID optionId) { this.optionId = optionId; }
    public String getOptionLabel() { return optionLabel; }
    public void setOptionLabel(String optionLabel) { this.optionLabel = optionLabel; }
    public String getAnswerText() { return answerText; }
    public void setAnswerText(String answerText) { this.answerText = answerText; }
    public LocalDateTime getAnsweredAt() { return answeredAt; }
}
