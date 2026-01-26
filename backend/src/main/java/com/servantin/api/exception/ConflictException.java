package com.servantin.api.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when there is a conflict with the current state of a resource.
 * Returns HTTP 409 Conflict.
 */
public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(message, HttpStatus.CONFLICT, "CONFLICT");
    }

    public ConflictException(String message, String errorCode) {
        super(message, HttpStatus.CONFLICT, errorCode);
    }

    public static ConflictException emailAlreadyExists(String email) {
        return new ConflictException("Email already registered: " + email, "EMAIL_EXISTS");
    }

    public static ConflictException resourceAlreadyExists(String resourceType) {
        return new ConflictException(resourceType + " already exists", "RESOURCE_EXISTS");
    }
}
