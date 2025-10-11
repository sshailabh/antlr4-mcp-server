package com.github.sshailabh.antlr4mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sshailabh.antlr4mcp.analysis.CallGraphAnalyzer;
import com.github.sshailabh.antlr4mcp.codegen.MultiTargetCompiler;
import com.github.sshailabh.antlr4mcp.infrastructure.imports.ImportResolver;
import com.github.sshailabh.antlr4mcp.service.*;
import com.github.sshailabh.antlr4mcp.tools.*;
import com.github.sshailabh.antlr4mcp.visualization.AtnVisualizer;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import java.io.OutputStream;
import java.io.PrintStream;

/**
 * ANTLR4 MCP Server - Production Ready v0.2.0
 * 
 * High-performance Model Context Protocol server providing ANTLR4 compiler capabilities
 * to LLM clients via stdio transport. Implements 10 specialized tools for grammar
 * analysis, validation, parsing, and code generation.
 * 
 * @author Senior Compiler Engineering Team
 * @version 0.2.0
 */
@Slf4j
@SpringBootApplication
public class AntlrMcpServerApplication {

    private static final String VERSION = "0.2.0";
    private static final String SERVER_NAME = "antlr4-mcp-server";
    
    public static void main(String[] args) {
        suppressStderr();
        
        ApplicationContext context = SpringApplication.run(AntlrMcpServerApplication.class, args);
        
        initializeMcpServer(context);
    }
    
    private static void suppressStderr() {
        System.setErr(new PrintStream(new OutputStream() {
            @Override public void write(int b) {}
        }));
    }
    
    private static void initializeMcpServer(ApplicationContext context) {
        ObjectMapper mapper = context.getBean(ObjectMapper.class);
        
        McpSyncServer server = buildServer(context, mapper);
        
        log.info("ANTLR4 MCP Server v{} initialized - 10 tools registered", VERSION);
        log.info("Server ready for MCP client connections via stdio");
        
        registerShutdownHook(server);
        keepAlive();
    }
    
    private static McpSyncServer buildServer(ApplicationContext context, ObjectMapper mapper) {
        var transport = new StdioServerTransportProvider(new JacksonMcpJsonMapper(mapper));
        
        var validateTool = createValidateTool(context, mapper);
        var parseTool = createParseTool(context, mapper);
        var ambiguityTool = createAmbiguityTool(context, mapper);
        var visualizeTool = createVisualizeTool(context, mapper);
        var contextTool = createContextTool(context, mapper);
        var compileTool = createCompileTool(context, mapper);
        var atnTool = createAtnTool(context, mapper);
        var callGraphTool = createCallGraphTool(context, mapper);
        var profileTool = createProfileTool(context, mapper);
        var ambiguityVisualizeTool = createAmbiguityVisualizeTool(context, mapper);
        
        return McpServer.sync(transport)
            .serverInfo(SERVER_NAME, VERSION)
            .toolCall(validateTool.toTool(), validateTool::execute)
            .toolCall(parseTool.toTool(), parseTool::execute)
            .toolCall(ambiguityTool.toTool(), ambiguityTool::execute)
            .toolCall(visualizeTool.toTool(), visualizeTool::execute)
            .toolCall(contextTool.toTool(), contextTool::execute)
            .toolCall(compileTool.toTool(), compileTool::execute)
            .toolCall(atnTool.toTool(), atnTool::execute)
            .toolCall(callGraphTool.toTool(), callGraphTool::execute)
            .toolCall(profileTool.toTool(), profileTool::execute)
            .toolCall(ambiguityVisualizeTool.toTool(), ambiguityVisualizeTool::execute)
            .immediateExecution(true)
            .build();
    }
    
    private static ValidateGrammarTool createValidateTool(ApplicationContext ctx, ObjectMapper mapper) {
        return new ValidateGrammarTool(ctx.getBean(GrammarCompiler.class), mapper);
    }
    
    private static ParseSampleTool createParseTool(ApplicationContext ctx, ObjectMapper mapper) {
        return new ParseSampleTool(ctx.getBean(GrammarCompiler.class), mapper);
    }
    
    private static DetectAmbiguityTool createAmbiguityTool(ApplicationContext ctx, ObjectMapper mapper) {
        return new DetectAmbiguityTool(ctx.getBean(AmbiguityDetector.class), mapper);
    }
    
    private static VisualizeRuleTool createVisualizeTool(ApplicationContext ctx, ObjectMapper mapper) {
        return new VisualizeRuleTool(ctx.getBean(TreeVisualizer.class), mapper);
    }
    
    private static BuildLanguageContextTool createContextTool(ApplicationContext ctx, ObjectMapper mapper) {
        return new BuildLanguageContextTool(
            ctx.getBean(GrammarCompiler.class),
            ctx.getBean(ImportResolver.class),
            mapper
        );
    }
    
    private static CompileGrammarMultiTargetTool createCompileTool(ApplicationContext ctx, ObjectMapper mapper) {
        return new CompileGrammarMultiTargetTool(ctx.getBean(MultiTargetCompiler.class), mapper);
    }
    
    private static VisualizeAtnTool createAtnTool(ApplicationContext ctx, ObjectMapper mapper) {
        return new VisualizeAtnTool(
            ctx.getBean(GrammarCompiler.class),
            ctx.getBean(AtnVisualizer.class),
            mapper
        );
    }
    
    private static AnalyzeCallGraphTool createCallGraphTool(ApplicationContext ctx, ObjectMapper mapper) {
        return new AnalyzeCallGraphTool(ctx.getBean(CallGraphAnalyzer.class), mapper);
    }
    
    private static ProfileGrammarTool createProfileTool(ApplicationContext ctx, ObjectMapper mapper) {
        return new ProfileGrammarTool(ctx.getBean(GrammarProfiler.class), mapper);
    }
    
    private static VisualizeAmbiguitiesTool createAmbiguityVisualizeTool(ApplicationContext ctx, ObjectMapper mapper) {
        return new VisualizeAmbiguitiesTool(ctx.getBean(AmbiguityVisualizer.class), mapper);
    }
    
    private static void registerShutdownHook(McpSyncServer server) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down ANTLR4 MCP Server v{}...", VERSION);
            try {
                server.close();
                log.info("Shutdown complete");
            } catch (Exception e) {
                log.error("Shutdown error", e);
            }
        }));
    }
    
    private static void keepAlive() {
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            log.info("Server interrupted");
            Thread.currentThread().interrupt();
        }
    }
}
