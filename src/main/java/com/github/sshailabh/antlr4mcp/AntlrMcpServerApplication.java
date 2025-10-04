package com.github.sshailabh.antlr4mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sshailabh.antlr4mcp.service.AmbiguityDetector;
import com.github.sshailabh.antlr4mcp.service.GrammarCompiler;
import com.github.sshailabh.antlr4mcp.service.TreeVisualizer;
import com.github.sshailabh.antlr4mcp.tools.DetectAmbiguityTool;
import com.github.sshailabh.antlr4mcp.tools.ParseSampleTool;
import com.github.sshailabh.antlr4mcp.tools.ValidateGrammarTool;
import com.github.sshailabh.antlr4mcp.tools.VisualizeRuleTool;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@Slf4j
@SpringBootApplication
public class AntlrMcpServerApplication {

    public static void main(String[] args) {
        log.info("ANTLR4 MCP Server starting...");
        ApplicationContext context = SpringApplication.run(AntlrMcpServerApplication.class, args);

        GrammarCompiler grammarCompiler = context.getBean(GrammarCompiler.class);
        AmbiguityDetector ambiguityDetector = context.getBean(AmbiguityDetector.class);
        TreeVisualizer treeVisualizer = context.getBean(TreeVisualizer.class);
        ObjectMapper objectMapper = context.getBean(ObjectMapper.class);

        ValidateGrammarTool validateGrammarTool = new ValidateGrammarTool(grammarCompiler, objectMapper);
        ParseSampleTool parseSampleTool = new ParseSampleTool(grammarCompiler, objectMapper);
        DetectAmbiguityTool detectAmbiguityTool = new DetectAmbiguityTool(ambiguityDetector, objectMapper);
        VisualizeRuleTool visualizeRuleTool = new VisualizeRuleTool(treeVisualizer, objectMapper);

        var jsonMapper = new JacksonMcpJsonMapper(objectMapper);
        var transportProvider = new StdioServerTransportProvider(jsonMapper);

        McpSyncServer mcpServer = McpServer.sync(transportProvider)
            .serverInfo("antlr4-mcp-server", "0.1.0-M1")
            .toolCall(validateGrammarTool.toTool(), validateGrammarTool::execute)
            .toolCall(parseSampleTool.toTool(), parseSampleTool::execute)
            .toolCall(detectAmbiguityTool.toTool(), detectAmbiguityTool::execute)
            .toolCall(visualizeRuleTool.toTool(), visualizeRuleTool::execute)
            .immediateExecution(true)
            .build();

        log.info("ANTLR4 MCP Server initialized successfully with {} tools", 4);

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
