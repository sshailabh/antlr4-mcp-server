# Quick Start

Get the ANTLR4 MCP Server running in under 5 minutes.

---

## 1. Choose Your Setup

Docker (Recommended)

```bash
docker pull sshailabh1/antlr4-mcp-server:latest
```

Or build locally:
```bash
docker build -t antlr4-mcp-server:latest .
```

<details>
<summary><b>JAR (Requires Java 17+)</b></summary>

```bash
./mvnw clean package -DskipTests
```

Output: `target/antlr4-mcp-server-0.2.0.jar`

</details>

---

## 2. Configure Your MCP Client

<details>
<summary><b>Claude Desktop</b></summary>

Add to your config file:
- **macOS**: `~/Library/Application Support/Claude/claude_desktop_config.json`
- **Linux**: `~/.config/claude/claude_desktop_config.json`
- **Windows**: `%APPDATA%\Claude\claude_desktop_config.json`

**Docker:**
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

**JAR:**
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

Restart Claude Desktop after saving.

</details>

<details>
<summary><b>Cursor IDE</b></summary>

Add to `.cursor/mcp.json` in your project:

**Docker:**
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

**JAR:**
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

Restart Cursor after saving.

</details>

<details>
<summary><b>VS Code + Continue</b></summary>

Add to your Continue configuration:

```json
{
  "mcpServers": [
    {
      "name": "antlr4",
      "command": "docker",
      "args": ["run", "-i", "--rm", "sshailabh1/antlr4-mcp-server:latest"]
    }
  ]
}
```

</details>

---

## 3. Verify Setup

Ask your AI assistant:

> "List all available ANTLR4 tools"

You should see 9 tools:

| Tool | Purpose |
|------|---------|
| `validate_grammar` | Check grammar syntax |
| `parse_sample` | Parse input text |
| `compile_grammar_multi_target` | Generate parser code |
| `detect_ambiguity` | Find ambiguities |
| `analyze_left_recursion` | Detect recursion patterns |
| `analyze_first_follow` | Compute FIRST/FOLLOW sets |
| `analyze_call_graph` | Rule dependencies |
| `profile_grammar` | Performance profiling |
| `visualize_atn` | ATN diagrams |

---

## 4. Try It

**Validate a grammar:**
```
Validate this grammar:

grammar Hello;
greeting: 'hello' name ;
name: WORD ;
WORD: [a-zA-Z]+ ;
WS: [ \t\r\n]+ -> skip ;
```

**Parse input:**
```
Parse "hello world" with start rule "greeting"
```

**Generate Python parser:**
```
Generate a Python parser for the Hello grammar
```

---

## Next Steps

- [Tool Usage Guide](TOOL_USAGE.md) — Detailed tool reference with examples
- [Development Guide](DEVELOPMENT.md) — Build, test, contribute
