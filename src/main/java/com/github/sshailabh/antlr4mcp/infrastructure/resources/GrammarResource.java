package com.github.sshailabh.antlr4mcp.infrastructure.resources;

import lombok.Builder;
import lombok.Data;

/**
 * Represents a grammar resource available through MCP
 */
@Data
@Builder
public class GrammarResource {
    private String uri;
    private String name;
    private String path;
    private String mimeType;
    private String description;
}
