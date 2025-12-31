package com.github.sshailabh.antlr4mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sshailabh.antlr4mcp.codegen.CompilationResult;
import com.github.sshailabh.antlr4mcp.codegen.GeneratedFile;
import com.github.sshailabh.antlr4mcp.codegen.MultiTargetCompiler;
import com.github.sshailabh.antlr4mcp.codegen.TargetLanguage;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * MCP Tool for compiling ANTLR grammars for multiple target languages
 */
@Slf4j
public class CompileGrammarMultiTargetTool {

    private final MultiTargetCompiler multiTargetCompiler;
    private final ObjectMapper objectMapper;

    public CompileGrammarMultiTargetTool(MultiTargetCompiler multiTargetCompiler,
                                         ObjectMapper objectMapper) {
        this.multiTargetCompiler = multiTargetCompiler;
        this.objectMapper = objectMapper;
    }

    public McpSchema.Tool toTool() {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> grammarText = new HashMap<>();
        grammarText.put("type", "string");
        grammarText.put("description", "The ANTLR4 grammar text to compile");
        properties.put("grammar_text", grammarText);

        Map<String, Object> targetLanguage = new HashMap<>();
        targetLanguage.put("type", "string");
        targetLanguage.put("enum", Arrays.asList(
            "java", "python", "python3", "javascript", "typescript", "cpp",
            "csharp", "go", "swift", "php", "dart"
        ));
        targetLanguage.put("default", "java");
        targetLanguage.put("description", "Target language for code generation");
        properties.put("target_language", targetLanguage);

        Map<String, Object> generateListener = new HashMap<>();
        generateListener.put("type", "boolean");
        generateListener.put("default", true);
        generateListener.put("description", "Generate listener classes");
        properties.put("generate_listener", generateListener);

        Map<String, Object> generateVisitor = new HashMap<>();
        generateVisitor.put("type", "boolean");
        generateVisitor.put("default", false);
        generateVisitor.put("description", "Generate visitor classes");
        properties.put("generate_visitor", generateVisitor);

        Map<String, Object> includeGeneratedCode = new HashMap<>();
        includeGeneratedCode.put("type", "boolean");
        includeGeneratedCode.put("default", false);
        includeGeneratedCode.put("description", "Include full generated code in response (default: false, only file list)");
        properties.put("include_generated_code", includeGeneratedCode);

        McpSchema.JsonSchema schema = new McpSchema.JsonSchema(
            "object",
            properties,
            List.of("grammar_text"),
            null,
            null,
            null
        );

        return McpSchema.Tool.builder()
            .name("compile_grammar_multi_target")
            .description("Compile ANTLR4 grammar for a specific target language (Java, Python, JavaScript, " +
                "TypeScript, C++, C#, Go, Swift, PHP, or Dart). Returns generated file list and optionally " +
                "the generated code. This tool validates the grammar and generates language-specific parser code.")
            .inputSchema(schema)
            .build();
    }

    public McpSchema.CallToolResult execute(McpSyncServerExchange exchange, McpSchema.CallToolRequest request) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> arguments = (Map<String, Object>) request.arguments();

            String grammarText = (String) arguments.get("grammar_text");
            String targetLangStr = (String) arguments.getOrDefault("target_language", "java");
            Boolean generateListener = (Boolean) arguments.getOrDefault("generate_listener", true);
            Boolean generateVisitor = (Boolean) arguments.getOrDefault("generate_visitor", false);
            Boolean includeCode = (Boolean) arguments.getOrDefault("include_generated_code", false);

            log.info("Compiling grammar for target language: {} (listener={}, visitor={})", 
                     targetLangStr, generateListener, generateVisitor);

            // Parse target language
            TargetLanguage targetLanguage;
            try {
                targetLanguage = TargetLanguage.fromString(targetLangStr);
            } catch (IllegalArgumentException e) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", e.getMessage());
                String jsonResult = objectMapper.writeValueAsString(error);
                return new McpSchema.CallToolResult(jsonResult, true);
            }

            // Compile grammar
            CompilationResult result = multiTargetCompiler.compileForTarget(
                grammarText, targetLanguage, null, generateListener, generateVisitor);

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("targetLanguage", targetLanguage.getDisplayName());

            if (result.isSuccess()) {
                response.put("fileCount", result.getGeneratedFileCount());

                // File summary
                List<Map<String, Object>> files = new ArrayList<>();
                for (GeneratedFile file : result.getGeneratedFiles()) {
                    Map<String, Object> fileInfo = new HashMap<>();
                    fileInfo.put("fileName", file.getFileName());
                    fileInfo.put("fileType", file.getFileType());
                    fileInfo.put("lineCount", file.getLineCount());
                    fileInfo.put("size", file.getSize());

                    if (includeCode) {
                        fileInfo.put("content", file.getContent());
                    }

                    files.add(fileInfo);
                }
                response.put("files", files);

                // Runtime info
                Map<String, Object> runtimeInfo = new HashMap<>();
                runtimeInfo.put("import", targetLanguage.getRuntimeImport());
                runtimeInfo.put("stronglyTyped", targetLanguage.isStronglyTyped());
                runtimeInfo.put("garbageCollected", targetLanguage.isGarbageCollected());
                response.put("runtimeInfo", runtimeInfo);

            } else {
                response.put("errors", result.getErrors());
            }

            String jsonResult = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(response);

            return new McpSchema.CallToolResult(jsonResult, !result.isSuccess());

        } catch (Exception e) {
            log.error("Failed to compile grammar", e);
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
}
