# ANTLR4 MCP Server

> Enable AI assistants to help you develop ANTLR4 grammars through natural conversation

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://openjdk.org/)
[![ANTLR](https://img.shields.io/badge/ANTLR-4.13.2-green.svg)](https://www.antlr.org/)
[![Version](https://img.shields.io/badge/Version-0.2.0-blue.svg)](https://github.com/sshailabh/antlr4-mcp-server)

## What is This?

The ANTLR4 MCP Server lets you work with [ANTLR4](https://www.antlr.org/) grammars using AI assistants like **Claude** and **Cursor**. Just paste your grammar into a conversation and get instant validation, parsing, and analysis.

### Quick Example

**You**: Can you validate this calculator grammar?

```antlr
grammar Calculator;
expr : expr ('*'|'/') expr
     | expr ('+'|'-') expr
     | NUMBER
     ;
NUMBER : [0-9]+ ;
```

**Claude**: ✅ Your grammar is valid! However, I notice potential ambiguity due to left-recursion...

---

## Features

### ⚡ High Performance

**Optimized dual-path architecture:**

| Operation | Performance | Architecture |
|-----------|-------------|--------------|
| Grammar validation | 10-50ms | ⚡ Fast path via interpreter mode |
| Parse sample | 20-100ms | ⚡ Instant parsing with caching |
| Memory per grammar | 5-10MB | 💾 Efficient memory usage |
| Advanced visualization | 500-2000ms | 🐌 Full compilation when needed |

### ✅ Complete Feature Set

| Feature | Description | Performance |
|---------|-------------|-------------|
| 🔍 **Grammar Validation** | Syntax checking with structured errors | ⚡ Fast |
| 📊 **Parse Sample** | Test grammars with sample inputs, LISP trees | ⚡ Fast |
| ⚠️ **Ambiguity Detection** | Find parsing conflicts with runtime analysis | ⚡ Fast |
| 📊 **Call Graph Analysis** | Rule dependencies and structure analysis | ⚡ Fast |
| 🔢 **Complexity Analysis** | Grammar complexity metrics and insights | ⚡ Fast |
| ♻️ **Left-Recursion Analysis** | Detect and analyze left-recursion patterns | ⚡ Fast |
| 🎯 **Multi-target Compilation** | Generate parsers for Java, Python, JavaScript | 🐌 Compilation |
| 🧪 **Test Input Generation** | Automatic sample test case generation | ⚡ Fast |
| 🔄 **ATN Visualization** | Visual automaton state machines | 🐌 Compilation |
| 🎯 **Decision Visualization** | Visualize parser decision points and DFA | 🐌 Compilation |

**10 Tools Optimized** | **460+ Tests Passing** | **Production Ready**

---

## Quick Start

### Prerequisites

- **Docker Desktop** (4.0+) - [Download here](https://www.docker.com/products/docker-desktop)
- **Claude Desktop** or **Cursor IDE**

### Installation (2 minutes)

1. **Clone and build**:
```bash
git clone https://github.com/sshailabh/antlr4-mcp-server.git
cd antlr4-mcp-server
./docker/build.sh
```

2. **Configure Claude Desktop**:

Edit `~/Library/Application Support/Claude/claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "antlr": {
      "command": "docker",
      "args": ["run", "-i", "--rm", "antlr4-mcp-server:latest"]
    }
  }
}
```

3. **Restart Claude Desktop** - Done! 🎉

### Configure Cursor IDE

Add to Cursor MCP settings:

```json
{
  "mcpServers": {
    "antlr": {
      "command": "docker",
      "args": ["run", "-i", "--rm", "antlr4-mcp-server:latest"]
    }
  }
}
```

---

## Usage Examples

### Validate a Grammar

```
You: Check my JSON grammar for errors

[Paste your grammar]
```

**Claude validates syntax and reports any issues**

### Parse Sample Input

```
You: Parse this JSON: {"name": "test", "value": 42}

[Include your JSON grammar]
```

**Claude shows you the parse tree**

### Detect Ambiguities

```
You: Are there any ambiguities in my expression grammar?
```

**Claude analyzes and suggests fixes**

### Profile Performance

```
You: Profile my grammar's performance on this large input
```

**Claude shows decision statistics and timing metrics**

---

## Advanced Configuration

### Custom Memory Limits

For large grammars, increase Docker memory:

```json
{
  "mcpServers": {
    "antlr": {
      "command": "docker",
      "args": [
        "run", "-i", "--rm",
        "--memory=1g",
        "antlr4-mcp-server:latest"
      ]
    }
  }
}
```

### Enable Debug Logging

```json
{
  "mcpServers": {
    "antlr": {
      "command": "docker",
      "args": [
        "run", "-i", "--rm",
        "-e", "LOGGING_LEVEL_ROOT=DEBUG",
        "antlr4-mcp-server:latest"
      ]
    }
  }
}
```

---

## Security

### Built-in Security Features

- ✅ **Input Validation**: Grammar names, rule names, file paths validated against injection attacks
- ✅ **Resource Limits**: Memory (512MB), CPU, timeout (30s) constraints enforced
- ✅ **Docker Isolation**: Non-root user, read-only filesystem, tmpfs for temp files
- ✅ **Path Protection**: Symlink blocking, directory traversal prevention
- ✅ **Process Security**: Command whitelisting, output limiting, no shell expansion

### Security Configuration

Default limits (configurable via `application.yml`):

```yaml
antlr:
  max-grammar-size-mb: 10
  max-input-size-mb: 1
  max-response-size-kb: 50
  compilation-timeout-seconds: 30
```

### Reporting Vulnerabilities

**DO NOT** create public issues for security vulnerabilities.

Email security reports to: [shailabhshashank@gmail.com]

**Response Timeline**:
- 24 hours: Initial acknowledgment
- 72 hours: Preliminary assessment
- 7 days: Fix development for critical issues

---

## Development

### Build from Source

```bash
# Prerequisites: JDK 17+, Maven 3.8+, Docker

git clone https://github.com/sshailabh/antlr4-mcp-server.git
cd antlr4-mcp-server

# Build with tests
./mvnw clean package

# Run tests
./mvnw test

# Build Docker image
./docker/build.sh
```

### Run Tests

```bash
# All tests (349 tests)
./mvnw test

# Specific test class
./mvnw test -Dtest=GrammarCompilerTest

# Integration tests only
./mvnw test -Dtest=*IntegrationTest
```

### Project Structure

```
antlr4-mcp-server/
├── src/main/java/.../antlr4mcp/
│   ├── service/              # Core services (compiler, interpreter, analyzers)
│   ├── tools/                # MCP tool implementations (10 tools optimized)
│   ├── model/                # Data models and DTOs
│   ├── analysis/             # Call graph, complexity, left-recursion analysis
│   ├── codegen/              # Multi-target code generation
│   ├── infrastructure/       # Imports, caching, resources
│   ├── visualization/        # SVG/DOT diagram generation
│   └── security/             # Input validation, resource limits
├── docs/                     # Documentation (usage, tool analysis, examples)
└── docker/                   # Docker build scripts
```

### Tool Architecture

**Current Status**: **10 tools optimized** for optimal LLM usage with dual-path performance architecture.

**Core Tools (Essential)**:
- `validate_grammar` - Grammar syntax validation
- `parse_sample` - Sample input parsing & testing
- `detect_ambiguity` - Ambiguity detection with examples
- `analyze_call_graph` - Rule dependencies & structure analysis
- `analyze_complexity` - Grammar complexity metrics
- `analyze_left_recursion` - Left-recursion pattern analysis
- `compile_grammar_multi_target` - Multi-language code generation
- `generate_test_inputs` - Automatic test case generation

**Advanced Tools (Specialized)**:
- `visualize_atn` - Internal ATN structure visualization
- `visualize_dfa` - Decision point & DFA state analysis

See **[Tool Analysis](docs/TOOL_ANALYSIS.md)** for detailed tool descriptions and architecture.

---

## Documentation

### 📖 User Documentation
- **[Usage Guide](docs/USAGE.md)** - Complete tool reference with examples
- **[Tool Analysis](docs/TOOL_ANALYSIS.md)** - Complete tool reference and architecture guide
- **[API Schemas](docs/development/API_SCHEMAS.md)** - Tool specifications

### 📊 Performance & Architecture
- **Fast Path Tools** (8 tools): 10-50ms via GrammarInterpreter + caching
- **Slow Path Tools** (2 tools): 500-2000ms via full compilation
- **Optimized Grammar Loading**: Automatic fallback from fast to slow path

---

## Troubleshooting

### "MCP server not responding"

1. Verify Docker is running: `docker ps`
2. Check image exists: `docker images | grep antlr4-mcp-server`
3. Test manually:
   ```bash
   echo '{"jsonrpc":"2.0","method":"tools/list","id":1}' | \
     docker run -i --rm antlr4-mcp-server:latest
   ```
4. Restart Claude Desktop/Cursor completely

### "Grammar validation always fails"

1. Ensure proper grammar declaration: `grammar MyGrammar;`
2. Check semicolons after all rules
3. Review Docker logs: `docker logs <container-id>`

### "Docker permission denied"

**macOS/Linux**:
```bash
sudo usermod -aG docker $USER
# Log out and back in
```

**Windows (WSL2)**:
- Ensure Docker Desktop is running
- Enable WSL2 integration in Docker Desktop settings

---

## Resources

- **[ANTLR Official Site](https://www.antlr.org)** - ANTLR documentation
- **[ANTLR4 GitHub](https://github.com/antlr/antlr4)** - ANTLR source code
- **[Model Context Protocol](https://modelcontextprotocol.io)** - MCP specification
- **[Java MCP SDK](https://github.com/modelcontextprotocol/java-sdk)** - Official Java SDK

## Support

- **Issues**: [GitHub Issues](https://github.com/sshailabh/antlr4-mcp-server/issues)
- **Discussions**: [GitHub Discussions](https://github.com/sshailabh/antlr4-mcp-server/discussions)
- **Documentation**: See [`/docs`](docs/) directory

