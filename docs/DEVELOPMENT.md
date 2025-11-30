# ANTLR4 MCP Server - Development Guide

**Version**: 0.2.0 | **Tests**: 240 passing

## Prerequisites

- **Java 17+** required
- Maven 3.6+ (via Maven Wrapper `./mvnw`)
- Git

### Java Version Management

```bash
# Using SDKMAN
sdk install java 21.0.5-amzn
sdk use java 21.0.5-amzn

# Verify
java -version
# Should show: Java 17 or higher
```

## Project Structure

```
antlr4-mcp-server/
├── src/main/java/com/github/sshailabh/antlr4mcp/
│   ├── AntlrMcpServerApplication.java  # Entry point
│   ├── tools/                          # 9 MCP tool implementations
│   │   ├── ValidateGrammarTool.java
│   │   ├── ParseSampleTool.java
│   │   ├── DetectAmbiguityTool.java
│   │   ├── AnalyzeLeftRecursionTool.java
│   │   ├── AnalyzeFirstFollowTool.java
│   │   ├── AnalyzeCallGraphTool.java
│   │   ├── VisualizeAtnTool.java
│   │   ├── CompileGrammarMultiTargetTool.java
│   │   └── ProfileGrammarTool.java
│   ├── service/                        # Core services
│   │   ├── GrammarCompiler.java        # Grammar loading/validation
│   │   ├── InterpreterParser.java      # Fast parsing (interpreter mode)
│   │   ├── AmbiguityDetector.java      # Ambiguity detection
│   │   ├── LeftRecursionAnalyzer.java  # Left recursion analysis
│   │   ├── FirstFollowAnalyzer.java    # FIRST/FOLLOW sets
│   │   └── GrammarProfiler.java        # Performance profiling
│   ├── analysis/                       # Call graph analysis
│   ├── codegen/                        # Multi-target code generation
│   ├── visualization/                  # ATN visualization
│   ├── model/                          # DTOs (9 result types)
│   ├── security/                       # Input validation
│   └── config/                         # Spring configuration
├── src/main/resources/
│   └── application.yml                 # Configuration
├── src/test/java/                      # Test suite (240 tests)
└── src/test/resources/grammars/        # Test grammars
```

## Building and Testing

### Full Build

```bash
# Clean build with tests
./mvnw clean package

# Output: target/antlr4-mcp-server-0.2.0.jar (~38MB)
```

### Running Tests

```bash
# All tests (240 total, ~6 seconds)
./mvnw test -q

# Specific test class
./mvnw test -Dtest=ValidateGrammarToolTest -q

# Specific test method
./mvnw test -Dtest=ValidateGrammarToolTest#testValidGrammar -q

# Multiple test classes
./mvnw test -Dtest=ValidateGrammarToolTest,ParseSampleToolTest -q
```

### Quick Compile

```bash
./mvnw compile -q
```

### Build Without Tests

```bash
./mvnw package -DskipTests
```

## Architecture

### Entry Point

`AntlrMcpServerApplication.java`:

```java
public static void main(String[] args) {
    suppressStderr();  // MCP requires clean stdout
    ApplicationContext context = SpringApplication.run(...);
    initializeMcpServer(context);
}
```

### Key Design Decisions

1. **Interpreter Mode**: Uses `ParserInterpreter` for fast parsing (~100ms vs ~2000ms for compilation)
2. **Stateless**: No caching - each operation is independent
3. **No Import Support**: Grammars with `import` are rejected - inline all rules
4. **9 Tools**: Focused toolset for grammar development

### Tool Implementation Pattern

```java
@Component
public class MyTool {
    private final MyService service;
    private final ObjectMapper mapper;

    public McpSchema.Tool toTool() {
        return McpSchema.Tool.builder()
            .name("my_tool")
            .description("Tool description")
            .inputSchema(getInputSchema())
            .build();
    }

    private McpSchema.JsonSchema getInputSchema() {
        Map<String, Object> properties = new HashMap<>();
        // Define properties...
        return new McpSchema.JsonSchema("object", properties, required, null, null, null);
    }

    public McpSchema.CallToolResult execute(McpSyncServerExchange ex, McpSchema.CallToolRequest req) {
        Map<String, Object> args = (Map<String, Object>) req.arguments();
        // Process and return JSON result
    }
}
```

### Service Layer

| Service | Purpose |
|---------|---------|
| `GrammarCompiler` | Load, validate, compile grammars |
| `InterpreterParser` | Fast parsing without compilation |
| `AmbiguityDetector` | Detect parsing ambiguities |
| `LeftRecursionAnalyzer` | Analyze left recursion patterns |
| `FirstFollowAnalyzer` | Compute FIRST/FOLLOW sets |
| `GrammarProfiler` | Performance profiling |
| `CallGraphAnalyzer` | Rule dependency analysis |
| `AtnVisualizer` | ATN diagram generation |
| `MultiTargetCompiler` | Code generation for 10 targets |

## Adding a New Tool

### Step 1: Create Tool Class

