package com.github.sshailabh.antlr4mcp.service;

import com.github.sshailabh.antlr4mcp.model.ErrorLocation;
import com.github.sshailabh.antlr4mcp.model.ErrorResponse;
import com.github.sshailabh.antlr4mcp.model.ErrorType;
import com.github.sshailabh.antlr4mcp.model.ParseTimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.tool.ANTLRMessage;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeoutException;

/**
 * Transforms exceptions and ANTLR error messages into structured ErrorResponse objects
 * optimized for LLM consumption.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ErrorTransformer {

    private final ErrorSuggestions errorSuggestions;

    /**
     * Transform any exception to structured ErrorResponse
     */
    public ErrorResponse transform(Exception e, String context) {
        log.debug("Transforming exception: {}", e.getClass().getSimpleName());

        if (e instanceof ParseTimeoutException) {
            return transformParseTimeoutError((ParseTimeoutException) e);
        } else if (e instanceof TimeoutException) {
            return transformTimeoutError((TimeoutException) e);
        } else if (e instanceof RecognitionException) {
            return transformRecognitionError((RecognitionException) e);
        } else if (e instanceof IllegalArgumentException) {
            return transformInvalidInputError(e, context);
        } else {
            return transformGenericError(e, context);
        }
    }

    /**
     * Transform ANTLR grammar error message
     */
    public ErrorResponse transformANTLRMessage(ANTLRMessage msg) {
        org.antlr.v4.tool.ErrorType antlrErrorType = msg.getErrorType();
        ErrorType ourErrorType = mapANTLRErrorType(antlrErrorType);

        ErrorLocation location = extractLocationFromMessage(msg);
        String message = msg.toString();

        return buildErrorResponse(ourErrorType, message, location, null);
    }

    /**
     * Transform ParseTimeoutException (custom timeout exception with detailed message)
     */
    private ErrorResponse transformParseTimeoutError(ParseTimeoutException e) {
        ErrorType errorType = ErrorType.PARSE_TIMEOUT;

        return buildErrorResponse(
                errorType,
                e.getMessage(),
                null,
                null
        );
    }

    /**
     * Transform timeout exception
     */
    private ErrorResponse transformTimeoutError(TimeoutException e) {
        ErrorType errorType = ErrorType.PARSE_TIMEOUT;

        return buildErrorResponse(
                errorType,
                "Parsing exceeded timeout - possible infinite loop or very complex grammar",
                null,
                "Check for left-recursive rules or reduce grammar complexity"
        );
    }

    /**
     * Transform ANTLR recognition exception (parsing error)
     */
    private ErrorResponse transformRecognitionError(RecognitionException e) {
        ErrorType errorType = ErrorType.PARSE_ERROR;

        ErrorLocation location = ErrorLocation.builder()
                .line(e.getOffendingToken() != null ? e.getOffendingToken().getLine() : null)
                .column(e.getOffendingToken() != null ? e.getOffendingToken().getCharPositionInLine() : null)
                .build();

        String message = e.getMessage() != null ? e.getMessage() : "Parse error";

        return buildErrorResponse(errorType, message, location, null);
    }

    /**
     * Transform invalid input error
     */
    private ErrorResponse transformInvalidInputError(Exception e, String context) {
        ErrorType errorType = ErrorType.INVALID_INPUT;

        return buildErrorResponse(
                errorType,
                e.getMessage() != null ? e.getMessage() : "Invalid input provided",
                null,
                context
        );
    }

    /**
     * Transform generic error
     */
    private ErrorResponse transformGenericError(Exception e, String context) {
        ErrorType errorType = ErrorType.INTERNAL_ERROR;

        String message = e.getMessage() != null ? e.getMessage() : "Internal error: " + e.getClass().getSimpleName();

        return buildErrorResponse(errorType, message, null, context);
    }

    /**
     * Build complete error response with suggestions and documentation
     */
    private ErrorResponse buildErrorResponse(
            ErrorType errorType,
            String message,
            ErrorLocation location,
            String context
    ) {
        return ErrorResponse.builder()
                .success(false)
                .error(ErrorResponse.ErrorDetail.builder()
                        .type(errorType.getCode())
                        .category(errorSuggestions.getCategory(errorType))
                        .message(message)
                        .location(location)
                        .suggestion(errorSuggestions.getSuggestion(errorType, context))
                        .example(errorSuggestions.getExample(errorType))
                        .documentation(errorSuggestions.getDocumentationUrl(errorType))
                        .severity("error")
                        .context(context)
                        .build())
                .build();
    }

    /**
     * Map ANTLR ErrorType to our ErrorType
     */
    private ErrorType mapANTLRErrorType(org.antlr.v4.tool.ErrorType antlrErrorType) {
        if (antlrErrorType == null) {
            return ErrorType.GRAMMAR_LOAD_ERROR;
        }

        // Map ANTLR error types to our error types
        String errorName = antlrErrorType.name();

        if (errorName.contains("LEFT_RECURSION")) {
            return ErrorType.LEFT_RECURSION;
        } else if (errorName.contains("SYNTAX")) {
            return ErrorType.SYNTAX_ERROR;
        } else if (errorName.contains("UNDEFINED") || errorName.contains("UNKNOWN")) {
            return ErrorType.UNDEFINED_RULE;
        } else if (errorName.contains("AMBIG")) {
            return ErrorType.AMBIGUITY;
        } else if (errorName.contains("TOKEN")) {
            return ErrorType.TOKEN_CONFLICT;
        } else if (errorName.contains("IMPORT")) {
            return ErrorType.IMPORT_ERROR;
        } else {
            return ErrorType.GRAMMAR_LOAD_ERROR;
        }
    }

    /**
     * Extract location information from ANTLR message
     */
    private ErrorLocation extractLocationFromMessage(ANTLRMessage msg) {
        if (msg.line > 0) {
            return ErrorLocation.builder()
                    .line(msg.line)
                    .column(msg.charPosition)
                    .grammarFile(msg.fileName)
                    .build();
        }
        return null;
    }
}
