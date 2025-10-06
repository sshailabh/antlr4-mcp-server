package com.github.sshailabh.antlr4mcp.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single event in a parse trace.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TraceEvent {
    private int step;
    private String type; // ENTER_RULE, EXIT_RULE, CONSUME_TOKEN, DECISION, ERROR
    private String ruleName;
    private String tokenText;
    private Integer tokenType;
    private Integer line;
    private Integer column;
    private int depth; // Nesting depth
    private String message; // Additional information about the event
    private Long timestamp; // Optional timing information
}