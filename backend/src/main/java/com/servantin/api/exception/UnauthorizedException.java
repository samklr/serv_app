package com.servantin.api.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when authentication is required but not provided or invalid.
 * Returns HTTP 401 Unauthorized.
 */
public class UnauthorizedException extends ApiException {

    public UnauthorizedException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
    }

    public UnauthorizedException(String message, String errorCode) {
        super(message, HttpStatus.UNAUTHORIZED, errorCode);
    }

    public static UnauthorizedException invalidCredentials() {
        return new UnauthorizedException("Invalid email or password", "INVALID_CREDENTIALS");
    }

    public static UnauthorizedException emailNotVerified() {
        return new UnauthorizedException("Please verify your email before logging in", "EMAIL_NOT_VERIFIED");
    }
}
