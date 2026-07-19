package com.example.demo.exception;

/**
 * Thrown when an operation is not allowed for an entity's current state, for
 * example publishing an article that is archived, or an invalid publication
 * transition. Mapped to HTTP 409 by {@link GlobalExceptionHandler}.
 */
public class InvalidStateException extends RuntimeException {

    public InvalidStateException(String message) {
        super(message);
    }
}
