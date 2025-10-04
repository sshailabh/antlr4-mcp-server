# ANTLR4 MCP Server

> Enable AI assistants to help you develop ANTLR4 grammars through natural conversation

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-11+-orange.svg)](https://openjdk.org/)
[![ANTLR](https://img.shields.io/badge/ANTLR-4.13.2-green.svg)](https://www.antlr.org/)

## What is This?

The ANTLR4 MCP Server lets you work with [ANTLR4](https://www.antlr.org/) grammars using AI assistants like **Claude** and **Cursor**. Just paste your grammar into a conversation and get instant validation, parsing, and ambiguity detection.

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

## Features

| Feature | Description |
|---------|-------------|
| 🔍 **Grammar Validation** | Check syntax errors, undefined rules |
| ⚠️ **Ambiguity Detection** | Find conflicts and get fix suggestions |
| 🌳 **Parse Testing** | Test your grammar with sample inputs |
| 📊 **Visualization** | Generate rule diagrams |
| 📁 **File Support** | Work with grammar files |
| 🚀 **Code Generation** | Generate parsers for multiple languages |

## Installation

### Prerequisites

- **Docker Desktop** (4.0+)
- **Claude Desktop** or **Cursor IDE**

### Setup (2 minutes)

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

See [DEPLOYMENT.md](DEPLOYMENT.md) for Cursor setup instructions.

## Usage

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

## Example Grammars

Explore complete examples in [`docs/examples/`](docs/examples/):

- **[Calculator](docs/examples/CALCULATOR.md)** - Arithmetic expressions with operator precedence
- **[JSON](docs/examples/JSON.md)** - JSON parser with nested objects and arrays

## Documentation

### 📖 User Documentation
- **[Usage Guide](docs/USAGE.md)** - Complete tool reference and examples
- **[Deployment Guide](DEPLOYMENT.md)** - Setup for Claude and Cursor
- **[API Schemas](API_SCHEMAS.md)** - Tool specifications

### 🔧 Developer Documentation
- **[Project Details](PROJECT_DETAILS.md)** - Architecture, roadmap, contributing
- **[Architecture](ARCHITECTURE.md)** - Technical design
- **[Security](SECURITY.md)** - Security implementation
- **[Contributing](CONTRIBUTING.md)** - Development guidelines

See [PROJECT_DETAILS.md](PROJECT_DETAILS.md) for roadmap and planned features.

## Security

This server implements comprehensive security measures:
- Input validation and sanitization
- Resource limits (memory, CPU, time)
- Docker container isolation
- Path traversal protection

See [SECURITY.md](SECURITY.md) for details.

## Roadmap

See [PROJECT_DETAILS.md](PROJECT_DETAILS.md) for the complete development roadmap and planned features.

## Development

### Build from Source

```bash
# Prerequisites: JDK 11+, Maven 3.8+, Docker

git clone https://github.com/sshailabh/antlr4-mcp-server.git
cd antlr4-mcp-server

# Build
mvn clean package

# Run tests
mvn test

# Build Docker image
./docker/build.sh
```

### Contributing

We welcome contributions! See [CONTRIBUTING.md](CONTRIBUTING.md) and [PROJECT_DETAILS.md](PROJECT_DETAILS.md) for guidelines.

## Resources

- **[ANTLR Official Site](https://www.antlr.org)** - ANTLR documentation
- **[ANTLR4 GitHub](https://github.com/antlr/antlr4)** - ANTLR source code
- **[Model Context Protocol](https://modelcontextprotocol.io)** - MCP specification
- **[Java MCP SDK](https://github.com/modelcontextprotocol/java-sdk)** - Official Java SDK

## Support

- **Issues**: [GitHub Issues](https://github.com/sshailabh/antlr4-mcp-server/issues)
- **Discussions**: [GitHub Discussions](https://github.com/sshailabh/antlr4-mcp-server/discussions)
- **Documentation**: See [`/docs`](docs/) directory

## License

Apache License 2.0 - See [LICENSE](LICENSE) for details.

## Acknowledgments

- **ANTLR Team** - For the amazing parser generator
- **Terence Parr** - Creator of ANTLR
- **Anthropic** - For Claude and MCP specification
- **Spring AI Team** - For Java MCP SDK

---

**Version**: 0.1.0
**Status**: Active Development
**Maintainer**: [@sshailabh](https://github.com/sshailabh)

Ready to start? Check out the [Usage Guide](docs/USAGE.md) or try an [example](docs/examples/)!
