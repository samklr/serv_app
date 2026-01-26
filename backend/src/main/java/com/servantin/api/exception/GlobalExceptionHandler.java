package com.servantin.api.exception;

import com.servantin.api.dto.common.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<ApiError.FieldError> fieldErrors = ex.getBindingResult().getAllErrors().stream()
                .map(error -> {
                    String fieldName = error instanceof FieldError fe ? fe.getField() : error.getObjectName();
                    Object rejectedValue = error instanceof FieldError fe ? fe.getRejectedValue() : null;
                    return ApiError.FieldError.builder()
                            .field(fieldName)
                            .message(error.getDefaultMessage())
                            .rejectedValue(rejectedValue)
                            .build();
                })
                .collect(Collectors.toList());

        ApiError apiError = ApiError.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message("Validation failed")
                .errorCode("VALIDATION_ERROR")
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .errors(fieldErrors)
                .build();

        return ResponseEntity.badRequest().body(apiError);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request) {

        ApiError apiError = ApiError.builder()
                .status(HttpStatus.UNAUTHORIZED.value())
                .message("Invalid email or password")
                .errorCode("INVALID_CREDENTIALS")
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(apiError);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {

        ApiError apiError = ApiError.builder()
                .status(HttpStatus.FORBIDDEN.value())
                .message("Access denied")
                .errorCode("ACCESS_DENIED")
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(apiError);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleMaxUploadSize(
            MaxUploadSizeExceededException ex, HttpServletRequest request) {

        ApiError apiError = ApiError.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message("File size exceeds the maximum allowed limit")
                .errorCode("FILE_TOO_LARGE")
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.badRequest().body(apiError);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApiException(
            ApiException ex, HttpServletRequest request) {

        log.warn("API exception: {} - {}", ex.getClass().getSimpleName(), ex.getMessage());

        ApiError apiError = ApiError.builder()
                .status(ex.getStatus().value())
                .message(ex.getMessage())
                .errorCode(ex.getErrorCode())
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(ex.getStatus()).body(apiError);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiError> handleRuntimeException(
            RuntimeException ex, HttpServletRequest request) {

        log.error("Unhandled runtime exception: ", ex);

        // For backwards compatibility, try to map common message patterns
        // This should be removed once all services use custom exceptions
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = ex.getMessage();
        String errorCode = "INTERNAL_ERROR";

        if (message != null) {
            if (message.toLowerCase().contains("not found")) {
                status = HttpStatus.NOT_FOUND;
                errorCode = "RESOURCE_NOT_FOUND";
            } else if (message.toLowerCase().contains("already") || message.toLowerCase().contains("duplicate")) {
                status = HttpStatus.CONFLICT;
                errorCode = "CONFLICT";
            } else if (message.toLowerCase().contains("access denied") || message.toLowerCase().contains("permission")) {
                status = HttpStatus.FORBIDDEN;
                errorCode = "FORBIDDEN";
            } else if (message.toLowerCase().contains("invalid") || message.toLowerCase().contains("expired")) {
                status = HttpStatus.BAD_REQUEST;
                errorCode = "BAD_REQUEST";
            }
        }

        ApiError apiError = ApiError.builder()
                .status(status.value())
                .message(message != null ? message : "An unexpected error occurred")
                .errorCode(errorCode)
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(status).body(apiError);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(
            Exception ex, HttpServletRequest request) {

        log.error("Unexpected exception: ", ex);

        ApiError apiError = ApiError.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message("An unexpected error occurred")
                .errorCode("INTERNAL_ERROR")
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiError);
    }
}
