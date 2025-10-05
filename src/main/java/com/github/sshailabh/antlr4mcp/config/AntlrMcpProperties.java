package com.github.sshailabh.antlr4mcp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for ANTLR4 MCP Server Phase 2
 */
@Configuration
@ConfigurationProperties(prefix = "antlr.mcp")
@Data
public class AntlrMcpProperties {

    private String version = "2.0";
    private CacheProperties cache = new CacheProperties();
    private ResourcesProperties resources = new ResourcesProperties();
    private PerformanceProperties performance = new PerformanceProperties();
    private FeaturesProperties features = new FeaturesProperties();
    private SecurityProperties security = new SecurityProperties();

    @Data
    public static class CacheProperties {
        private boolean enabled = true;
        private int maxSize = 1000;
        private long ttlSeconds = 360;
    }

    @Data
    public static class ResourcesProperties {
        private boolean enabled = true;
        private List<String> allowedPaths = new ArrayList<>();
        private boolean autoDiscovery = true;
    }

    @Data
    public static class PerformanceProperties {
        private int maxGrammarSizeMb = 10;
        private int parseTimeoutSeconds = 30;
        private int maxConcurrentRequests = 50;
        private long asyncThresholdMs = 5000;
    }

    @Data
    public static class FeaturesProperties {
        private boolean importResolution = true;
        private boolean visualization = true;
        private boolean testGeneration = true;
    }

    @Data
    public static class SecurityProperties {
        private boolean validateInputs = true;
        private boolean sanitizePaths = true;
        private int maxImportDepth = 10;
    }
}
