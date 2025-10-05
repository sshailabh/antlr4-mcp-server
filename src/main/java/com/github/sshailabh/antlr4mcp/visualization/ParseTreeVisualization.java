package com.github.sshailabh.antlr4mcp.visualization;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Parse tree visualization result with multiple formats
 */
@Data
@Builder
public class ParseTreeVisualization {
    private String asciiFormat;
    private Map<String, Object> jsonFormat;
    private String dotFormat;
    private String lispFormat;
    private String svgFormat;
    private String htmlFormat;
    private int nodeCount;
    private int maxDepth;
}
