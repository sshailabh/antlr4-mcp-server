package com.github.sshailabh.antlr4mcp.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Location information for errors in grammar or input files.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorLocation {
    /**
     * Line number (1-indexed)
     */
    private Integer line;

    /**
     * Column number (0-indexed, following ANTLR convention)
     */
    private Integer column;

    /**
     * Rule name where error occurred (if applicable)
     */
    private String ruleName;

    /**
     * Grammar file name
     */
    private String grammarFile;

    /**
     * Start position in input (character offset)
     */
    private Integer startPos;

    /**
     * End position in input (character offset)
     */
    private Integer endPos;

    public static ErrorLocation of(int line, int column) {
        return ErrorLocation.builder()
                .line(line)
                .column(column)
                .build();
    }

    public static ErrorLocation of(int line, int column, String ruleName) {
        return ErrorLocation.builder()
                .line(line)
                .column(column)
                .ruleName(ruleName)
                .build();
    }
}
