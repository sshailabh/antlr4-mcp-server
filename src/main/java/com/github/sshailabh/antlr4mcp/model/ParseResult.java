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

    public ParseResult() {
        this.errors = new ArrayList<>();
    }

    public ParseResult(boolean success, String tokens, String parseTree, String svg, List<GrammarError> errors) {
        this.success = success;
        this.tokens = tokens;
        this.parseTree = parseTree;
        this.svg = svg;
        this.errors = errors != null ? errors : new ArrayList<>();
    }
}
