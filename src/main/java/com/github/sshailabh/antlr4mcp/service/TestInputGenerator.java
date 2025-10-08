package com.github.sshailabh.antlr4mcp.service;

import com.github.sshailabh.antlr4mcp.model.GeneratedTestInputs;
import com.github.sshailabh.antlr4mcp.model.TestInput;
import com.github.sshailabh.antlr4mcp.util.FileSystemUtils;
import com.github.sshailabh.antlr4mcp.util.GrammarNameExtractor;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.Tool;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.tool.Grammar;
import org.antlr.v4.tool.Rule;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generates sample test inputs for ANTLR4 grammar rules.
 * Uses a simplified approach with hardcoded token examples.
 */
@Slf4j
@Service
public class TestInputGenerator {

    private static final Map<String, String> TOKEN_EXAMPLES = Map.of(
        "INT", "42",
        "FLOAT", "3.14",
        "ID", "foo",
        "STRING", "\"hello\"",
        "WS", " "
    );

    private static final int MAX_RECURSION_DEPTH = 3;

    /**
     * Generate test inputs for a grammar rule.
     *
     * @param grammarText The complete ANTLR4 grammar
     * @param ruleName Name of the rule to generate inputs for
     * @param maxInputs Maximum number of inputs to generate
     * @return Generated test inputs
     */
    public GeneratedTestInputs generate(String grammarText, String ruleName, int maxInputs) {
        log.info("Generating test inputs for rule: {} (max: {})", ruleName, maxInputs);

        if (grammarText == null || grammarText.trim().isEmpty()) {
            throw new IllegalArgumentException("Grammar text cannot be null or empty");
        }

        if (ruleName == null || ruleName.trim().isEmpty()) {
            throw new IllegalArgumentException("Rule name cannot be null or empty");
        }

        try {
            Grammar grammar = loadGrammarFromText(grammarText);
            if (grammar == null) {
                throw new IllegalStateException("Failed to load grammar");
            }

            Rule rule = grammar.getRule(ruleName);
            if (rule == null) {
                throw new IllegalArgumentException("Rule not found: " + ruleName);
            }

            List<TestInput> inputs = new ArrayList<>();
            Set<Integer> coveredAlternatives = new HashSet<>();

            // Generate inputs by exploring alternatives
            int alternativeCount = rule.numberOfAlts;
            for (int alt = 1; alt <= alternativeCount && inputs.size() < maxInputs; alt++) {
                try {
                    String input = generateForAlternative(grammarText, rule, alt, 0);
                    if (input != null && !input.trim().isEmpty()) {
                        inputs.add(TestInput.builder()
                            .input(input.trim())
                            .description("Alternative " + alt + " of rule '" + ruleName + "'")
                            .path(ruleName + " [alt:" + alt + "]")
                            .alternative(alt)
                            .complexity(1)
                            .build());
                        coveredAlternatives.add(alt);
                    }
                } catch (Exception e) {
                    log.warn("Failed to generate input for alternative {}: {}", alt, e.getMessage());
                }
            }

            log.info("Generated {} test inputs for rule '{}'", inputs.size(), ruleName);

            return GeneratedTestInputs.builder()
                .ruleName(ruleName)
                .totalInputs(inputs.size())
                .inputs(inputs)
                .alternativesCovered(coveredAlternatives.size())
                .maxDepth(MAX_RECURSION_DEPTH)
                .limited(inputs.size() >= maxInputs)
                .build();

        } catch (IllegalArgumentException e) {
            log.error("Invalid input for test generation", e);
            throw e;
        } catch (Exception e) {
            log.error("Test input generation failed", e);
            throw new RuntimeException("Test generation error: " + e.getMessage(), e);
        }
    }

    /**
     * Generate input for a specific alternative in a rule.
     */
    private String generateForAlternative(String grammarText, Rule rule, int alternative, int depth) {
        if (depth > MAX_RECURSION_DEPTH) {
            return "";
        }

        // This is a simplified implementation
        // In a full implementation, we would walk the ATN for the specific alternative
        // For now, we generate based on common patterns

        // Try to find tokens mentioned in the grammar
        List<String> tokens = extractTokens(grammarText);

        if (tokens.isEmpty()) {
            return getTokenExample(rule.name);
        }

        StringBuilder result = new StringBuilder();
        for (String token : tokens) {
            String example = getTokenExample(token);
            if (!example.isEmpty()) {
                if (result.length() > 0) {
                    result.append(" ");
                }
                result.append(example);
            }
        }

        return result.toString();
    }

    /**
     * Get example value for a token.
     */
    private String getTokenExample(String tokenName) {
        // Check hardcoded examples
        if (TOKEN_EXAMPLES.containsKey(tokenName.toUpperCase())) {
            return TOKEN_EXAMPLES.get(tokenName.toUpperCase());
        }

        // Generate based on token name patterns
        if (tokenName.matches(".*INT.*")) {
            return "42";
        } else if (tokenName.matches(".*FLOAT.*|.*DOUBLE.*|.*DECIMAL.*")) {
            return "3.14";
        } else if (tokenName.matches(".*ID.*|.*IDENTIFIER.*")) {
            return "foo";
        } else if (tokenName.matches(".*STRING.*")) {
            return "\"hello\"";
        } else if (tokenName.matches(".*NUM.*|.*NUMBER.*")) {
            return "123";
        }

        return "";
    }

    /**
     * Extract token names from grammar text (simplified).
     */
    private List<String> extractTokens(String grammarText) {
        List<String> tokens = new ArrayList<>();
        // Match uppercase words (likely tokens)
        Pattern pattern = Pattern.compile("\\b[A-Z][A-Z_0-9]*\\b");
        Matcher matcher = pattern.matcher(grammarText);

        while (matcher.find()) {
            String token = matcher.group();
            if (!token.equals("WS")) { // Skip whitespace tokens
                tokens.add(token);
            }
        }

        return tokens;
    }

    /**
     * Load grammar from text using full compilation.
     */
    private Grammar loadGrammarFromText(String grammarText) {
        try {
            String grammarName = GrammarNameExtractor.extractGrammarName(grammarText);
            if (grammarName == null) {
                log.error("Could not extract grammar name");
                return null;
            }

            Path tempDir = Files.createTempDirectory("antlr-testgen-");
            Path tempFile = tempDir.resolve(grammarName + ".g4");
            Files.writeString(tempFile, grammarText);

            Tool tool = new Tool();
            tool.errMgr.setFormat("antlr");
            tool.outputDirectory = tempDir.toString();
            tool.inputDirectory = tempFile.getParent().toFile();
            Grammar grammar = tool.loadGrammar(tempFile.toString());

            if (grammar != null) {
                tool.process(grammar, false);
            }

            FileSystemUtils.deleteDirectoryRecursively(tempDir.toFile());
            return grammar;
        } catch (Exception e) {
            log.error("Failed to load grammar", e);
            return null;
        }
    }


}
