package com.github.sshailabh.antlr4mcp.infrastructure.imports;

import lombok.Builder;
import lombok.Data;

import java.nio.file.Path;

/**
 * Represents an imported grammar with its metadata
 */
@Data
@Builder
public class ImportedGrammar {
    private String name;
    private String content;
    private Path path;
    private String uri;
}
