package com.github.sshailabh.antlr4mcp.model;

/**
 * Exception thrown when parsing exceeds the configured timeout.
 *
 * This exception indicates that a parsing operation took longer than the
 * allowed timeout period, suggesting either:
 * - An extremely large input
 * - A pathological grammar that causes exponential parsing time
 * - An infinite loop in the parsing logic
 */
public class ParseTimeoutException extends RuntimeException {

    public ParseTimeoutException(String message) {
        super(message);
    }

    public ParseTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
