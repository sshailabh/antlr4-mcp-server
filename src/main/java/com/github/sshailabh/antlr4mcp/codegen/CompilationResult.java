package com.github.sshailabh.antlr4mcp.codegen;

import com.github.sshailabh.antlr4mcp.model.GrammarError;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of grammar compilation for a target language
 */
@Data
@Builder
public class CompilationResult {
    private boolean success;
    private TargetLanguage targetLanguage;
    private List<GeneratedFile> generatedFiles;
    private List<GrammarError> errors;

    public static CompilationResult success(TargetLanguage target, List<GeneratedFile> files) {
        return CompilationResult.builder()
            .success(true)
            .targetLanguage(target)
            .generatedFiles(files)
            .errors(new ArrayList<>())
            .build();
    }

    public static CompilationResult failure(List<GrammarError> errors) {
        return CompilationResult.builder()
            .success(false)
            .targetLanguage(null)
            .generatedFiles(new ArrayList<>())
            .errors(errors)
            .build();
    }

    public boolean hasGeneratedFiles() {
        return generatedFiles != null && !generatedFiles.isEmpty();
    }

    public int getGeneratedFileCount() {
        return generatedFiles != null ? generatedFiles.size() : 0;
    }
}
