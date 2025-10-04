package com.github.sshailabh.antlr4mcp.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VisualizationResult {
    private boolean success;
    private String format;
    private String svgContent;
    private String dotContent;
    private String description;

    @Builder.Default
    private List<GrammarError> errors = new ArrayList<>();
}
