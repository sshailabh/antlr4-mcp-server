package com.github.sshailabh.antlr4mcp.codegen;

import com.github.sshailabh.antlr4mcp.model.GrammarError;
import com.github.sshailabh.antlr4mcp.util.FileSystemUtils;
import com.github.sshailabh.antlr4mcp.util.GrammarNameExtractor;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.Tool;
import org.antlr.v4.tool.ANTLRMessage;
import org.antlr.v4.tool.ANTLRToolListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Compiles ANTLR grammars for multiple target languages
 */
@Component
@Slf4j
public class MultiTargetCompiler {

    /**
     * Compile grammar for specific target language
     */
    public CompilationResult compileForTarget(String grammarText, TargetLanguage target) {
        return compileForTarget(grammarText, target, null);
    }

    /**
     * Compile grammar for specific target language with grammar name
     */
    public CompilationResult compileForTarget(String grammarText, TargetLanguage target, String grammarName) {
        log.info("Compiling grammar for target: {}", target.getDisplayName());

        Path tempDir = null;
        try {
            // Create temp directory for generated files
            tempDir = Files.createTempDirectory("antlr-" + target.name().toLowerCase());
            log.debug("Created temp directory: {}", tempDir);

            // Determine grammar name
            if (grammarName == null) {
                grammarName = GrammarNameExtractor.extractGrammarName(grammarText);
                if (grammarName == null) {
                    grammarName = "Grammar"; // fallback for compatibility
                }
            }

            // Write grammar file
            Path grammarFile = tempDir.resolve(grammarName + ".g4");
            Files.writeString(grammarFile, grammarText, StandardCharsets.UTF_8);
            log.debug("Wrote grammar file: {}", grammarFile);

            // Compile grammar
            CompilationErrorListener errorListener = new CompilationErrorListener();
            Tool antlrTool = new Tool(new String[]{
                "-Dlanguage=" + target.getAntlrName(),
                "-o", tempDir.toString(),
                grammarFile.toString()
            });

            antlrTool.addListener(errorListener);
            antlrTool.processGrammarsOnCommandLine();

            // Check for errors
            if (errorListener.hasErrors()) {
                log.warn("Compilation failed with {} errors", errorListener.getErrors().size());
                return CompilationResult.failure(errorListener.getErrors());
            }

            // Collect generated files
            List<GeneratedFile> generatedFiles = collectGeneratedFiles(tempDir, target);
            log.info("Successfully generated {} files for target {}",
                    generatedFiles.size(), target.getDisplayName());

            return CompilationResult.success(target, generatedFiles);

        } catch (Exception e) {
            log.error("Compilation failed for target {}", target, e);
            GrammarError error = new GrammarError();
            error.setType("COMPILATION_ERROR");
            error.setMessage(e.getMessage());
            error.setLine(0);
            error.setColumn(0);
            return CompilationResult.failure(List.of(error));
        } finally {
            // Cleanup temp directory
            if (tempDir != null) {
                try {
                    FileSystemUtils.deleteDirectoryRecursively(tempDir);
                } catch (IOException e) {
                    log.warn("Failed to cleanup temp directory: {}", tempDir, e);
                }
            }
        }
    }

    /**
     * Collect generated files from compilation output directory
     */
    private List<GeneratedFile> collectGeneratedFiles(Path directory, TargetLanguage target)
            throws IOException {
        List<GeneratedFile> files = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(directory)) {
            List<Path> generatedPaths = paths
                .filter(Files::isRegularFile)
                .filter(p -> !p.toString().endsWith(".g4"))
                .filter(p -> shouldIncludeFile(p, target))
                .collect(Collectors.toList());

            for (Path path : generatedPaths) {
                String content = Files.readString(path, StandardCharsets.UTF_8);
                String relativePath = directory.relativize(path).toString();

                files.add(GeneratedFile.builder()
                    .fileName(relativePath)
                    .content(content)
                    .targetLanguage(target)
                    .fileType(determineFileType(path, target))
                    .build());
            }
        }

        return files;
    }

    /**
     * Check if file should be included in output
     */
    private boolean shouldIncludeFile(Path path, TargetLanguage target) {
        String fileName = path.getFileName().toString();

        // Include main source files
        if (fileName.endsWith(target.getFileExtension())) {
            return true;
        }

        // Include headers for C++
        if (target == TargetLanguage.CPP && fileName.endsWith(".h")) {
            return true;
        }

        // Include tokens file
        if (fileName.endsWith(".tokens")) {
            return true;
        }

        // Include interp file
        if (fileName.endsWith(".interp")) {
            return true;
        }

        return false;
    }

    /**
     * Determine file type for generated file
     */
    private String determineFileType(Path path, TargetLanguage target) {
        String fileName = path.getFileName().toString();

        if (fileName.endsWith(".tokens")) {
            return "tokens";
        }

        if (fileName.endsWith(".interp")) {
            return "interpreter";
        }

        if (fileName.contains("Lexer")) {
            return "lexer";
        }

        if (fileName.contains("Parser")) {
            return "parser";
        }

        if (fileName.contains("Listener")) {
            return "listener";
        }

        if (fileName.contains("Visitor")) {
            return "visitor";
        }

        if (fileName.contains("BaseListener")) {
            return "base_listener";
        }

        if (fileName.contains("BaseVisitor")) {
            return "base_visitor";
        }

        return "other";
    }



    /**
     * Error listener for ANTLR Tool compilation
     */
    private static class CompilationErrorListener implements ANTLRToolListener {
        private final List<GrammarError> errors = new ArrayList<>();

        @Override
        public void info(String msg) {
            log.info("ANTLR Tool: {}", msg);
        }

        @Override
        public void error(ANTLRMessage msg) {
            log.error("ANTLR Tool Error: {}", msg);
            GrammarError error = new GrammarError();
            error.setType("ANTLR_TOOL_ERROR");
            error.setMessage(msg.toString());
            error.setLine(0);
            error.setColumn(0);
            errors.add(error);
        }

        @Override
        public void warning(ANTLRMessage msg) {
            log.warn("ANTLR Tool Warning: {}", msg);
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public List<GrammarError> getErrors() {
            return errors;
        }
    }
}
