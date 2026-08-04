package com.example.demo.repository;

import com.example.demo.model.TicketAttachment;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketAttachmentRepository extends JpaRepository<TicketAttachment, Long> {
}
