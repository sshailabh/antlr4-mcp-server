package com.github.sshailabh.antlr4mcp.visualization;

import lombok.Builder;
import lombok.Data;

/**
 * ATN visualization result containing multiple output formats
 */
@Data
@Builder
public class AtnVisualization {
    private String ruleName;
    private int stateCount;
    private int transitionCount;
    private String dotFormat;
    private String svgFormat;
    private String mermaidFormat;
    private AtnGraph graph;
}
