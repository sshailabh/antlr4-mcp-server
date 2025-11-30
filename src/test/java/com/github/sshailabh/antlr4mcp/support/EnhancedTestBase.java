package com.github.sshailabh.antlr4mcp.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sshailabh.antlr4mcp.model.*;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Enhanced test base with modern testing practices for 15+ year Java developers.
 *
 * Features:
 * - Fluent assertions using AssertJ
 * - Minimal boilerplate with smart defaults
 * - Type-safe result parsing
 * - Comprehensive assertion helpers
 * - Parameterized test support
 */
@SpringBootTest
public abstract class EnhancedTestBase {

    @Autowired
    protected ObjectMapper objectMapper;

    protected McpSyncServerExchange mockExchange;

    @BeforeEach
    public void baseSetUp() {
        mockExchange = mock(McpSyncServerExchange.class);
    }

    // ========== Tool Execution Helpers ==========

    /**
     * Execute a tool with a map of arguments
     */
    protected <T> ToolExecution executeTool(T tool, String toolName, Map<String, Object> args,
                                           ToolExecutor<T> executor) {
        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(toolName, args);
        McpSchema.CallToolResult result = executor.execute(tool, mockExchange, request);
        return new ToolExecution(result, objectMapper);
    }

    /**
     * Execute a tool with fluent args builder
     */
    protected <T> ToolExecution executeTool(T tool, String toolName, Consumer<Args> argsBuilder,
                                           ToolExecutor<T> executor) {
        Args args = new Args();
        argsBuilder.accept(args);
        return executeTool(tool, toolName, args.build(), executor);
    }

    @FunctionalInterface
    public interface ToolExecutor<T> {
        McpSchema.CallToolResult execute(T tool, McpSyncServerExchange exchange, McpSchema.CallToolRequest request);
    }

    // ========== Fluent Assertions ==========

    protected ToolResultAssert assertThat(McpSchema.CallToolResult result) {
        return new ToolResultAssert(result, objectMapper);
    }

    protected ValidationResultAssert assertThat(ValidationResult result) {
        return new ValidationResultAssert(result);
    }

    protected ParseResultAssert assertThat(ParseResult result) {
        return new ParseResultAssert(result);
    }

    // ========== Nested Classes ==========

    /**
     * Fluent argument builder
     */
    public static class Args {
        private final Map<String, Object> map = new java.util.HashMap<>();

        public Args grammar(String text) {
            map.put("grammar_text", text);
            return this;
        }

        public Args input(String text) {
            map.put("sample_input", text);
            return this;
        }

        public Args startRule(String rule) {
            map.put("start_rule", rule);
            return this;
        }

        public Args format(String format) {
            map.put("format", format);
            return this;
        }

        public Args put(String key, Object value) {
            map.put(key, value);
            return this;
        }

        public Map<String, Object> build() {
            return map;
        }
    }

    /**
     * Tool execution result wrapper with type-safe parsing
     */
    public static class ToolExecution {
        private final McpSchema.CallToolResult result;
        private final ObjectMapper objectMapper;

        public ToolExecution(McpSchema.CallToolResult result, ObjectMapper objectMapper) {
            this.result = result;
            this.objectMapper = objectMapper;
        }

        public McpSchema.CallToolResult raw() {
            return result;
        }

        public String contentText() {
            assertFalse(result.content().isEmpty(), "Result should have content");
            return ((McpSchema.TextContent) result.content().get(0)).text();
        }

        public <T> T as(Class<T> type) {
            try {
                return objectMapper.readValue(contentText(), type);
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse result as " + type.getName(), e);
            }
        }

        public <T> T as(TypeReference<T> typeRef) {
            try {
                return objectMapper.readValue(contentText(), typeRef);
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse result", e);
            }
        }

        public ValidationResult asValidation() {
            return as(ValidationResult.class);
        }

        public ParseResult asParse() {
            return as(ParseResult.class);
        }

        public Map<String, Object> asMap() {
            return as(new TypeReference<>() {});
        }

        public ToolResultAssert assertThat() {
            return new ToolResultAssert(result, objectMapper);
        }
    }

    /**
     * Custom assertions for tool results
     */
    public static class ToolResultAssert {
        private final McpSchema.CallToolResult result;
        private final ObjectMapper objectMapper;

