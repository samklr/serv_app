package com.servantin.api.dto.common;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class ApiError {
    private int status;
    private String message;
    private String errorCode;
    private String path;
    private Instant timestamp;
    private List<FieldError> errors;

    @Data
    @Builder
    public static class FieldError {
        private String field;
        private String message;
        private Object rejectedValue;
    }
}
