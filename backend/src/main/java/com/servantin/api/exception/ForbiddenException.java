package com.servantin.api.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when the user does not have permission to access a resource.
 * Returns HTTP 403 Forbidden.
 */
public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        super(message, HttpStatus.FORBIDDEN, "FORBIDDEN");
    }

    public ForbiddenException(String message, String errorCode) {
        super(message, HttpStatus.FORBIDDEN, errorCode);
    }

    public static ForbiddenException accessDenied() {
        return new ForbiddenException("Access denied", "ACCESS_DENIED");
    }

    public static ForbiddenException insufficientPermissions(String requiredRole) {
        return new ForbiddenException("Insufficient permissions. Required role: " + requiredRole, "INSUFFICIENT_PERMISSIONS");
    }

    public static ForbiddenException notResourceOwner() {
        return new ForbiddenException("You do not have permission to access this resource", "NOT_OWNER");
    }
}
