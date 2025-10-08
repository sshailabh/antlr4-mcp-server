package com.github.sshailabh.antlr4mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sshailabh.antlr4mcp.analysis.CallGraphAnalyzer;
import com.github.sshailabh.antlr4mcp.codegen.MultiTargetCompiler;
// ImportResolver removed - no longer needed
import com.github.sshailabh.antlr4mcp.infrastructure.resources.GrammarResource;
import com.github.sshailabh.antlr4mcp.infrastructure.resources.GrammarResourceProvider;
import com.github.sshailabh.antlr4mcp.service.*;
import com.github.sshailabh.antlr4mcp.tools.*;
import com.github.sshailabh.antlr4mcp.visualization.AtnVisualizer;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import java.util.List;

@Slf4j
@SpringBootApplication
public class AntlrMcpServerApplication {

    public static void main(String[] args) {
        log.info("ANTLR4 MCP Server starting...");
        ApplicationContext context = SpringApplication.run(AntlrMcpServerApplication.class, args);

        GrammarCompiler grammarCompiler = context.getBean(GrammarCompiler.class);
        GrammarInterpreter grammarInterpreter = context.getBean(GrammarInterpreter.class);
        ErrorTransformer errorTransformer = context.getBean(ErrorTransformer.class);

        // Core services for essential tools
        RuntimeAmbiguityDetector runtimeAmbiguityDetector = context.getBean(RuntimeAmbiguityDetector.class);
        GrammarResourceProvider resourceProvider = context.getBean(GrammarResourceProvider.class);
        MultiTargetCompiler multiTargetCompiler = context.getBean(MultiTargetCompiler.class);
        AtnVisualizer atnVisualizer = context.getBean(AtnVisualizer.class);
        CallGraphAnalyzer callGraphAnalyzer = context.getBean(CallGraphAnalyzer.class);
        GrammarComplexityAnalyzer grammarComplexityAnalyzer = context.getBean(GrammarComplexityAnalyzer.class);
        LeftRecursionAnalyzer leftRecursionAnalyzer = context.getBean(LeftRecursionAnalyzer.class);
        DecisionVisualizer decisionVisualizer = context.getBean(DecisionVisualizer.class);
        TestInputGenerator testInputGenerator = context.getBean(TestInputGenerator.class);
        ObjectMapper objectMapper = context.getBean(ObjectMapper.class);

        // Essential tools (8 tools)
        ValidateGrammarTool validateGrammarTool = new ValidateGrammarTool(grammarInterpreter, errorTransformer, objectMapper);
        ParseSampleTool parseSampleTool = new ParseSampleTool(grammarInterpreter, errorTransformer, objectMapper);
        DetectAmbiguityTool detectAmbiguityTool = new DetectAmbiguityTool(runtimeAmbiguityDetector, objectMapper);

        CompileGrammarMultiTargetTool compileMultiTargetTool = new CompileGrammarMultiTargetTool(
            multiTargetCompiler, objectMapper
        );
        VisualizeAtnTool visualizeAtnTool = new VisualizeAtnTool(
            grammarCompiler, atnVisualizer, objectMapper
        );
        AnalyzeCallGraphTool analyzeCallGraphTool = new AnalyzeCallGraphTool(
            callGraphAnalyzer, objectMapper
        );

        // Removed redundant tools: ProfileGrammarTool, VisualizeAmbiguitiesTool, ParseWithTraceTool
        AnalyzeComplexityTool analyzeComplexityTool = new AnalyzeComplexityTool(
            grammarComplexityAnalyzer, objectMapper
        );
        AnalyzeLeftRecursionTool analyzeLeftRecursionTool = new AnalyzeLeftRecursionTool(
            leftRecursionAnalyzer, objectMapper
        );
        VisualizeDfaTool visualizeDfaTool = new VisualizeDfaTool(
            decisionVisualizer, objectMapper
        );
        GenerateTestInputsTool generateTestInputsTool = new GenerateTestInputsTool(
            testInputGenerator, objectMapper
        );

        var jsonMapper = new JacksonMcpJsonMapper(objectMapper);
        var transportProvider = new StdioServerTransportProvider(jsonMapper);

        // Get available grammar resources
        List<GrammarResource> grammarResources = resourceProvider.listResources();
        log.info("Found {} grammar resources", grammarResources.size());

        // Build MCP server with tools (resources TBD in future update)
        McpSyncServer mcpServer = McpServer.sync(transportProvider)
            .serverInfo("antlr4-mcp-server", "0.2.0")
            // Core Tools (Essential - 8 tools)
            .toolCall(validateGrammarTool.toTool(), validateGrammarTool::execute)
            .toolCall(parseSampleTool.toTool(), parseSampleTool::execute)
            .toolCall(detectAmbiguityTool.toTool(), detectAmbiguityTool::execute)
            .toolCall(analyzeCallGraphTool.toTool(), analyzeCallGraphTool::execute)
            .toolCall(analyzeComplexityTool.toTool(), analyzeComplexityTool::execute)
            .toolCall(analyzeLeftRecursionTool.toTool(), analyzeLeftRecursionTool::execute)
            .toolCall(compileMultiTargetTool.toTool(), compileMultiTargetTool::execute)
            .toolCall(generateTestInputsTool.toTool(), generateTestInputsTool::execute)
            // Advanced Tools (Specialized - 2 tools)
            .toolCall(visualizeAtnTool.toTool(), visualizeAtnTool::execute)
            .toolCall(visualizeDfaTool.toTool(), visualizeDfaTool::execute)
            .immediateExecution(true)
            .build();

        log.info("ANTLR4 MCP Server initialized successfully with 10 tools (optimized)");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down ANTLR4 MCP Server...");
            mcpServer.close();
        }));
    }

    @Bean
    public CommandLineRunner startupRunner() {
        return args -> {
            log.info("Spring Boot initialization complete");
        };
    }
}