```java
package com.github.sshailabh.antlr4mcp.tools;

@Component
@RequiredArgsConstructor
public class MyNewTool {
    private final MyService service;
    private final ObjectMapper mapper;

    public McpSchema.Tool toTool() {
        return McpSchema.Tool.builder()
            .name("my_new_tool")
            .description("What this tool does")
            .inputSchema(getInputSchema())
            .build();
    }

    private McpSchema.JsonSchema getInputSchema() {
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> grammarText = new HashMap<>();
        grammarText.put("type", "string");
        grammarText.put("description", "ANTLR4 grammar content");
        properties.put("grammar_text", grammarText);

        return new McpSchema.JsonSchema(
            "object", properties, List.of("grammar_text"), null, null, null
        );
    }

    public McpSchema.CallToolResult execute(McpSyncServerExchange ex, McpSchema.CallToolRequest req) {
        try {
            Map<String, Object> args = (Map<String, Object>) req.arguments();
            String grammarText = (String) args.get("grammar_text");

            MyResult result = service.process(grammarText);

            return new McpSchema.CallToolResult(
                mapper.writeValueAsString(result), false
            );
        } catch (Exception e) {
            return new McpSchema.CallToolResult(
                "{\"success\":false,\"error\":\"" + e.getMessage() + "\"}", true
            );
        }
    }
}
```

### Step 2: Register in Main Application

```java
// In AntlrMcpServerApplication.java buildServer()
var myNewTool = new MyNewTool(context.getBean(MyService.class), mapper);

return McpServer.sync(transport)
    // ... existing tools ...
    .toolCall(myNewTool.toTool(), myNewTool::execute)
    .build();
```

### Step 3: Write Tests

```java
class MyNewToolTest {
    private MyNewTool tool;
    private ObjectMapper objectMapper;
    private McpSyncServerExchange mockExchange;

    @BeforeEach
    void setUp() {
        MyService service = new MyService(...);
        objectMapper = new ObjectMapper();
        tool = new MyNewTool(service, objectMapper);
        mockExchange = mock(McpSyncServerExchange.class);
    }

    @Test
    void testBasicFunctionality() throws Exception {
        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(
            "my_new_tool",
            Map.of("grammar_text", "grammar Test; start: 'hello';")
        );

        McpSchema.CallToolResult result = tool.execute(mockExchange, request);

        assertThat(result.isError()).isFalse();
    }
}
```

### Step 4: Update Documentation

- Update tool count in `README.md`, `CLAUDE.md`, `USER_GUIDE.md`
- Add tool description to `USER_GUIDE.md`

## Configuration

### application.yml

```yaml
antlr:
  max-grammar-size-mb: 10
  max-input-size-mb: 1
  compilation-timeout-seconds: 30

antlr.mcp:
  version: "0.2.0"
  performance:
    max-grammar-size-mb: 10
    parse-timeout-seconds: 30
    max-concurrent-requests: 50
  security:
    validate-inputs: true
    sanitize-paths: true
```

## Testing

### Test Categories

| Category | Count | Focus |
|----------|-------|-------|
| Tool Tests | ~100 | MCP tool behavior |
| Service Tests | ~80 | Core logic |
| Integration Tests | ~40 | End-to-end |
| Model Tests | ~20 | DTOs |

### Test MCP Server Manually

```bash
# List available tools
echo '{"jsonrpc":"2.0","method":"tools/list","id":1}' | \
  java -jar target/antlr4-mcp-server-0.2.0.jar

# Call a tool
echo '{"jsonrpc":"2.0","method":"tools/call","id":2,"params":{"name":"validate_grammar","arguments":{"grammar_text":"grammar Test; start: ID; ID: [a-z]+;"}}}' | \
  java -jar target/antlr4-mcp-server-0.2.0.jar
```

## Debugging

### Enable Debug Logging

```bash
java -Dlogging.level.com.github.sshailabh.antlr4mcp=DEBUG \
     -jar target/antlr4-mcp-server-0.2.0.jar
```

### Monitor Logs

```bash
tail -f logs/antlr4-mcp-server.log
grep ERROR logs/antlr4-mcp-server.log
```

## Docker

### Build Image

```bash
./docker/build.sh
# Or: docker build -t antlr4-mcp-server:0.2.0 .
```

### Run Container

```bash
docker run -i --rm antlr4-mcp-server:0.2.0
```

### Test Container

```bash
echo '{"jsonrpc":"2.0","method":"tools/list","id":1}' | \
  docker run -i --rm antlr4-mcp-server:0.2.0
```

## Code Quality

### Before Committing

```bash
# 1. Run all tests
./mvnw test -q

# 2. Verify build
./mvnw package -DskipTests

# 3. Test MCP protocol
echo '{"jsonrpc":"2.0","method":"tools/list","id":1}' | \
  java -jar target/antlr4-mcp-server-0.2.0.jar 2>/dev/null | head -1

# 4. Clean artifacts
./mvnw clean
```

### Coding Standards

- Use Lombok (`@Data`, `@Builder`, `@RequiredArgsConstructor`)
- Return JSON results using ObjectMapper
- Handle errors gracefully with structured responses
- Write tests for all tools and services

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Tests fail | Verify Java 17+ with `java -version` |
| Out of memory | Add `-Xmx1g` to JVM args |
| Timeout | Increase `compilation-timeout-seconds` |
| Slow tests | Run `./mvnw clean` first |

## Resources

- [ANTLR4 Documentation](https://github.com/antlr/antlr4/blob/master/doc/index.md)
- [MCP Specification](https://modelcontextprotocol.io/)
- [Java MCP SDK](https://github.com/modelcontextprotocol/java-sdk)

---

**Version**: 0.2.0 | **Last Updated**: November 2025
