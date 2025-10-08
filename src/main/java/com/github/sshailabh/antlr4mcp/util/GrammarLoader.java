package com.github.sshailabh.antlr4mcp.util;

import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.Tool;
import org.antlr.v4.tool.Grammar;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility class for loading ANTLR4 grammars with full compilation.
 * Consolidates duplicate grammar loading code across multiple services.
 * 
 * This utility provides full compilation using ANTLR Tool which is required for:
 * - Complex grammar analysis (decision points, complexity metrics)
 * - Left-recursion analysis and transformations
 * - Any analysis requiring complete grammar processing
 * 
 * For simple parsing and visualization tasks, consider using GrammarInterpreter
 * service which provides 10-100x better performance.
 */
@Slf4j
public final class GrammarLoader {

    private GrammarLoader() {
        // Utility class - prevent instantiation
    }

    /**
     * Load grammar from text using full ANTLR compilation.
     * 
     * This method:
     * 1. Extracts grammar name using GrammarNameExtractor
     * 2. Creates temporary file for ANTLR processing
     * 3. Uses ANTLR Tool for full grammar compilation
     * 4. Processes grammar with tool.process() for complete analysis
     * 5. Cleans up temporary files using FileSystemUtils
     * 
     * @param grammarText The complete ANTLR4 grammar text
     * @return Grammar object ready for analysis, or null if loading failed
     * @throws IllegalArgumentException if grammarText is null or empty
     */
    public static Grammar loadGrammar(String grammarText) {
        if (grammarText == null || grammarText.trim().isEmpty()) {
            throw new IllegalArgumentException("Grammar text cannot be null or empty");
        }

        try {
            // Extract grammar name
            String grammarName = GrammarNameExtractor.extractGrammarName(grammarText);
            if (grammarName == null) {
                log.error("Could not extract grammar name from grammar text");
                return null;
            }

            // Create temporary file for ANTLR processing
            Path tempDir = Files.createTempDirectory("antlr-grammar-");
            Path tempFile = tempDir.resolve(grammarName + ".g4");
            Files.writeString(tempFile, grammarText);

            try {
                // Load grammar using ANTLR Tool (full compilation)
                Tool tool = new Tool();
                tool.errMgr.setFormat("antlr");
                tool.outputDirectory = tempDir.toString();
                tool.inputDirectory = tempFile.getParent().toFile();
                Grammar grammar = tool.loadGrammar(tempFile.toString());

                if (grammar != null) {
                    // Process grammar for complete analysis
                    tool.process(grammar, false);
                    log.debug("Successfully loaded and processed grammar: {}", grammarName);
                } else {
                    log.error("Failed to load grammar: {}", grammarName);
                }

                return grammar;

            } finally {
                // Cleanup temporary files
                FileSystemUtils.deleteDirectoryRecursively(tempDir.toFile());
            }

        } catch (Exception e) {
            log.error("Failed to load grammar from text", e);
            return null;
        }
    }


}
