package com.github.sshailabh.antlr4mcp.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GrammarError {
    private String type;
    private Integer line;
    private Integer column;
    private String message;
    private String suggestedFix;
    private String ruleName;

    public GrammarError() {
    }

    public GrammarError(String type, Integer line, Integer column, String message,
                       String suggestedFix, String ruleName) {
        this.type = type;
        this.line = line;
        this.column = column;
        this.message = message;
        this.suggestedFix = suggestedFix;
        this.ruleName = ruleName;
    }
}
