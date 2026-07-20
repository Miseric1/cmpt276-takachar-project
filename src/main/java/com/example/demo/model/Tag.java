package com.example.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * A free-form label attached to FAQs and Knowledge Base articles for searching
 * and filtering. Tags are stored once and shared through many-to-many joins, so
 * the same "printer" or "billing" tag is reused rather than duplicated.
 */
@Entity
@Table(name = "tags", indexes = @Index(name = "idx_tag_name", columnList = "name", unique = true))
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Tag name is required")
    @Size(max = 60, message = "Tag name must be at most 60 characters")
    @Column(nullable = false, unique = true, length = 60)
    private String name;

    public Tag() {
    }

    public Tag(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
