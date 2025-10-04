package com.github.sshailabh.antlr4mcp.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidationResult {
    private boolean success;
    private String grammarName;
    private Integer lexerRules;
    private Integer parserRules;

    @Builder.Default
    private List<GrammarError> errors = new ArrayList<>();

    public ValidationResult() {
    }

    public ValidationResult(boolean success, String grammarName, Integer lexerRules,
                          Integer parserRules, List<GrammarError> errors) {
        this.success = success;
        this.grammarName = grammarName;
        this.lexerRules = lexerRules;
        this.parserRules = parserRules;
        this.errors = errors != null ? errors : new ArrayList<>();
    }

    public static ValidationResult error(String message) {
        return ValidationResult.builder()
            .success(false)
            .errors(List.of(GrammarError.builder()
                .type("validation_error")
                .message(message)
                .build()))
            .build();
    }

    public static ValidationResult success(String grammarName, int lexerRules, int parserRules) {
        return ValidationResult.builder()
            .success(true)
            .grammarName(grammarName)
            .lexerRules(lexerRules)
            .parserRules(parserRules)
            .errors(new ArrayList<>())
            .build();
    }
}
