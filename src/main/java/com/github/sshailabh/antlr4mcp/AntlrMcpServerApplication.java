package com.github.sshailabh.antlr4mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sshailabh.antlr4mcp.analysis.CallGraphAnalyzer;
import com.github.sshailabh.antlr4mcp.codegen.MultiTargetCompiler;
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
 * ANTLR4 MCP Server v0.2.0
 * 
 * Model Context Protocol server providing ANTLR4 grammar analysis capabilities
 * to LLMs for compiler development. Uses interpreter mode for fast parsing.
 * 
 * Tools (9 total):
 * 1. validate_grammar       - Syntax validation with detailed errors
 * 2. parse_sample           - Parse input, return tree (fast interpreter mode)
 * 3. detect_ambiguity       - Detect parsing ambiguities
 * 4. analyze_left_recursion - Detect left recursion patterns
 * 5. analyze_first_follow   - Compute FIRST/FOLLOW sets
 * 6. analyze_call_graph     - Rule dependency analysis
 * 7. visualize_atn          - ATN state machine diagrams
 * 8. compile_grammar        - Code generation for 10 targets
 * 9. profile_grammar        - Performance profiling with optimization hints
 */
@Slf4j
@SpringBootApplication
public class AntlrMcpServerApplication {

    private static final String VERSION = "0.2.0";
    private static final String SERVER_NAME = "antlr4-mcp-server";
    private static final int TOOL_COUNT = 9;
    
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
        
        log.info("ANTLR4 MCP Server v{} - {} tools", VERSION, TOOL_COUNT);
        
        registerShutdownHook(server);
        keepAlive();
    }
    
    private static McpSyncServer buildServer(ApplicationContext context, ObjectMapper mapper) {
        var transport = new StdioServerTransportProvider(new JacksonMcpJsonMapper(mapper));
        
        // Get services
        var grammarCompiler = context.getBean(GrammarCompiler.class);
        var ambiguityDetector = context.getBean(AmbiguityDetector.class);
        var leftRecursionAnalyzer = context.getBean(LeftRecursionAnalyzer.class);
        var firstFollowAnalyzer = context.getBean(FirstFollowAnalyzer.class);
        var callGraphAnalyzer = context.getBean(CallGraphAnalyzer.class);
        var atnVisualizer = context.getBean(AtnVisualizer.class);
        var multiTargetCompiler = context.getBean(MultiTargetCompiler.class);
        var grammarProfiler = context.getBean(GrammarProfiler.class);
        
        // Create tools
        var validateTool = new ValidateGrammarTool(grammarCompiler, mapper);
        var parseTool = new ParseSampleTool(grammarCompiler, mapper);
        var ambiguityTool = new DetectAmbiguityTool(ambiguityDetector, mapper);
        var leftRecursionTool = new AnalyzeLeftRecursionTool(leftRecursionAnalyzer, mapper);
        var firstFollowTool = new AnalyzeFirstFollowTool(firstFollowAnalyzer, mapper);
        var callGraphTool = new AnalyzeCallGraphTool(callGraphAnalyzer, mapper);
        var atnTool = new VisualizeAtnTool(grammarCompiler, atnVisualizer, mapper);
        var compileTool = new CompileGrammarMultiTargetTool(multiTargetCompiler, mapper);
        var profileTool = new ProfileGrammarTool(grammarProfiler, mapper);
        
        return McpServer.sync(transport)
            .serverInfo(SERVER_NAME, VERSION)
            .toolCall(validateTool.toTool(), validateTool::execute)
            .toolCall(parseTool.toTool(), parseTool::execute)
            .toolCall(ambiguityTool.toTool(), ambiguityTool::execute)
            .toolCall(leftRecursionTool.toTool(), leftRecursionTool::execute)
            .toolCall(firstFollowTool.toTool(), firstFollowTool::execute)
            .toolCall(callGraphTool.toTool(), callGraphTool::execute)
            .toolCall(atnTool.toTool(), atnTool::execute)
            .toolCall(compileTool.toTool(), compileTool::execute)
            .toolCall(profileTool.toTool(), profileTool::execute)
            .immediateExecution(true)
            .build();
    }
    
    private static void registerShutdownHook(McpSyncServer server) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down...");
            try {
                server.close();
            } catch (Exception e) {
                log.error("Shutdown error", e);
            }
        }));
    }
    
    private static void keepAlive() {
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
