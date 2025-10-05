package com.github.sshailabh.antlr4mcp.visualization;

import lombok.Builder;
import lombok.Data;

/**
 * Options for parse tree visualization
 */
@Data
@Builder
public class VisualizationOptions {
    @Builder.Default
    private boolean syntaxHighlighting = true;

    @Builder.Default
    private boolean showText = true;

    @Builder.Default
    private boolean showPosition = false;

    @Builder.Default
    private boolean showRuleIndices = false;

    @Builder.Default
    private int maxDepth = -1; // -1 = unlimited

    public static VisualizationOptions defaults() {
        return VisualizationOptions.builder().build();
    }
}
