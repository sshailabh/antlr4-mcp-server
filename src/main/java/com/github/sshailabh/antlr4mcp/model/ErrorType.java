package com.github.sshailabh.antlr4mcp.model;

/**
 * Enumeration of error types for structured error responses.
 * Each error type has a code and description for LLM consumption.
 */
public enum ErrorType {
    LEFT_RECURSION("left_recursion", "Left-recursive rule detected"),
    SYNTAX_ERROR("syntax_error", "Grammar syntax error"),
    UNDEFINED_RULE("undefined_rule", "Rule not defined"),
    TOKEN_CONFLICT("token_conflict", "Lexer token conflict"),
    AMBIGUITY("ambiguity", "Grammar ambiguity detected"),
    SEMANTIC_ERROR("semantic_error", "Semantic validation failed"),
    PARSE_TIMEOUT("parse_timeout", "Parsing timeout exceeded"),
    PARSE_ERROR("parse_error", "Input parsing failed"),
    GRAMMAR_LOAD_ERROR("grammar_load_error", "Failed to load grammar"),
    IMPORT_ERROR("import_error", "Grammar import error"),
    INVALID_INPUT("invalid_input", "Invalid input provided"),
    INTERNAL_ERROR("internal_error", "Internal processing error");

    private final String code;
    private final String description;

    ErrorType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ErrorType fromCode(String code) {
        for (ErrorType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return INTERNAL_ERROR;
    }
}
