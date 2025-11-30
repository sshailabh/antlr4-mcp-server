package com.github.sshailabh.antlr4mcp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for ANTLR4 MCP Server.
 */
@Configuration
@ConfigurationProperties(prefix = "antlr")
@Data
public class AntlrMcpProperties {

    /** Maximum grammar size in MB */
    private int maxGrammarSizeMb = 10;
    
    /** Maximum input size in MB for parsing */
    private int maxInputSizeMb = 1;
    
    /** Compilation timeout in seconds */
    private int compilationTimeoutSeconds = 30;
    
    /** Security settings */
    private Security security = new Security();

    @Data
    public static class Security {
        /** Enable security validation */
        private boolean enabled = true;
        
        /** Validation sub-settings */
        private Validation validation = new Validation();
        
        /** Resource limit settings */
        private ResourceLimits resourceLimits = new ResourceLimits();
    }

    @Data
    public static class Validation {
        private boolean enabled = true;
    }

    @Data
    public static class ResourceLimits {
        private boolean enabled = true;
    }
}
