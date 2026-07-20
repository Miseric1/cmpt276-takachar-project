package com.example.demo.exception;

/**
 * Thrown when creating or updating an entity would violate a uniqueness rule
 * (for example a duplicate FAQ question or a duplicate article title). Mapped to
 * HTTP 409 by {@link GlobalExceptionHandler}.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
