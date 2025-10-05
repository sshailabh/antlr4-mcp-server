package com.github.sshailabh.antlr4mcp.service;

import com.github.sshailabh.antlr4mcp.model.GrammarError;
import com.github.sshailabh.antlr4mcp.model.ParseResult;
import com.github.sshailabh.antlr4mcp.model.ValidationResult;
import com.github.sshailabh.antlr4mcp.security.ResourceManager;
import com.github.sshailabh.antlr4mcp.security.SecurityValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.Tool;
import org.antlr.v4.tool.ANTLRMessage;
import org.antlr.v4.tool.ANTLRToolListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class GrammarCompiler {

    private final SecurityValidator securityValidator;
    private final ResourceManager resourceManager;

    @Value("${antlr.security.validation.enabled:true}")
    private boolean securityValidationEnabled;

    @Value("${antlr.security.resource-limits.enabled:true}")
    private boolean resourceLimitsEnabled;

    @Value("${antlr.max-grammar-size-mb:10}")
    private int maxGrammarSizeMb = 10;

    @Value("${antlr.compilation-timeout-seconds:30}")
    private int compilationTimeoutSeconds;

    /**
     * Load grammar from text and return Grammar object for analysis
     */
    public org.antlr.v4.tool.Grammar loadGrammar(String grammarText) throws Exception {
        log.info("Loading grammar for analysis");

        String grammarName = extractGrammarName(grammarText);
        if (grammarName == null) {
            throw new IllegalArgumentException("Invalid grammar: no grammar declaration found");
        }

        Path tempDir = Files.createTempDirectory("antlr-load-");
        Path tempFile = tempDir.resolve(grammarName + ".g4");
        Files.writeString(tempFile, grammarText);

        try {
            Tool antlr = new Tool();
            antlr.outputDirectory = tempDir.toString();

            org.antlr.v4.tool.Grammar grammar = antlr.loadGrammar(tempFile.toString());
            if (grammar == null) {
                throw new IllegalArgumentException("Failed to load grammar");
            }

            return grammar;
        } finally {
            // Cleanup
            try (Stream<Path> paths = Files.walk(tempDir)) {
                paths.sorted(java.util.Comparator.reverseOrder())
                     .forEach(p -> {
                         try {
                             Files.delete(p);
                         } catch (Exception e) {
                             log.warn("Failed to delete temp file: {}", p);
                         }
                     });
            }
        }
    }

    public ValidationResult validate(String grammarText) {
        log.info("Validating grammar, size: {} bytes", grammarText.length());

        if (grammarText.length() > maxGrammarSizeMb * 1024 * 1024) {
            return ValidationResult.error(
                String.format("Grammar size exceeds limit (%dMB)", maxGrammarSizeMb)
            );
        }

        if (containsImports(grammarText)) {
            return ValidationResult.error(
                "Import statements are not supported in M1. " +
                "Please inline all imported grammars. " +
                "This limitation will be addressed in M2."
            );
        }

        try {
            String grammarName = extractGrammarName(grammarText);
            if (grammarName == null) {
                return ValidationResult.error(
                    "Could not find grammar declaration. Expected 'grammar Name;' or 'lexer grammar Name;' or 'parser grammar Name;'"
                );
            }

            // Validate grammar name for security
            if (securityValidationEnabled) {
                try {
                    securityValidator.validateGrammarName(grammarName);
                } catch (SecurityException e) {
                    return ValidationResult.error("Invalid grammar name: " + e.getMessage());
                }
            }

            Path tempDir = Files.createTempDirectory("antlr-validate-");
            Path tempFile = tempDir.resolve(grammarName + ".g4");

            // Validate the temp file path for security
            if (securityValidationEnabled) {
                securityValidator.validatePath(tempFile, tempDir);
            }

            Files.writeString(tempFile, grammarText);

            Tool antlr = new Tool();
            antlr.outputDirectory = tempDir.toString();
            List<GrammarError> errors = new ArrayList<>();

            antlr.addListener(new ANTLRToolListener() {
                @Override
                public void info(String msg) {
                    log.debug("ANTLR info: {}", msg);
                }

                @Override
                public void error(ANTLRMessage msg) {
                    log.debug("ANTLR error: {}", msg);
                    errors.add(convertAntlrError(msg));
                }

                @Override
                public void warning(ANTLRMessage msg) {
                    log.debug("ANTLR warning: {}", msg);
                }
            });

            antlr.inputDirectory = tempFile.getParent().toFile();
            org.antlr.v4.tool.Grammar g = antlr.loadGrammar(tempFile.toString());
            if (g != null) {
                antlr.process(g, false);
            }

            deleteDirectory(tempDir.toFile());

            if (!errors.isEmpty()) {
                return ValidationResult.builder()
                    .success(false)
                    .errors(errors)
                    .build();
            }

            int lexerRules = countLexerRules(grammarText);
            int parserRules = countParserRules(grammarText);

            return ValidationResult.success(grammarName, lexerRules, parserRules);

        } catch (Exception e) {
            log.error("Grammar validation failed", e);
            return ValidationResult.error("Validation error: " + e.getMessage());
        }
    }

    private boolean containsImports(String grammarText) {
        return Pattern.compile("\\bimport\\s+[A-Za-z][A-Za-z0-9_]*\\s*;").matcher(grammarText).find();
    }

    private String extractGrammarName(String grammarText) {
        Pattern pattern = Pattern.compile("(lexer\\s+grammar|parser\\s+grammar|grammar)\\s+([A-Za-z][A-Za-z0-9_]*)\\s*;");
        Matcher matcher = pattern.matcher(grammarText);
        if (matcher.find()) {
            return matcher.group(2);
        }
        return null;
    }

    private int countLexerRules(String grammarText) {
        Pattern pattern = Pattern.compile("^[A-Z][A-Za-z0-9_]*\\s*:", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(grammarText);
        int count = 0;
        while (matcher.find()) count++;
        return count;
    }

    private int countParserRules(String grammarText) {
        Pattern pattern = Pattern.compile("^[a-z][A-Za-z0-9_]*\\s*:", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(grammarText);
        int count = 0;
        while (matcher.find()) count++;
        return count;
    }

    private GrammarError convertAntlrError(ANTLRMessage msg) {
        String message = msg.getMessageTemplate(false).render();
        return GrammarError.builder()
            .type("syntax_error")
            .line(msg.line)
            .column(msg.charPosition)
            .message(message)
            .suggestedFix(generateFix(message))
            .build();
    }

    private String generateFix(String message) {
        String msgLower = message.toLowerCase();

        if (msgLower.contains("missing ';'")) {
            return "Add semicolon after rule definition";
        } else if (msgLower.contains("undefined")) {
            return "Define the referenced rule or check for typos";
        } else if (msgLower.contains("token")) {
            return "Check token definition and usage";
        } else if (msgLower.contains("no viable alternative")) {
            return "Check syntax at this location";
        }

        return "Review grammar syntax at this location";
    }

    private String getAntlrRuntimeClasspath() {
        String classpath = System.getProperty("java.class.path");
        StringBuilder antlrCp = new StringBuilder();
        for (String entry : classpath.split(File.pathSeparator)) {
            // We need both antlr4 (for TestRig) and antlr4-runtime
            if (entry.contains("antlr4") || entry.contains("antlr-runtime")) {
                if (antlrCp.length() > 0) {
                    antlrCp.append(File.pathSeparator);
                }
                antlrCp.append(entry);
            }
        }
        return antlrCp.length() > 0 ? antlrCp.toString() : System.getProperty("java.class.path");
    }

    private void deleteDirectory(File directory) {
        secureDeleteDirectory(directory);
    }

    /**
     * Securely deletes a directory with symlink protection.
     */
    private void secureDeleteDirectory(File directory) {
        if (!directory.exists()) {
            return;
        }

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                // Check for symlinks to prevent escaping the temp directory
                try {
                    if (Files.isSymbolicLink(file.toPath())) {
                        log.warn("Skipping symlink during cleanup: {}", file.getPath());
                        continue;
                    }
                } catch (Exception e) {
                    log.warn("Could not check symlink status for: {}", file.getPath());
                    continue;
                }

                if (file.isDirectory()) {
                    secureDeleteDirectory(file);
                } else {
                    if (!file.delete()) {
                        log.warn("Failed to delete file: {}", file.getPath());
                    }
                }
            }
        }
        if (!directory.delete()) {
            log.warn("Failed to delete directory: {}", directory.getPath());
        }
    }

    public ParseResult parse(String grammarText, String sampleInput, String startRule,
                            boolean showTokens, String treeFormat, boolean visualizeTree) {
        log.info("Parsing input with grammar, startRule: {}", startRule);

        // Validate start rule for security
        if (securityValidationEnabled) {
            try {
                securityValidator.validateRuleName(startRule);
            } catch (SecurityException e) {
                return ParseResult.builder()
                    .success(false)
                    .errors(List.of(GrammarError.builder()
                        .type("invalid_rule_name")
                        .message("Invalid start rule: " + e.getMessage())
                        .build()))
                    .build();
            }
        }

        // Validate input size
        if (resourceLimitsEnabled) {
            try {
                resourceManager.validateInputSize(sampleInput, "Sample input");
            } catch (SecurityException e) {
                return ParseResult.builder()
                    .success(false)
                    .errors(List.of(GrammarError.builder()
                        .type("input_too_large")
                        .message(e.getMessage())
                        .build()))
                    .build();
            }
        }

        ValidationResult validation = validate(grammarText);
        if (!validation.isSuccess()) {
            return ParseResult.builder()
                .success(false)
                .errors(validation.getErrors())
                .build();
        }

        try {
            String grammarName = extractGrammarName(grammarText);

            // Validate grammar name for security
            if (securityValidationEnabled) {
                try {
                    securityValidator.validateGrammarName(grammarName);
                } catch (SecurityException e) {
                    return ParseResult.builder()
                        .success(false)
                        .errors(List.of(GrammarError.builder()
                            .type("invalid_grammar_name")
                            .message("Invalid grammar name: " + e.getMessage())
                            .build()))
                        .build();
                }
            }

            Path tempDir = Files.createTempDirectory("antlr-parse-");
            Path tempFile = tempDir.resolve(grammarName + ".g4");

            // Validate path security
            if (securityValidationEnabled) {
                securityValidator.validatePath(tempFile, tempDir);
            }

            Files.writeString(tempFile, grammarText);

            // Configure ANTLR Tool with arguments
            String[] args = new String[] {
                "-o", tempDir.toString(),
                tempFile.toString()
            };
            Tool antlr = new Tool(args);
            List<GrammarError> errors = new ArrayList<>();

            antlr.addListener(new ANTLRToolListener() {
                @Override
                public void info(String msg) {
                    log.debug("ANTLR info: {}", msg);
                }

                @Override
                public void error(ANTLRMessage msg) {
                    log.error("ANTLR error: {}", msg);
                    errors.add(convertAntlrError(msg));
                }

                @Override
                public void warning(ANTLRMessage msg) {
                    log.debug("ANTLR warning: {}", msg);
                }
            });

            // Process the grammar file to generate code
            antlr.processGrammarsOnCommandLine();

            if (!errors.isEmpty()) {
                deleteDirectory(tempDir.toFile());
                return ParseResult.builder()
                    .success(false)
                    .errors(errors)
                    .build();
            }

            // Compile generated Java files
            List<String> javaFiles = new ArrayList<>();
            try (Stream<Path> stream = Files.walk(tempDir)) {
                stream.filter(path -> path.toString().endsWith(".java"))
                      .forEach(path -> javaFiles.add(path.toString()));
            }

            log.debug("Found Java files to compile: {}", javaFiles);

            if (!javaFiles.isEmpty()) {
                List<String> compileCommand = new ArrayList<>();
                compileCommand.add("javac");
                compileCommand.add("-cp");
                compileCommand.add(getAntlrRuntimeClasspath());
                compileCommand.add("-d");
                compileCommand.add(tempDir.toString());
                compileCommand.addAll(javaFiles);

                log.debug("Compiling Java files: {}", String.join(" ", compileCommand));

                ProcessBuilder compilePb = new ProcessBuilder(compileCommand);
                compilePb.directory(tempDir.toFile());

                ResourceManager.ProcessResult compileResult;
                try {
                    compileResult = resourceManager.executeWithTimeout(compilePb);
                } catch (TimeoutException e) {
                    deleteDirectory(tempDir.toFile());
                    return ParseResult.builder()
                        .success(false)
                        .errors(List.of(GrammarError.builder()
                            .type("timeout_error")
                            .message("Compilation timed out")
                            .build()))
                        .build();
                }

                if (compileResult.getExitCode() != 0) {
                    deleteDirectory(tempDir.toFile());
                    return ParseResult.builder()
                        .success(false)
                        .errors(List.of(GrammarError.builder()
                            .type("compilation_error")
                            .message("Failed to compile generated parser: " + compileResult.getErrorOutput())
                            .build()))
                        .build();
                }
            }

            Path inputFile = tempDir.resolve("input.txt");
            Files.writeString(inputFile, sampleInput);

            List<String> command = new ArrayList<>();
            command.add("java");
            command.add("-cp");
            command.add(tempDir.toString() + File.pathSeparator + getAntlrRuntimeClasspath());
            command.add("org.antlr.v4.gui.TestRig");
            command.add(grammarName);
            command.add(startRule);
            if (showTokens) command.add("-tokens");
            if (treeFormat != null) command.add("-" + treeFormat);
            // TestRig reads from stdin by default, not from a file argument

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(false);
            pb.redirectInput(inputFile.toFile());

            // Execute with timeout and resource limits
            ResourceManager.ProcessResult processResult;
            try {
                processResult = resourceManager.executeWithTimeout(pb);
            } catch (TimeoutException e) {
                deleteDirectory(tempDir.toFile());
                return ParseResult.builder()
                    .success(false)
                    .errors(List.of(GrammarError.builder()
                        .type("timeout_error")
                        .message("Parse operation timed out after " + compilationTimeoutSeconds + " seconds")
                        .build()))
                    .build();
            }

            int exitCode = processResult.getExitCode();
            String output = processResult.getOutput();
            String errorOutput = processResult.getErrorOutput();

            log.debug("TestRig exit code: {}", exitCode);
            log.debug("TestRig output: {}", output);
            log.debug("TestRig error: {}", errorOutput);

            deleteDirectory(tempDir.toFile());

            if (exitCode != 0) {
                String errorMsg = errorOutput != null && !errorOutput.isEmpty() ? errorOutput : output;
                return ParseResult.builder()
                    .success(false)
                    .errors(List.of(GrammarError.builder()
                        .type("parse_error")
                        .message("Parse failed: " + resourceManager.limitOutput(errorMsg))
                        .build()))
                    .build();
            }

            String outputStr = resourceManager.limitOutput(output);
            String tokens = null;
            String parseTree = null;

            if (showTokens) {
                int treeStart = outputStr.indexOf("(");
                if (treeStart > 0) {
                    tokens = outputStr.substring(0, treeStart).trim();
                    parseTree = outputStr.substring(treeStart).trim();
                } else {
                    tokens = outputStr;
                }
            } else {
                parseTree = outputStr.trim();
            }

            return ParseResult.builder()
                .success(true)
                .tokens(tokens)
                .parseTree(parseTree)
                .build();

        } catch (Exception e) {
            log.error("Parse failed", e);
            return ParseResult.builder()
                .success(false)
                .errors(List.of(GrammarError.builder()
                    .type("internal_error")
                    .message("Parse error: " + e.getMessage())
                    .build()))
                .build();
        }
    }
}
