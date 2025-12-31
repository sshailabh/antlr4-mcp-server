# ANTLR4 MCP Server - Development Guide

**Version**: 0.2.0

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
├── src/test/java/                      # Test suite
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

### Test MCP Server Manually

```bash
# MCP handshake (initialize -> notifications/initialized) is required before using tools.
printf '%s\n' \
  '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"cli","version":"0"}}}' \
  '{"jsonrpc":"2.0","method":"notifications/initialized"}' \
  '{"jsonrpc":"2.0","id":2,"method":"tools/list"}' | \
  java -jar target/antlr4-mcp-server-0.2.0.jar

# Call a tool
printf '%s\n' \
  '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"cli","version":"0"}}}' \
  '{"jsonrpc":"2.0","method":"notifications/initialized"}' \
  '{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"validate_grammar","arguments":{"grammar_text":"grammar Test; start: ID; ID: [a-z]+; WS: [ \\t\\r\\n]+ -> skip;"}}}' | \
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

### Image Details

| Metric | Value |
|--------|-------|
| **Size** | ~311MB (optimized with jlink) |
| **Base** | Debian Bookworm Slim |
| **JRE** | Custom minimal Eclipse Temurin 17 (~56MB) |
| **Architectures** | linux/amd64, linux/arm64 |

### Build Image

```bash
# Single platform (current machine)
./docker/build.sh 0.2.0

# Or directly with docker
docker build -t antlr4-mcp-server:latest .
```

### Multi-Architecture Build

```bash
# Build for both amd64 and arm64 (requires pushing to registry)
./docker/build.sh 0.2.0 --multi-arch --push
```

### Run Container

```bash
docker run -i --rm antlr4-mcp-server:latest
```

### Test Container

```bash
# Using Python demo (recommended)
cd ../dsl-starter
python3 scripts/mcp_all_tools_demo.py --server docker

# Manual MCP test
printf '%s\n' \
  '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"cli","version":"0"}}}' \
  '{"jsonrpc":"2.0","method":"notifications/initialized"}' \
  '{"jsonrpc":"2.0","id":2,"method":"tools/list"}' | \
  docker run -i --rm antlr4-mcp-server:latest
```

### Docker Optimization

The Dockerfile uses jlink to create a minimal custom JRE:

```dockerfile
# Create custom minimal JRE
RUN $JAVA_HOME/bin/jlink \
    --add-modules java.base,java.logging,java.naming,java.desktop,... \
    --strip-debug \
    --no-man-pages \
    --no-header-files \
    --compress=2 \
    --output /javaruntime
```

This reduces image size from ~526MB (full JDK) to ~311MB.

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

**Version**: 0.2.0
