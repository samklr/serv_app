package com.servantin.api.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a requested resource is not found.
 * Returns HTTP 404 Not Found.
 */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }

    public ResourceNotFoundException(String resourceType, String identifier) {
        super(resourceType + " not found: " + identifier, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }

    public ResourceNotFoundException(String resourceType, Long id) {
        super(resourceType + " not found with id: " + id, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }

    public ResourceNotFoundException(String resourceType, java.util.UUID id) {
        super(resourceType + " not found with id: " + id, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }
}
