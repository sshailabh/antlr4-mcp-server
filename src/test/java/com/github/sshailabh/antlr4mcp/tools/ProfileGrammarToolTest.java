package com.github.sshailabh.antlr4mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sshailabh.antlr4mcp.model.ProfileResult;
import com.github.sshailabh.antlr4mcp.service.GrammarCompiler;
import com.github.sshailabh.antlr4mcp.service.GrammarProfiler;
import com.github.sshailabh.antlr4mcp.security.SecurityValidator;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ProfileGrammarToolTest {

    private ProfileGrammarTool tool;
    private GrammarProfiler grammarProfiler;
    private ObjectMapper objectMapper;
    private McpSyncServerExchange mockExchange;

    private static final String SIMPLE_GRAMMAR = """
        grammar Simple;
        start: ID+ ;
        ID: [a-z]+ ;
        WS: [ \\t\\n\\r]+ -> skip ;
        """;

    private static final String EXPR_GRAMMAR = """
        grammar Expr;
        prog: expr+ ;
        expr: expr ('*'|'/') expr
            | expr ('+'|'-') expr
            | '(' expr ')'
            | NUMBER
            ;
        NUMBER: [0-9]+ ;
        WS: [ \\t\\n\\r]+ -> skip ;
        """;

    @BeforeEach
    void setUp() {
        SecurityValidator securityValidator = new SecurityValidator();
        GrammarCompiler grammarCompiler = new GrammarCompiler(securityValidator);
        ReflectionTestUtils.setField(grammarCompiler, "maxGrammarSizeMb", 10);
        
        grammarProfiler = new GrammarProfiler(grammarCompiler);
        objectMapper = new ObjectMapper();
        tool = new ProfileGrammarTool(grammarProfiler, objectMapper);
        mockExchange = mock(McpSyncServerExchange.class);
    }

    private String getContentText(McpSchema.CallToolResult result) {
        return ((McpSchema.TextContent) result.content().get(0)).text();
    }

    private McpSchema.CallToolRequest createRequest(Map<String, Object> args) {
        return new McpSchema.CallToolRequest("profile_grammar", args);
    }

    @Test
    void testToTool() {
        McpSchema.Tool mcpTool = tool.toTool();
        
        assertThat(mcpTool.name()).isEqualTo("profile_grammar");
        assertThat(mcpTool.description()).contains("Profile grammar performance");
    }

    @Test
    void testProfileSimpleGrammar() throws Exception {
        McpSchema.CallToolRequest request = createRequest(Map.of(
            "grammar_text", SIMPLE_GRAMMAR,
            "sample_input", "hello world test",
            "start_rule", "start"
        ));

        McpSchema.CallToolResult result = tool.execute(mockExchange, request);

        assertThat(result.isError()).isFalse();
        
        ProfileResult profile = objectMapper.readValue(getContentText(result), ProfileResult.class);
        
        assertThat(profile.isSuccess()).isTrue();
        assertThat(profile.getGrammarName()).isEqualTo("Simple");
        assertThat(profile.getTotalTimeNanos()).isGreaterThan(0);
        assertThat(profile.getDecisions()).isNotEmpty();
        assertThat(profile.getInsights()).isNotEmpty();
    }

    @Test
    void testProfileExpressionGrammar() throws Exception {
        McpSchema.CallToolRequest request = createRequest(Map.of(
            "grammar_text", EXPR_GRAMMAR,
            "sample_input", "1 + 2 * 3",
            "start_rule", "prog"
        ));

        McpSchema.CallToolResult result = tool.execute(mockExchange, request);

        assertThat(result.isError()).isFalse();
        
        ProfileResult profile = objectMapper.readValue(getContentText(result), ProfileResult.class);
        
        assertThat(profile.isSuccess()).isTrue();
        assertThat(profile.getGrammarName()).isEqualTo("Expr");
        assertThat(profile.getDecisions()).hasSizeGreaterThan(1);
        assertThat(profile.getInsights()).isNotEmpty();
    }

    @Test
    void testProfileInvalidStartRule() throws Exception {
        McpSchema.CallToolRequest request = createRequest(Map.of(
            "grammar_text", SIMPLE_GRAMMAR,
            "sample_input", "hello",
            "start_rule", "nonexistent"
        ));

        McpSchema.CallToolResult result = tool.execute(mockExchange, request);

        ProfileResult profile = objectMapper.readValue(getContentText(result), ProfileResult.class);
        
        assertThat(profile.isSuccess()).isFalse();
        assertThat(profile.getErrors()).isNotEmpty();
        assertThat(profile.getErrors().get(0).getMessage()).contains("nonexistent");
    }

    @Test
    void testProfileInvalidGrammar() throws Exception {
        McpSchema.CallToolRequest request = createRequest(Map.of(
            "grammar_text", "not a grammar",
            "sample_input", "test",
            "start_rule", "start"
        ));

        McpSchema.CallToolResult result = tool.execute(mockExchange, request);

        ProfileResult profile = objectMapper.readValue(getContentText(result), ProfileResult.class);
        
        assertThat(profile.isSuccess()).isFalse();
        assertThat(profile.getErrors()).isNotEmpty();
    }
}
