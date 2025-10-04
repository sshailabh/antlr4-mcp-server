package com.github.sshailabh.antlr4mcp.service;

import com.github.sshailabh.antlr4mcp.model.GrammarError;
import com.github.sshailabh.antlr4mcp.model.VisualizationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TreeVisualizer {

    public VisualizationResult visualize(String grammarText, String ruleName, String format) {
        log.info("Visualizing rule: {} in format: {}", ruleName, format);

        try {
            log.debug("Tree visualization not fully implemented in M1 basic version");

            return VisualizationResult.builder()
                .success(false)
                .errors(java.util.List.of(GrammarError.builder()
                    .type("not_implemented")
                    .message("Tree visualization not implemented in M1. This feature will be available in M2.")
                    .build()))
                .build();

        } catch (Exception e) {
            log.error("Visualization failed", e);
            return VisualizationResult.builder()
                .success(false)
                .errors(java.util.List.of(GrammarError.builder()
                    .type("internal_error")
                    .message("Visualization error: " + e.getMessage())
                    .build()))
                .build();
        }
    }
}
