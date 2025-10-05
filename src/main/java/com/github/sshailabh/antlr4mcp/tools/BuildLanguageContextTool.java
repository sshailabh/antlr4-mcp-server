package com.github.sshailabh.antlr4mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sshailabh.antlr4mcp.infrastructure.imports.ImportResolver;
import com.github.sshailabh.antlr4mcp.infrastructure.imports.ImportedGrammar;
import com.github.sshailabh.antlr4mcp.model.ValidationResult;
import com.github.sshailabh.antlr4mcp.service.GrammarCompiler;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MCP Tool for building comprehensive language context from ANTLR4 grammars.
 * Analyzes grammar structure, dependencies, and usage patterns.
 */
@Slf4j
public class BuildLanguageContextTool {

    private final GrammarCompiler grammarCompiler;
    private final ImportResolver importResolver;
    private final ObjectMapper objectMapper;

    private static final Pattern RULE_PATTERN = Pattern.compile(
        "^([a-zA-Z][a-zA-Z0-9_]*)\\s*:", Pattern.MULTILINE
    );

    private static final Pattern RULE_REFERENCE_PATTERN = Pattern.compile(
        "\\b([a-z][a-zA-Z0-9_]*)\\b"
    );

    public BuildLanguageContextTool(GrammarCompiler grammarCompiler,
                                   ImportResolver importResolver,
                                   ObjectMapper objectMapper) {
        this.grammarCompiler = grammarCompiler;
        this.importResolver = importResolver;
        this.objectMapper = objectMapper;
    }

    public McpSchema.Tool toTool() {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> grammarText = new HashMap<>();
        grammarText.put("type", "string");
        grammarText.put("description", "The main ANTLR4 grammar text to analyze");
        properties.put("grammarText", grammarText);

        Map<String, Object> grammarPath = new HashMap<>();
        grammarPath.put("type", "string");
        grammarPath.put("description", "Optional path to grammar file (for resolving imports)");
        properties.put("grammarPath", grammarPath);

        Map<String, Object> includeImports = new HashMap<>();
        includeImports.put("type", "boolean");
        includeImports.put("description", "Whether to analyze imported grammars (default: true)");
        includeImports.put("default", true);
        properties.put("includeImports", includeImports);

        McpSchema.JsonSchema schema = new McpSchema.JsonSchema(
            "object",
            properties,
            List.of("grammarText"),
            null,
            null,
            null
        );

        return McpSchema.Tool.builder()
            .name("build_language_context")
            .description("Analyze ANTLR4 grammar to build comprehensive language context including " +
                "grammar structure, rule dependencies, import relationships, and usage patterns. " +
                "Supports multi-file grammar projects with import resolution.")
            .inputSchema(schema)
            .build();
    }

    public McpSchema.CallToolResult execute(McpSyncServerExchange exchange, McpSchema.CallToolRequest request) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> arguments = (Map<String, Object>) request.arguments();

            String grammarText = (String) arguments.get("grammarText");
            String grammarPath = (String) arguments.get("grammarPath");
            Boolean includeImportsObj = (Boolean) arguments.get("includeImports");
            boolean includeImports = includeImportsObj != null ? includeImportsObj : true;

            log.info("Building language context, includeImports: {}", includeImports);

