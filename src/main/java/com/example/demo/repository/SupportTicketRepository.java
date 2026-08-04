package com.example.demo.repository;

import com.example.demo.model.SupportTicket;
import com.example.demo.model.TicketStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SupportTicketRepository
        extends JpaRepository<SupportTicket, Long>, JpaSpecificationExecutor<SupportTicket> {
    boolean existsByReferenceNumber(String referenceNumber);
    long countByStatus(TicketStatus status);
    long countByStatusNotIn(List<TicketStatus> statuses);
    long countByTargetResolutionAtBeforeAndStatusNotIn(LocalDateTime target, List<TicketStatus> statuses);
    List<SupportTicket> findTop20ByOrderByCreatedAtDesc();
}
