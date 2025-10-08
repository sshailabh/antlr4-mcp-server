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
    private String grammarType;
    private Integer lexerRules;
    private Integer parserRules;

    @Builder.Default
    private List<GrammarError> errors = new ArrayList<>();

    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    public ValidationResult() {
    }

    public ValidationResult(boolean success, String grammarName, String grammarType,
                          Integer lexerRules, Integer parserRules,
                          List<GrammarError> errors, List<String> warnings) {
        this.success = success;
        this.grammarName = grammarName;
        this.grammarType = grammarType;
        this.lexerRules = lexerRules;
        this.parserRules = parserRules;
        this.errors = errors != null ? errors : new ArrayList<>();
        this.warnings = warnings != null ? warnings : new ArrayList<>();
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
