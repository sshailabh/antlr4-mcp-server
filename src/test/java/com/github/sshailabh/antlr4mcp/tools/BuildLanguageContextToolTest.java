package com.github.sshailabh.antlr4mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sshailabh.antlr4mcp.config.AntlrMcpProperties;
import com.github.sshailabh.antlr4mcp.infrastructure.cache.GrammarCacheManager;
import com.github.sshailabh.antlr4mcp.infrastructure.imports.ImportResolver;
import com.github.sshailabh.antlr4mcp.infrastructure.resources.FileSystemService;
import com.github.sshailabh.antlr4mcp.security.PathValidator;
import com.github.sshailabh.antlr4mcp.security.ResourceManager;
import com.github.sshailabh.antlr4mcp.security.SecurityValidator;
import com.github.sshailabh.antlr4mcp.service.GrammarCompiler;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BuildLanguageContextToolTest {

    private BuildLanguageContextTool buildLanguageContextTool;
    private GrammarCompiler grammarCompiler;
    private ImportResolver importResolver;
    private ObjectMapper objectMapper;
    private McpSyncServerExchange mockExchange;

    @BeforeEach
    void setUp() {
        SecurityValidator securityValidator = new SecurityValidator();
        ResourceManager resourceManager = new ResourceManager();
        grammarCompiler = new GrammarCompiler(securityValidator, resourceManager);
        ReflectionTestUtils.setField(grammarCompiler, "maxGrammarSizeMb", 10);

        // Setup ImportResolver with mocks
        AntlrMcpProperties mockProperties = mock(AntlrMcpProperties.class);
        FileSystemService mockFileSystemService = mock(FileSystemService.class);
        PathValidator mockPathValidator = mock(PathValidator.class);
        GrammarCacheManager mockCacheManager = mock(GrammarCacheManager.class);

        AntlrMcpProperties.FeaturesProperties featuresProps = new AntlrMcpProperties.FeaturesProperties();
        featuresProps.setImportResolution(true);
        when(mockProperties.getFeatures()).thenReturn(featuresProps);

        AntlrMcpProperties.SecurityProperties securityProps = new AntlrMcpProperties.SecurityProperties();
        securityProps.setMaxImportDepth(10);
        when(mockProperties.getSecurity()).thenReturn(securityProps);

        AntlrMcpProperties.ResourcesProperties resourcesProps = new AntlrMcpProperties.ResourcesProperties();
        resourcesProps.setEnabled(true);
        resourcesProps.setAllowedPaths(List.of("/tmp"));
        when(mockProperties.getResources()).thenReturn(resourcesProps);

        importResolver = new ImportResolver(mockProperties, mockFileSystemService, mockPathValidator, mockCacheManager);

        objectMapper = new ObjectMapper();
        buildLanguageContextTool = new BuildLanguageContextTool(grammarCompiler, importResolver, objectMapper);
        mockExchange = mock(McpSyncServerExchange.class);
    }

    private String getContentText(McpSchema.CallToolResult result) {
        return ((McpSchema.TextContent) result.content().get(0)).text();
    }

    @Test
    void testToTool() {
        McpSchema.Tool tool = buildLanguageContextTool.toTool();

        assertNotNull(tool);
        assertEquals("build_language_context", tool.name());
        assertNotNull(tool.description());
        assertTrue(tool.description().contains("language context"));
        assertNotNull(tool.inputSchema());
    }

    @Test
    void testBuildContextForSimpleGrammar() throws Exception {
        String grammar = "grammar Simple;\n" +
                        "start : 'hello' ;\n";

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammarText", grammar);

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("build_language_context", arguments);
        McpSchema.CallToolResult result = buildLanguageContextTool.execute(mockExchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        @SuppressWarnings("unchecked")
        Map<String, Object> context = objectMapper.readValue(contentText, Map.class);
        assertNotNull(context);
        assertTrue((Boolean) context.get("success"));

        @SuppressWarnings("unchecked")
        Map<String, Object> grammarInfo = (Map<String, Object>) context.get("grammar");
        assertEquals("Simple", grammarInfo.get("name"));
        assertEquals("parser", grammarInfo.get("type"));  // Only parser rules, no lexer rules
        assertEquals(1, grammarInfo.get("parserRuleCount"));
        assertEquals(0, grammarInfo.get("lexerRuleCount"));
    }

    @Test
    void testBuildContextForCalcGrammar() throws Exception {
        Path grammarPath = Paths.get("src/test/resources/grammars/SimpleCalc.g4");
        String grammar = Files.readString(grammarPath);

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammarText", grammar);

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("build_language_context", arguments);
        McpSchema.CallToolResult result = buildLanguageContextTool.execute(mockExchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        @SuppressWarnings("unchecked")
        Map<String, Object> context = objectMapper.readValue(contentText, Map.class);
        assertNotNull(context);
        assertTrue((Boolean) context.get("success"));

        @SuppressWarnings("unchecked")
        Map<String, Object> grammarInfo = (Map<String, Object>) context.get("grammar");
        assertEquals("SimpleCalc", grammarInfo.get("name"));
        assertEquals(3, grammarInfo.get("parserRuleCount"));
        assertEquals(2, grammarInfo.get("lexerRuleCount"));

        // Check rules
        @SuppressWarnings("unchecked")
        Map<String, Object> rules = (Map<String, Object>) context.get("rules");
        assertNotNull(rules);

        @SuppressWarnings("unchecked")
        List<String> parserRules = (List<String>) rules.get("parser");
        assertTrue(parserRules.contains("expr"));
        assertTrue(parserRules.contains("term"));
        assertTrue(parserRules.contains("factor"));

        // Check rule dependencies
        @SuppressWarnings("unchecked")
        Map<String, Object> ruleDependencies = (Map<String, Object>) context.get("ruleDependencies");
        assertNotNull(ruleDependencies);
    }

    @Test
    void testBuildContextForJsonGrammar() throws Exception {
        Path grammarPath = Paths.get("src/test/resources/grammars/Json.g4");
        String grammar = Files.readString(grammarPath);

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammarText", grammar);

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("build_language_context", arguments);
        McpSchema.CallToolResult result = buildLanguageContextTool.execute(mockExchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        @SuppressWarnings("unchecked")
        Map<String, Object> context = objectMapper.readValue(contentText, Map.class);
        assertNotNull(context);
        assertTrue((Boolean) context.get("success"));

        @SuppressWarnings("unchecked")
        Map<String, Object> grammarInfo = (Map<String, Object>) context.get("grammar");
        assertEquals("Json", grammarInfo.get("name"));
        assertEquals("combined", grammarInfo.get("type"));

        // Check analysis
        @SuppressWarnings("unchecked")
        Map<String, Object> analysis = (Map<String, Object>) context.get("analysis");
        assertNotNull(analysis);
        assertNotNull(analysis.get("complexity"));
        assertNotNull(analysis.get("maxDependencyDepth"));
        assertNotNull(analysis.get("cyclicDependencies"));
    }

    @Test
    void testBuildContextForLexerGrammar() throws Exception {
        Path grammarPath = Paths.get("src/test/resources/grammars/CommonLexer.g4");
        String grammar = Files.readString(grammarPath);

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammarText", grammar);

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("build_language_context", arguments);
        McpSchema.CallToolResult result = buildLanguageContextTool.execute(mockExchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        @SuppressWarnings("unchecked")
        Map<String, Object> context = objectMapper.readValue(contentText, Map.class);
        assertNotNull(context);
        assertTrue((Boolean) context.get("success"));

        @SuppressWarnings("unchecked")
        Map<String, Object> grammarInfo = (Map<String, Object>) context.get("grammar");
        assertEquals("CommonLexer", grammarInfo.get("name"));
        assertEquals("lexer", grammarInfo.get("type"));
        assertEquals(0, grammarInfo.get("parserRuleCount"));
        assertTrue((Integer) grammarInfo.get("lexerRuleCount") >= 5);
    }

    @Test
    void testBuildContextWithInvalidGrammar() throws Exception {
        String grammar = "grammar Invalid;\n" +
                        "start : undefined_rule ;\n";

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammarText", grammar);

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("build_language_context", arguments);
        McpSchema.CallToolResult result = buildLanguageContextTool.execute(mockExchange, request);

        assertNotNull(result);
        // Invalid grammar should return error

        String contentText = getContentText(result);
        @SuppressWarnings("unchecked")
        Map<String, Object> context = objectMapper.readValue(contentText, Map.class);
        assertNotNull(context);
        assertFalse((Boolean) context.get("success"));
    }

    @Test
    void testBuildContextWithoutImports() throws Exception {
        Path grammarPath = Paths.get("src/test/resources/grammars/SimpleCalc.g4");
        String grammar = Files.readString(grammarPath);

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammarText", grammar);
        arguments.put("includeImports", false);

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("build_language_context", arguments);
        McpSchema.CallToolResult result = buildLanguageContextTool.execute(mockExchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        @SuppressWarnings("unchecked")
        Map<String, Object> context = objectMapper.readValue(contentText, Map.class);
        assertNotNull(context);
        assertTrue((Boolean) context.get("success"));
    }

    @Test
    void testBuildContextRuleDependencies() throws Exception {
        Path grammarPath = Paths.get("src/test/resources/grammars/SimpleCalc.g4");
        String grammar = Files.readString(grammarPath);

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammarText", grammar);

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("build_language_context", arguments);
        McpSchema.CallToolResult result = buildLanguageContextTool.execute(mockExchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        @SuppressWarnings("unchecked")
        Map<String, Object> context = objectMapper.readValue(contentText, Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> ruleDependencies = (Map<String, Object>) context.get("ruleDependencies");
        assertNotNull(ruleDependencies);

        // expr depends on term
        @SuppressWarnings("unchecked")
        List<String> exprDeps = (List<String>) ruleDependencies.get("expr");
        assertNotNull(exprDeps);
        assertTrue(exprDeps.contains("term"));

        // term depends on factor
        @SuppressWarnings("unchecked")
        List<String> termDeps = (List<String>) ruleDependencies.get("term");
        assertNotNull(termDeps);
        assertTrue(termDeps.contains("factor"));
    }

    @Test
    void testBuildContextComplexityAnalysis() throws Exception {
        Path grammarPath = Paths.get("src/test/resources/grammars/Json.g4");
        String grammar = Files.readString(grammarPath);

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammarText", grammar);

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("build_language_context", arguments);
        McpSchema.CallToolResult result = buildLanguageContextTool.execute(mockExchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        @SuppressWarnings("unchecked")
        Map<String, Object> context = objectMapper.readValue(contentText, Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> analysis = (Map<String, Object>) context.get("analysis");
        assertNotNull(analysis);

        Integer complexity = (Integer) analysis.get("complexity");
        assertNotNull(complexity);
        assertTrue(complexity >= 0);

        Integer maxDepth = (Integer) analysis.get("maxDependencyDepth");
        assertNotNull(maxDepth);
        assertTrue(maxDepth >= 0);

        Boolean hasCycles = (Boolean) analysis.get("cyclicDependencies");
        assertNotNull(hasCycles);
    }

    @Test
    void testBuildContextWithPath() throws Exception {
        Path grammarPath = Paths.get("src/test/resources/grammars/SimpleCalc.g4");
        String grammar = Files.readString(grammarPath);

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammarText", grammar);
        arguments.put("grammarPath", grammarPath.toString());

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("build_language_context", arguments);
        McpSchema.CallToolResult result = buildLanguageContextTool.execute(mockExchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        @SuppressWarnings("unchecked")
        Map<String, Object> context = objectMapper.readValue(contentText, Map.class);
        assertNotNull(context);
        assertTrue((Boolean) context.get("success"));
    }

    @Test
    void testBuildContextEmptyGrammar() throws Exception {
        String grammar = "";

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammarText", grammar);

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("build_language_context", arguments);
        McpSchema.CallToolResult result = buildLanguageContextTool.execute(mockExchange, request);

        assertNotNull(result);
        // Empty grammar should return error

        String contentText = getContentText(result);
        @SuppressWarnings("unchecked")
        Map<String, Object> context = objectMapper.readValue(contentText, Map.class);
        assertNotNull(context);
        assertFalse((Boolean) context.get("success"));
    }
}
