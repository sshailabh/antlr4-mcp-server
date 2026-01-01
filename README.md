# ANTLR4 MCP Server

MCP server enabling AI assistants to help with ANTLR4 grammar development.

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://openjdk.org/)
[![ANTLR](https://img.shields.io/badge/ANTLR-4.13.2-green.svg)](https://www.antlr.org/)

---

## Quick Start

### Docker (Recommended)

```bash
docker pull sshailabh1/antlr4-mcp-server:latest
```

Configure your MCP client:

```json
{
  "mcpServers": {
    "antlr4": {
      "command": "docker",
      "args": ["run", "-i", "--rm", "sshailabh1/antlr4-mcp-server:latest"]
    }
  }
}
```

### Build from Source

```bash
./mvnw clean package -DskipTests
```

```json
{
  "mcpServers": {
    "antlr4": {
      "command": "java",
      "args": ["-jar", "/path/to/antlr4-mcp-server-0.2.0.jar"]
    }
  }
}
```

---

## Tools

| Tool | Purpose |
|------|---------|
| `validate_grammar` | Syntax validation with error suggestions |
| `parse_sample` | Parse input, return tree (interpreter mode) |
| `detect_ambiguity` | Find parsing ambiguities |
| `analyze_left_recursion` | Detect recursion patterns |
| `analyze_first_follow` | Compute FIRST/FOLLOW sets |
| `analyze_call_graph` | Rule dependencies, cycles, unused rules |
| `visualize_atn` | ATN state diagrams (DOT/Mermaid) |
| `compile_grammar_multi_target` | Generate parsers (10 languages) |
| `profile_grammar` | Performance profiling |

---

## Usage Example

```
Validate and parse "2 + 3 * 4":

grammar Calc;
expr: expr ('*'|'/') expr | expr ('+'|'-') expr | NUMBER ;
NUMBER: [0-9]+ ;
WS: [ \t\r\n]+ -> skip ;
```

---

## Target Languages

Java, Python, JavaScript, TypeScript, C++, C#, Go, Swift, PHP, Dart

---

## Documentation

| Document | Purpose |
|----------|---------|
| [Quick Start](docs/QUICK_START.md) | Setup in 5 minutes |
| [Tool Usage](docs/TOOL_USAGE.md) | Complete tool reference |
| [Development](docs/DEVELOPMENT.md) | Build, test, contribute |
| [Examples](docs/examples/) | Calculator, JSON grammars |

---

## Constraints

| Constraint | Value |
|------------|-------|
| Max grammar size | 10 MB |
| Max input size | 1 MB |
| Timeout | 30 seconds |
| Imports | Not supported (inline all rules) |

---

## License

Apache License 2.0
