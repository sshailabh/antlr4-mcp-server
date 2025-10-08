package com.github.sshailabh.antlr4mcp.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Structured error response optimized for LLM consumption.
 * Includes fix suggestions, examples, and documentation links.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    /**
     * Always false for error responses
     */
    @Builder.Default
    private boolean success = false;

    /**
     * Error details
     */
    private ErrorDetail error;

    /**
     * Detailed error information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorDetail {
        /**
         * Error type code (e.g., "left_recursion", "syntax_error")
         */
        private String type;

        /**
         * Error category ("grammar", "parsing", "compilation", "internal")
         */
        private String category;

        /**
         * Human-readable error message
         */
        private String message;

        /**
         * Location where error occurred
         */
        private ErrorLocation location;

        /**
         * Fix suggestion for this error
         */
        private String suggestion;

        /**
         * Code example showing the fix
         */
        private String example;

        /**
         * Documentation URL for more information
         */
        private String documentation;

        /**
         * Severity level ("error", "warning", "info")
         */
        @Builder.Default
        private String severity = "error";

        /**
         * Additional context information
         */
        private String context;
    }

    /**
     * Create an error response with minimal information
     */
    public static ErrorResponse of(String type, String message) {
        return ErrorResponse.builder()
                .success(false)
                .error(ErrorDetail.builder()
                        .type(type)
                        .message(message)
                        .build())
                .build();
    }

    /**
     * Create an error response from ErrorType
     */
    public static ErrorResponse of(ErrorType errorType, String message) {
        return ErrorResponse.builder()
                .success(false)
                .error(ErrorDetail.builder()
                        .type(errorType.getCode())
                        .message(message)
                        .build())
                .build();
    }

    /**
     * Create an error response with location
     */
    public static ErrorResponse of(ErrorType errorType, String message, ErrorLocation location) {
        return ErrorResponse.builder()
                .success(false)
                .error(ErrorDetail.builder()
                        .type(errorType.getCode())
                        .message(message)
                        .location(location)
                        .build())
                .build();
    }
}
