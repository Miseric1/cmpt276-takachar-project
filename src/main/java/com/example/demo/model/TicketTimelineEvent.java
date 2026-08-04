package com.example.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "ticket_timeline_events")
public class TicketTimelineEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private SupportTicket ticket;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TicketEventType type;

    @Column(nullable = false)
    private String actorEmail;

    @Column(nullable = false, length = 2000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private TicketStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private TicketStatus toStatus;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public SupportTicket getTicket() { return ticket; }
    public void setTicket(SupportTicket ticket) { this.ticket = ticket; }
    public TicketEventType getType() { return type; }
    public void setType(TicketEventType type) { this.type = type; }
    public String getActorEmail() { return actorEmail; }
    public void setActorEmail(String actorEmail) { this.actorEmail = actorEmail; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public TicketStatus getFromStatus() { return fromStatus; }
    public void setFromStatus(TicketStatus fromStatus) { this.fromStatus = fromStatus; }
    public TicketStatus getToStatus() { return toStatus; }
    public void setToStatus(TicketStatus toStatus) { this.toStatus = toStatus; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
