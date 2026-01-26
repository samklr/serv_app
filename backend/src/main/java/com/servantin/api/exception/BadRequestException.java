package com.servantin.api.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when the request is malformed or contains invalid data.
 * Returns HTTP 400 Bad Request.
 */
public class BadRequestException extends ApiException {

    public BadRequestException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "BAD_REQUEST");
    }

    public BadRequestException(String message, String errorCode) {
        super(message, HttpStatus.BAD_REQUEST, errorCode);
    }

    public static BadRequestException invalidToken(String tokenType) {
        return new BadRequestException("Invalid or expired " + tokenType + " token", "INVALID_TOKEN");
    }

    public static BadRequestException invalidOperation(String message) {
        return new BadRequestException(message, "INVALID_OPERATION");
    }

    public static BadRequestException invalidFileType(String allowedTypes) {
        return new BadRequestException("Invalid file type. Allowed: " + allowedTypes, "INVALID_FILE_TYPE");
    }

    public static BadRequestException fileTooLarge(long maxSize) {
        return new BadRequestException("File size exceeds maximum allowed: " + (maxSize / 1024 / 1024) + "MB", "FILE_TOO_LARGE");
    }
}
