package com.github.sshailabh.antlr4mcp.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ParseResult {
    private boolean success;
    private String tokens;
    private String parseTree;
    private String svg;

    @Builder.Default
    private List<GrammarError> errors = new ArrayList<>();

    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    public ParseResult() {
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
    }

    public ParseResult(boolean success, String tokens, String parseTree, String svg,
                      List<GrammarError> errors, List<String> warnings) {
        this.success = success;
        this.tokens = tokens;
        this.parseTree = parseTree;
        this.svg = svg;
        this.errors = errors != null ? errors : new ArrayList<>();
        this.warnings = warnings != null ? warnings : new ArrayList<>();
    }
}
