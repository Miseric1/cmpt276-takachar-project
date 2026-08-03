package com.example.demo.repository;

import com.example.demo.model.SupportTicket;
import com.example.demo.model.TicketPriority;
import com.example.demo.model.TicketStatus;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public final class TicketSpecifications {
    private TicketSpecifications() {
    }

    public static Specification<SupportTicket> ownedBy(String customerEmail) {
        return (root, query, cb) -> blank(customerEmail) ? null
                : cb.equal(cb.lower(root.get("customerEmail")), customerEmail.trim().toLowerCase());
    }

    public static Specification<SupportTicket> statusIs(TicketStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<SupportTicket> priorityIs(TicketPriority priority) {
        return (root, query, cb) -> priority == null ? null : cb.equal(root.get("priority"), priority);
    }

    public static Specification<SupportTicket> departmentIs(String department) {
        return (root, query, cb) -> blank(department) ? null
                : cb.equal(cb.lower(root.get("department")), department.trim().toLowerCase());
    }

    public static Specification<SupportTicket> projectIs(String project) {
        return (root, query, cb) -> blank(project) ? null
                : cb.equal(cb.lower(root.get("project")), project.trim().toLowerCase());
    }

    public static Specification<SupportTicket> spocIs(String spocEmail) {
        return (root, query, cb) -> blank(spocEmail) ? null
                : cb.equal(cb.lower(root.get("spocEmail")), spocEmail.trim().toLowerCase());
    }

    public static Specification<SupportTicket> createdBetween(LocalDateTime from, LocalDateTime to) {
        return (root, query, cb) -> {
            if (from == null && to == null) return null;
            if (from == null) return cb.lessThanOrEqualTo(root.get("createdAt"), to);
            if (to == null) return cb.greaterThanOrEqualTo(root.get("createdAt"), from);
            return cb.between(root.get("createdAt"), from, to);
        };
    }

    public static Specification<SupportTicket> keyword(String keyword) {
        return (root, query, cb) -> {
            if (blank(keyword)) return null;
            String like = "%" + keyword.trim().toLowerCase() + "%";
            Predicate reference = cb.like(cb.lower(root.get("referenceNumber")), like);
            Predicate subject = cb.like(cb.lower(root.get("subject")), like);
            Predicate description = cb.like(cb.lower(root.get("description")), like);
            return cb.or(reference, subject, description);
        };
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