            // Validate main grammar
            ValidationResult validation = grammarCompiler.validate(grammarText);
            if (!validation.isSuccess()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "Grammar validation failed");
                error.put("errors", validation.getErrors());
                String jsonResult = objectMapper.writeValueAsString(error);
                return new McpSchema.CallToolResult(jsonResult, true);
            }

            // Extract grammar name
            String grammarName = extractGrammarName(grammarText);

            // Analyze main grammar
            GrammarAnalysis mainAnalysis = analyzeGrammar(grammarName, grammarText);

            // Resolve imports if requested
            Map<String, GrammarAnalysis> importAnalyses = new HashMap<>();
            if (includeImports && grammarPath != null && importResolver.hasImports(grammarText)) {
                try {
                    Path path = Paths.get(grammarPath);
                    Map<String, ImportedGrammar> imports = importResolver.resolveImports(grammarText, path);

                    for (Map.Entry<String, ImportedGrammar> entry : imports.entrySet()) {
                        GrammarAnalysis analysis = analyzeGrammar(
                            entry.getKey(),
                            entry.getValue().getContent()
                        );
                        importAnalyses.put(entry.getKey(), analysis);
                    }
                } catch (Exception e) {
                    log.warn("Failed to resolve imports: {}", e.getMessage());
                }
            }

            // Build context
            Map<String, Object> context = buildContext(grammarName, mainAnalysis, importAnalyses);
            context.put("success", true);

            String jsonResult = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(context);

            return new McpSchema.CallToolResult(jsonResult, false);

        } catch (Exception e) {
            log.error("Failed to build language context", e);
            try {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", e.getMessage());
                String jsonResult = objectMapper.writeValueAsString(error);
                return new McpSchema.CallToolResult(jsonResult, true);
            } catch (Exception jsonError) {
                return new McpSchema.CallToolResult(
                    "{\"success\":false,\"error\":\"" + e.getMessage() + "\"}",
                    true
                );
            }
        }
    }

    private GrammarAnalysis analyzeGrammar(String name, String content) {
        GrammarAnalysis analysis = new GrammarAnalysis();
        analysis.name = name;

        // Extract rules
        Matcher ruleMatcher = RULE_PATTERN.matcher(content);
        while (ruleMatcher.find()) {
            String ruleName = ruleMatcher.group(1);
            if (Character.isUpperCase(ruleName.charAt(0))) {
                analysis.lexerRules.add(ruleName);
            } else {
                analysis.parserRules.add(ruleName);
            }
        }

        // Extract rule dependencies
        for (String rule : analysis.parserRules) {
            Set<String> dependencies = extractRuleDependencies(content, rule);
            analysis.ruleDependencies.put(rule, dependencies);
        }

        // Extract imports
        analysis.imports.addAll(importResolver.extractImports(content));

        return analysis;
    }

    private Set<String> extractRuleDependencies(String content, String ruleName) {
        Set<String> dependencies = new HashSet<>();

        // Find the rule definition
        Pattern ruleDefPattern = Pattern.compile(
            "^" + Pattern.quote(ruleName) + "\\s*:([^;]+);",
            Pattern.MULTILINE | Pattern.DOTALL
        );

        Matcher defMatcher = ruleDefPattern.matcher(content);
        if (defMatcher.find()) {
            String ruleBody = defMatcher.group(1);

            // Find all rule references
            Matcher refMatcher = RULE_REFERENCE_PATTERN.matcher(ruleBody);
            while (refMatcher.find()) {
                String ref = refMatcher.group(1);
                if (!ref.equals(ruleName)) { // Exclude self-references
                    dependencies.add(ref);
                }
            }
        }

        return dependencies;
    }

    private Map<String, Object> buildContext(String grammarName,
                                             GrammarAnalysis mainAnalysis,
                                             Map<String, GrammarAnalysis> importAnalyses) {
        Map<String, Object> context = new HashMap<>();

        // Grammar info
        Map<String, Object> grammarInfo = new HashMap<>();
        grammarInfo.put("name", grammarName);
        grammarInfo.put("type", determineGrammarType(mainAnalysis));
        grammarInfo.put("lexerRuleCount", mainAnalysis.lexerRules.size());
        grammarInfo.put("parserRuleCount", mainAnalysis.parserRules.size());
        grammarInfo.put("totalRules", mainAnalysis.lexerRules.size() + mainAnalysis.parserRules.size());
        grammarInfo.put("hasImports", !mainAnalysis.imports.isEmpty());
        context.put("grammar", grammarInfo);

        // Rules
        Map<String, Object> rules = new HashMap<>();
        rules.put("lexer", mainAnalysis.lexerRules);
        rules.put("parser", mainAnalysis.parserRules);
        context.put("rules", rules);

        // Rule dependencies
        context.put("ruleDependencies", mainAnalysis.ruleDependencies);

        // Imports
        if (!mainAnalysis.imports.isEmpty()) {
            Map<String, Object> imports = new HashMap<>();
            imports.put("direct", mainAnalysis.imports);

            if (!importAnalyses.isEmpty()) {
                Map<String, Object> resolved = new HashMap<>();
                for (Map.Entry<String, GrammarAnalysis> entry : importAnalyses.entrySet()) {
                    Map<String, Object> importInfo = new HashMap<>();
                    importInfo.put("lexerRules", entry.getValue().lexerRules.size());
                    importInfo.put("parserRules", entry.getValue().parserRules.size());
                    importInfo.put("rules", entry.getValue().parserRules);
                    resolved.put(entry.getKey(), importInfo);
                }
                imports.put("resolved", resolved);
            }

            context.put("imports", imports);
        }

        // Analysis summary
        Map<String, Object> summary = new HashMap<>();
        summary.put("complexity", calculateComplexity(mainAnalysis));
        summary.put("maxDependencyDepth", calculateMaxDepth(mainAnalysis));
        summary.put("cyclicDependencies", detectCycles(mainAnalysis));

        context.put("analysis", summary);

        return context;
    }

    private String determineGrammarType(GrammarAnalysis analysis) {
        boolean hasLexer = !analysis.lexerRules.isEmpty();
        boolean hasParser = !analysis.parserRules.isEmpty();

        if (hasLexer && hasParser) {
            return "combined";
        } else if (hasLexer) {
            return "lexer";
        } else if (hasParser) {
            return "parser";
        } else {
            return "unknown";
        }
    }

    private int calculateComplexity(GrammarAnalysis analysis) {
        int complexity = 0;
        for (Set<String> deps : analysis.ruleDependencies.values()) {
            complexity += deps.size();
        }
        return complexity;
    }

    private int calculateMaxDepth(GrammarAnalysis analysis) {
        int maxDepth = 0;
        for (String rule : analysis.parserRules) {
            int depth = calculateDepth(rule, analysis, new HashSet<>(), 0);
            maxDepth = Math.max(maxDepth, depth);
        }
        return maxDepth;
    }

    private int calculateDepth(String rule, GrammarAnalysis analysis,
                              Set<String> visited, int currentDepth) {
        if (visited.contains(rule)) {
            return currentDepth;
        }

        visited.add(rule);
        Set<String> deps = analysis.ruleDependencies.getOrDefault(rule, Collections.emptySet());

        int maxChildDepth = currentDepth;
        for (String dep : deps) {
            if (analysis.parserRules.contains(dep)) {
                int childDepth = calculateDepth(dep, analysis, visited, currentDepth + 1);
                maxChildDepth = Math.max(maxChildDepth, childDepth);
            }
        }

        return maxChildDepth;
    }

    private boolean detectCycles(GrammarAnalysis analysis) {
        for (String rule : analysis.parserRules) {
            if (hasCycle(rule, analysis, new HashSet<>())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasCycle(String rule, GrammarAnalysis analysis, Set<String> visited) {
        if (visited.contains(rule)) {
            return true;
        }

        visited.add(rule);
        Set<String> deps = analysis.ruleDependencies.getOrDefault(rule, Collections.emptySet());

        for (String dep : deps) {
            if (analysis.parserRules.contains(dep)) {
                if (hasCycle(dep, analysis, new HashSet<>(visited))) {
                    return true;
                }
            }
        }

        return false;
    }

    private String extractGrammarName(String grammarText) {
        Pattern pattern = Pattern.compile(
            "(lexer\\s+grammar|parser\\s+grammar|grammar)\\s+([A-Za-z][A-Za-z0-9_]*)\\s*;"
        );
        Matcher matcher = pattern.matcher(grammarText);
        if (matcher.find()) {
            return matcher.group(2);
        }
        return "Unknown";
    }

    private static class GrammarAnalysis {
        String name;
        List<String> lexerRules = new ArrayList<>();
        List<String> parserRules = new ArrayList<>();
        List<String> imports = new ArrayList<>();
        Map<String, Set<String>> ruleDependencies = new HashMap<>();
    }
}
