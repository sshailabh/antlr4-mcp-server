package com.github.sshailabh.antlr4mcp.service;

import com.github.sshailabh.antlr4mcp.model.GrammarError;
import com.github.sshailabh.antlr4mcp.model.ValidationResult;
import com.github.sshailabh.antlr4mcp.security.SecurityValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.Tool;
import org.antlr.v4.tool.ANTLRMessage;
import org.antlr.v4.tool.ANTLRToolListener;
import org.antlr.v4.tool.Grammar;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Core service for loading and validating ANTLR4 grammars.
 * Uses ANTLR4's Tool class for grammar processing.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GrammarCompiler {

    private final SecurityValidator securityValidator;

    @Value("${antlr.security.validation.enabled:true}")
    private boolean securityEnabled;

    @Value("${antlr.max-grammar-size-mb:10}")
    private int maxGrammarSizeMb;

    private static final Pattern GRAMMAR_DECL = Pattern.compile(
        "(lexer\\s+grammar|parser\\s+grammar|grammar)\\s+([A-Za-z][A-Za-z0-9_]*)\\s*;"
    );
    private static final Pattern IMPORT_STMT = Pattern.compile(
        "\\bimport\\s+[A-Za-z][A-Za-z0-9_]*\\s*;"
    );
    private static final Pattern LEXER_RULE = Pattern.compile("^[A-Z][A-Za-z0-9_]*\\s*:", Pattern.MULTILINE);
    private static final Pattern PARSER_RULE = Pattern.compile("^[a-z][A-Za-z0-9_]*\\s*:", Pattern.MULTILINE);

    /**
     * Load grammar from text and return Grammar object for analysis.
     */
    public Grammar loadGrammar(String grammarText) throws Exception {
        String grammarName = extractGrammarName(grammarText);
        if (grammarName == null) {
            throw new IllegalArgumentException("No grammar declaration found");
        }

        Path tempDir = Files.createTempDirectory("antlr-");
        try {
            Path grammarFile = tempDir.resolve(grammarName + ".g4");
            Files.writeString(grammarFile, grammarText);

            Tool antlr = new Tool();
            antlr.outputDirectory = tempDir.toString();
            
            Grammar grammar = antlr.loadGrammar(grammarFile.toString());
            if (grammar == null) {
                throw new IllegalArgumentException("Failed to load grammar");
            }
            return grammar;
        } finally {
            deleteDir(tempDir);
        }
    }

    /**
     * Validate grammar syntax and return detailed results.
     */
    public ValidationResult validate(String grammarText) {
        log.debug("Validating grammar, {} bytes", grammarText.length());

        // Size check
        if (grammarText.length() > maxGrammarSizeMb * 1024 * 1024) {
            return ValidationResult.error("Grammar exceeds " + maxGrammarSizeMb + "MB limit");
        }

        // Import check (not supported)
        if (IMPORT_STMT.matcher(grammarText).find()) {
            return ValidationResult.error(
                "Import statements not supported. Inline all grammar rules into a single file."
            );
        }

        String grammarName = extractGrammarName(grammarText);
        if (grammarName == null) {
            return ValidationResult.error(
                "No grammar declaration found. Expected: grammar Name; or lexer grammar Name;"
            );
        }

        // Security validation
        if (securityEnabled) {
            try {
                securityValidator.validateGrammarName(grammarName);
            } catch (SecurityException e) {
                return ValidationResult.error("Invalid grammar name: " + e.getMessage());
            }
        }

        try {
            Path tempDir = Files.createTempDirectory("antlr-validate-");
            try {
                Path grammarFile = tempDir.resolve(grammarName + ".g4");
                Files.writeString(grammarFile, grammarText);

                List<GrammarError> errors = new ArrayList<>();
                Tool antlr = new Tool();
                antlr.outputDirectory = tempDir.toString();
                antlr.inputDirectory = tempDir.toFile();
                
                antlr.addListener(new ANTLRToolListener() {
                    @Override public void info(String msg) {}
                    @Override public void warning(ANTLRMessage msg) {}
                    @Override
                    public void error(ANTLRMessage msg) {
                        errors.add(GrammarError.builder()
                            .type("syntax_error")
                            .line(msg.line)
                            .column(msg.charPosition)
                            .message(msg.getMessageTemplate(false).render())
                            .build());
                    }
                });

                Grammar g = antlr.loadGrammar(grammarFile.toString());
                if (g != null) {
                    antlr.process(g, false);
                }

                if (!errors.isEmpty()) {
                    return ValidationResult.builder()
                        .success(false)
                        .errors(errors)
                        .build();
                }

                return ValidationResult.success(grammarName, countRules(grammarText, LEXER_RULE), 
                                                countRules(grammarText, PARSER_RULE));
            } finally {
                deleteDir(tempDir);
            }
        } catch (Exception e) {
            log.error("Validation failed", e);
            return ValidationResult.error("Validation error: " + e.getMessage());
        }
    }

    /**
     * Extract grammar name from text.
     */
    public String extractGrammarName(String grammarText) {
        Matcher m = GRAMMAR_DECL.matcher(grammarText);
        return m.find() ? m.group(2) : null;
    }

    private int countRules(String text, Pattern pattern) {
        Matcher m = pattern.matcher(text);
        int count = 0;
        while (m.find()) count++;
        return count;
    }

    private void deleteDir(Path dir) {
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(p -> {
                try { Files.delete(p); } catch (Exception e) { /* ignore */ }
            });
        } catch (Exception e) {
            log.warn("Failed to clean up: {}", dir);
        }
    }
}