        public ToolResultAssert(McpSchema.CallToolResult result, ObjectMapper objectMapper) {
            this.result = result;
            this.objectMapper = objectMapper;
            assertNotNull(result, "Result should not be null");
        }

        public ToolResultAssert isSuccess() {
            assertFalse(result.isError(), "Result should not be an error");
            assertFalse(result.content().isEmpty(), "Result should have content");
            return this;
        }

        public ToolResultAssert isError() {
            assertTrue(result.isError(), "Result should be an error");
            return this;
        }

        public ToolResultAssert hasContent() {
            assertFalse(result.content().isEmpty(), "Result should have content");
            return this;
        }

        public ToolResultAssert contentContains(String... expectedStrings) {
            String content = ((McpSchema.TextContent) result.content().get(0)).text();
            for (String expected : expectedStrings) {
                assertTrue(content.contains(expected), "Content should contain: " + expected);
            }
            return this;
        }

        public <T> T parseAs(Class<T> type) {
            try {
                String content = ((McpSchema.TextContent) result.content().get(0)).text();
                return objectMapper.readValue(content, type);
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse content", e);
            }
        }
    }

    /**
     * Custom assertions for ValidationResult
     */
    public static class ValidationResultAssert {
        private final ValidationResult result;

        public ValidationResultAssert(ValidationResult result) {
            this.result = result;
            assertNotNull(result, "ValidationResult should not be null");
        }

        public ValidationResultAssert isSuccess() {
            assertTrue(result.isSuccess(), "Validation should succeed");
            assertTrue(result.getErrors().isEmpty(), "Should have no errors");
            return this;
        }

        public ValidationResultAssert isFailure() {
            assertFalse(result.isSuccess(), "Validation should fail");
            assertFalse(result.getErrors().isEmpty(), "Should have errors");
            return this;
        }

        public ValidationResultAssert hasGrammarName(String expectedName) {
            assertEquals(expectedName, result.getGrammarName(), "Grammar name should match");
            return this;
        }

        public ValidationResultAssert hasParserRules(int expected) {
            assertEquals(expected, result.getParserRules(), "Parser rules count should match");
            return this;
        }

        public ValidationResultAssert hasLexerRules(int expected) {
            assertEquals(expected, result.getLexerRules(), "Lexer rules count should match");
            return this;
        }

        public ValidationResultAssert hasErrorContaining(String message) {
            boolean found = result.getErrors().stream()
                .anyMatch(err -> err.getMessage().contains(message));
            assertTrue(found, "Should have error containing: " + message);
            return this;
        }

        public ValidationResultAssert hasErrorCount(int expected) {
            assertEquals(expected, result.getErrors().size(), "Error count should match");
            return this;
        }
    }

    /**
     * Custom assertions for ParseResult
     */
    public static class ParseResultAssert {
        private final ParseResult result;

        public ParseResultAssert(ParseResult result) {
            this.result = result;
            assertNotNull(result, "ParseResult should not be null");
        }

        public ParseResultAssert isSuccess() {
            assertTrue(result.isSuccess(), "Parse should succeed");
            assertTrue(result.getErrors().isEmpty(), "Should have no errors");
            return this;
        }

        public ParseResultAssert isFailure() {
            assertFalse(result.isSuccess(), "Parse should fail");
            return this;
        }

        public ParseResultAssert hasParseTree() {
            assertNotNull(result.getParseTree(), "Should have parse tree");
            assertFalse(result.getParseTree().isEmpty(), "Parse tree should not be empty");
            return this;
        }

        public ParseResultAssert hasTokens() {
            assertNotNull(result.getTokens(), "Should have tokens");
            assertFalse(result.getTokens().isEmpty(), "Tokens should not be empty");
            return this;
        }

        public ParseResultAssert hasSvg() {
            assertNotNull(result.getSvg(), "Should have SVG");
            assertFalse(result.getSvg().isEmpty(), "SVG should not be empty");
            return this;
        }

        public ParseResultAssert parseTreeContains(String expected) {
            assertTrue(result.getParseTree().contains(expected),
                "Parse tree should contain: " + expected);
            return this;
        }

        public ParseResultAssert hasErrorContaining(String message) {
            boolean found = result.getErrors().stream()
                .anyMatch(err -> err.getMessage().contains(message));
            assertTrue(found, "Should have error containing: " + message);
            return this;
        }
    }

}
