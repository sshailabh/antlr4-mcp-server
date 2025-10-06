package com.github.sshailabh.antlr4mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sshailabh.antlr4mcp.analysis.CallGraphAnalyzer;
import com.github.sshailabh.antlr4mcp.codegen.MultiTargetCompiler;
import com.github.sshailabh.antlr4mcp.infrastructure.imports.ImportResolver;
import com.github.sshailabh.antlr4mcp.infrastructure.resources.GrammarResource;
import com.github.sshailabh.antlr4mcp.infrastructure.resources.GrammarResourceProvider;
import com.github.sshailabh.antlr4mcp.service.AmbiguityDetector;
import com.github.sshailabh.antlr4mcp.service.AmbiguityVisualizer;
import com.github.sshailabh.antlr4mcp.service.GrammarCompiler;
import com.github.sshailabh.antlr4mcp.service.GrammarProfiler;
import com.github.sshailabh.antlr4mcp.service.ParseTracer;
import com.github.sshailabh.antlr4mcp.service.TreeVisualizer;
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
        AmbiguityDetector ambiguityDetector = context.getBean(AmbiguityDetector.class);
        AmbiguityVisualizer ambiguityVisualizer = context.getBean(AmbiguityVisualizer.class);
        TreeVisualizer treeVisualizer = context.getBean(TreeVisualizer.class);
        ParseTracer parseTracer = context.getBean(ParseTracer.class);
        ImportResolver importResolver = context.getBean(ImportResolver.class);
        GrammarResourceProvider resourceProvider = context.getBean(GrammarResourceProvider.class);
        MultiTargetCompiler multiTargetCompiler = context.getBean(MultiTargetCompiler.class);
        AtnVisualizer atnVisualizer = context.getBean(AtnVisualizer.class);
        CallGraphAnalyzer callGraphAnalyzer = context.getBean(CallGraphAnalyzer.class);
        GrammarProfiler grammarProfiler = context.getBean(GrammarProfiler.class);
        ObjectMapper objectMapper = context.getBean(ObjectMapper.class);

        ValidateGrammarTool validateGrammarTool = new ValidateGrammarTool(grammarCompiler, objectMapper);
        ParseSampleTool parseSampleTool = new ParseSampleTool(grammarCompiler, objectMapper);
        DetectAmbiguityTool detectAmbiguityTool = new DetectAmbiguityTool(ambiguityDetector, objectMapper);
        VisualizeRuleTool visualizeRuleTool = new VisualizeRuleTool(treeVisualizer, objectMapper);

        BuildLanguageContextTool buildContextTool = new BuildLanguageContextTool(
            grammarCompiler, importResolver, objectMapper
        );

        CompileGrammarMultiTargetTool compileMultiTargetTool = new CompileGrammarMultiTargetTool(
            multiTargetCompiler, objectMapper
        );
        VisualizeAtnTool visualizeAtnTool = new VisualizeAtnTool(
            grammarCompiler, atnVisualizer, objectMapper
        );
        AnalyzeCallGraphTool analyzeCallGraphTool = new AnalyzeCallGraphTool(
            callGraphAnalyzer, objectMapper
        );

        ProfileGrammarTool profileGrammarTool = new ProfileGrammarTool(
            grammarProfiler, objectMapper
        );
        VisualizeAmbiguitiesTool visualizeAmbiguitiesTool = new VisualizeAmbiguitiesTool(
            ambiguityVisualizer, objectMapper
        );
        ParseWithTraceTool parseWithTraceTool = new ParseWithTraceTool(
            parseTracer, objectMapper
        );

        var jsonMapper = new JacksonMcpJsonMapper(objectMapper);
        var transportProvider = new StdioServerTransportProvider(jsonMapper);

        // Get available grammar resources
        List<GrammarResource> grammarResources = resourceProvider.listResources();
        log.info("Found {} grammar resources", grammarResources.size());

        // Build MCP server with tools (resources TBD in future update)
        McpSyncServer mcpServer = McpServer.sync(transportProvider)
            .serverInfo("antlr4-mcp-server", "0.1.0")
            .toolCall(validateGrammarTool.toTool(), validateGrammarTool::execute)
            .toolCall(parseSampleTool.toTool(), parseSampleTool::execute)
            .toolCall(detectAmbiguityTool.toTool(), detectAmbiguityTool::execute)
            .toolCall(visualizeRuleTool.toTool(), visualizeRuleTool::execute)
            // M2 Tools
            .toolCall(buildContextTool.toTool(), buildContextTool::execute)
            // M3 Tools
            .toolCall(compileMultiTargetTool.toTool(), compileMultiTargetTool::execute)
            .toolCall(visualizeAtnTool.toTool(), visualizeAtnTool::execute)
            .toolCall(analyzeCallGraphTool.toTool(), analyzeCallGraphTool::execute)
            // M3.1 Tools (Debugging)
            .toolCall(profileGrammarTool.toTool(), profileGrammarTool::execute)
            .toolCall(visualizeAmbiguitiesTool.toTool(), visualizeAmbiguitiesTool::execute)
            .toolCall(parseWithTraceTool.toTool(), parseWithTraceTool::execute)
            .immediateExecution(true)
            .build();

        log.info("ANTLR4 MCP Server initialized successfully with 11 tools");

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
