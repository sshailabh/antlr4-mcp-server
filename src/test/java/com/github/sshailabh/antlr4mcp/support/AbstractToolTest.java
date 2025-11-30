package com.github.sshailabh.antlr4mcp.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Abstract base class for tool tests providing common functionality
 * and utilities for testing MCP tools.
 *
 * This class provides:
 * - Common test setup and utilities
 * - MCP request/response helpers
 * - JSON parsing and validation
 * - Mock exchange creation
 * - Assertion helpers for tool results
 *
 * Usage:
 * <pre>
 * class MyToolTest extends AbstractToolTest {
 *     private MyTool tool;
 *
 *     {@literal @}BeforeEach
 *     void setUp() {
 *         super.setUp();
 *         tool = new MyTool(dependencies, objectMapper);
 *     }
 *
 *     {@literal @}Test
 *     void testTool() {
 *         McpSchema.CallToolResult result = executeTool(tool, "tool_name",
 *             Map.of("param", "value"));
 *         assertToolSuccess(result);
 *     }
 * }
 * </pre>
 */
@SpringBootTest
public abstract class AbstractToolTest {

    @Autowired
    protected ObjectMapper objectMapper;

    protected McpSyncServerExchange mockExchange;

    @BeforeEach
    public void setUp() {
        mockExchange = mock(McpSyncServerExchange.class);
    }

    /**
     * Creates an MCP CallToolRequest with the given tool name and arguments
     */
    protected McpSchema.CallToolRequest createRequest(String toolName, Map<String, Object> arguments) {
        return new McpSchema.CallToolRequest(toolName, arguments);
    }

    /**
     * Creates an empty arguments map for convenience
     */
    protected Map<String, Object> createArguments() {
        return new HashMap<>();
    }

    /**
     * Extracts text content from CallToolResult
     */
    protected String getContentText(McpSchema.CallToolResult result) {
        assertNotNull(result, "Result should not be null");
        assertNotNull(result.content(), "Result content should not be null");
        assertFalse(result.content().isEmpty(), "Result content should not be empty");
        return ((McpSchema.TextContent) result.content().get(0)).text();
    }

    /**
     * Parses JSON content from result into specified type
     */
    protected <T> T parseResult(McpSchema.CallToolResult result, Class<T> valueType) throws Exception {
        String contentText = getContentText(result);
        return objectMapper.readValue(contentText, valueType);
    }

    /**
     * Asserts that the tool result is successful (no error flag)
     */
    protected void assertToolSuccess(McpSchema.CallToolResult result) {
        assertNotNull(result, "Result should not be null");
        assertFalse(result.isError(), "Result should not be an error");
        assertNotNull(result.content(), "Result content should not be null");
    }

    /**
     * Asserts that the tool schema is properly configured
     */
    protected void assertValidToolSchema(McpSchema.Tool tool, String expectedName, String... requiredParams) {
        assertNotNull(tool, "Tool schema should not be null");
        assertEquals(expectedName, tool.name(), "Tool name should match");
        assertNotNull(tool.description(), "Tool description should not be null");
        assertFalse(tool.description().isEmpty(), "Tool description should not be empty");

        McpSchema.JsonSchema schema = tool.inputSchema();
        assertNotNull(schema, "Input schema should not be null");
        assertEquals("object", schema.type(), "Schema type should be object");

        if (requiredParams.length > 0) {
            assertNotNull(schema.required(), "Required parameters should be defined");
            for (String param : requiredParams) {
                assertTrue(schema.required().contains(param),
                    "Required parameter '" + param + "' should be in required list");
            }
        }

        assertNotNull(schema.properties(), "Schema properties should not be null");
    }

    /**
     * Asserts that result content contains expected JSON fields
     */
    protected void assertContentContains(McpSchema.CallToolResult result, String... expectedFields) {
        String contentText = getContentText(result);
        for (String field : expectedFields) {
            assertTrue(contentText.contains(field),
                "Content should contain field: " + field);
        }
    }

    /**
     * Creates a builder for fluent argument creation
     */
    protected ArgumentsBuilder arguments() {
        return new ArgumentsBuilder();
    }

    /**
     * Fluent builder for creating tool arguments
     */
    protected static class ArgumentsBuilder {
        private final Map<String, Object> args = new HashMap<>();

        public ArgumentsBuilder with(String key, Object value) {
            args.put(key, value);
            return this;
        }

        public ArgumentsBuilder withGrammar(String grammarText) {
            args.put("grammar_text", grammarText);
            return this;
        }

        public ArgumentsBuilder withInput(String input) {
            args.put("sample_input", input);
            return this;
        }

        public ArgumentsBuilder withStartRule(String startRule) {
            args.put("start_rule", startRule);
            return this;
        }

        public Map<String, Object> build() {
            return new HashMap<>(args);
        }
    }
}
