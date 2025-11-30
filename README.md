# ANTLR4 MCP Server

> Model Context Protocol server enabling LLMs to assist with ANTLR4 grammar development

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://openjdk.org/)
[![ANTLR](https://img.shields.io/badge/ANTLR-4.13.2-green.svg)](https://www.antlr.org/)
[![Tests](https://img.shields.io/badge/Tests-240%20passing-brightgreen.svg)]()
[![Version](https://img.shields.io/badge/Version-0.2.0-blue.svg)]()

## What This Does

This MCP server provides **9 specialized tools** that allow AI assistants (Claude, Cursor) to help you:

- **Validate** ANTLR4 grammar syntax with detailed error messages
- **Parse** sample inputs and inspect parse trees
- **Analyze** grammar structure (left recursion, FIRST/FOLLOW sets, call graphs)
- **Detect** ambiguities before they become runtime issues
- **Visualize** ATN state machines as DOT/Mermaid diagrams
- **Generate** parser code for 10 target languages
- **Profile** grammar performance with optimization hints

## Quick Start

```bash
# Clone and build
git clone https://github.com/sshailabh/antlr4-mcp-server.git
cd antlr4-mcp-server
./mvnw clean package

# Configure Claude Desktop (~/.config/claude/claude_desktop_config.json on Linux)
# macOS: ~/Library/Application Support/Claude/claude_desktop_config.json
```

```json
{
  "mcpServers": {
    "antlr4": {
      "command": "java",
      "args": ["-jar", "/absolute/path/to/antlr4-mcp-server-0.2.0.jar"]
    }
  }
}
```

Restart Claude Desktop — you now have ANTLR4 tools available.

---

## Tools Reference

| Tool | Purpose | Speed |
|------|---------|-------|
| `validate_grammar` | Syntax validation with error suggestions | ~50ms |
| `parse_sample` | Parse input, return tree (interpreter mode) | ~100ms |
| `detect_ambiguity` | Find parsing ambiguities | ~200ms |
| `analyze_left_recursion` | Detect direct/indirect left recursion | ~100ms |
| `analyze_first_follow` | Compute FIRST/FOLLOW sets | ~150ms |
| `analyze_call_graph` | Rule dependencies, cycles, unused rules | ~100ms |
| `visualize_atn` | ATN state diagrams (DOT/Mermaid) | ~500ms |
| `compile_grammar_multi_target` | Code gen for 10 languages | ~2000ms |
| `profile_grammar` | Performance profiling with hints | ~200ms |

---

## Usage Examples

### Basic Validation

```
You: Validate this grammar

grammar Calc;
expr: expr '+' term | term ;
term: NUMBER ;
NUMBER: [0-9]+ ;
```

<details>
<summary><b>Example Response</b></summary>

```json
{
  "success": true,
  "grammarName": "Calc",
  "grammarType": "COMBINED",
  "ruleCount": 2,
  "tokenCount": 1,
  "errors": [],
  "warnings": []
}
```

</details>

### Parse Sample Input

```
You: Parse "2 + 3" using this grammar with start rule "expr"

grammar Calc;
expr: expr '+' term | term ;
term: NUMBER ;
NUMBER: [0-9]+ ;
WS: [ \t\r\n]+ -> skip ;
```

<details>
<summary><b>Example Response</b></summary>

```json
{
  "success": true,
  "parseTree": "(expr (expr (term 2)) + (term 3))",
  "tokens": [
    {"type": "NUMBER", "text": "2", "line": 1, "column": 0},
    {"type": "'+'", "text": "+", "line": 1, "column": 2},
    {"type": "NUMBER", "text": "3", "line": 1, "column": 4}
  ],
  "errors": []
}
```

</details>

### Detect Ambiguities

```
You: Check for ambiguities in this dangling-else grammar

grammar IfElse;
stat: 'if' expr 'then' stat
    | 'if' expr 'then' stat 'else' stat
    | ID '=' expr ';'
    ;
expr: ID | NUMBER ;
ID: [a-z]+ ;
NUMBER: [0-9]+ ;
WS: [ \t\r\n]+ -> skip ;
```

<details>
<summary><b>Example Response</b></summary>

```json
{
  "success": true,
  "grammarName": "IfElse",
  "hasAmbiguities": true,
  "ambiguities": [
    {
      "rule": "stat",
      "description": "Dangling else - 'else' can bind to either 'if'",
      "alternatives": [1, 2]
    }
  ],
  "suggestions": [
    "Use semantic predicates to resolve",
    "Restructure to make binding explicit"
  ]
}
```

</details>

### Generate Python Parser

```
You: Generate Python parser for this JSON grammar

grammar JSON;
json: value ;
value: STRING | NUMBER | obj | array | 'true' | 'false' | 'null' ;
obj: '{' (pair (',' pair)*)? '}' ;
pair: STRING ':' value ;
array: '[' (value (',' value)*)? ']' ;
STRING: '"' (~["\\\] | '\\' .)* '"' ;
NUMBER: '-'? [0-9]+ ('.' [0-9]+)? ;
WS: [ \t\r\n]+ -> skip ;
```

<details>
<summary><b>Example Response</b></summary>

```json
{
  "success": true,
  "grammarName": "JSON",
  "targetLanguage": "python",
  "files": [
    {"filename": "JSONLexer.py", "content": "..."},
    {"filename": "JSONParser.py", "content": "..."},
    {"filename": "JSONListener.py", "content": "..."},
    {"filename": "JSONVisitor.py", "content": "..."}
  ],
  "instructions": "pip install antlr4-python3-runtime"
}
```

</details>

---

## Complete Grammar Examples

<details>
<summary><b>Calculator Grammar (with operator precedence)</b></summary>

```antlr
grammar Calculator;

expr
    : expr ('*'|'/') expr    // Higher precedence
    | expr ('+'|'-') expr    // Lower precedence
    | NUMBER
    | '(' expr ')'
    ;

NUMBER : [0-9]+ ('.' [0-9]+)? ;
WS     : [ \t\r\n]+ -> skip ;
```

**Test inputs:**
- `42 + 10` → Basic addition
- `3.14 * 2` → Decimal multiplication
- `(10 + 5) * 2` → Parenthesized expression
- `1 + 2 * 3` → Tests precedence (should be 7, not 9)

</details>

<details>
<summary><b>JSON Grammar (complete subset)</b></summary>

```antlr
grammar JSON;

json : value ;

value
    : object
    | array
    | STRING
    | NUMBER
    | 'true'
    | 'false'
    | 'null'
    ;

object
    : '{' pair (',' pair)* '}'
    | '{' '}'
    ;

pair : STRING ':' value ;

array
    : '[' value (',' value)* ']'
    | '[' ']'
    ;

STRING : '"' ( ESC | ~["\\] )* '"' ;
fragment ESC : '\\' ["\\/bfnrt] ;

NUMBER : '-'? INT ('.' [0-9]+)? EXP? ;
fragment INT : '0' | [1-9] [0-9]* ;
fragment EXP : [Ee] [+\-]? INT ;

WS : [ \t\r\n]+ -> skip ;
```

**Test inputs:**
- `{"name": "Alice", "age": 30}`
- `[1, 2, "test", true, null]`
- `{"nested": {"key": [1, 2, 3]}}`

</details>

<details>
<summary><b>Simple Programming Language</b></summary>

```antlr
grammar SimpleLang;

program : statement+ ;

statement
    : assignment
    | ifStatement
    | whileStatement
    | block
    ;

assignment : ID '=' expression ';' ;

ifStatement
    : 'if' '(' expression ')' statement ('else' statement)?
    ;

whileStatement
    : 'while' '(' expression ')' statement
    ;

block : '{' statement* '}' ;

expression
    : expression ('*'|'/') expression
    | expression ('+'|'-') expression
    | expression ('<'|'>'|'<='|'>='|'=='|'!=') expression
    | '(' expression ')'
    | ID
    | NUMBER
    ;

ID     : [a-zA-Z_][a-zA-Z0-9_]* ;
NUMBER : [0-9]+ ;
WS     : [ \t\r\n]+ -> skip ;
```

**Test inputs:**
- `x = 5;`
- `if (x > 0) y = 1;`
- `while (i < 10) { i = i + 1; }`

</details>

---

## Target Languages

| Language | Runtime |
|----------|---------|
| Java | `org.antlr:antlr4-runtime:4.13.2` |
| Python | `pip install antlr4-python3-runtime` |
| JavaScript | `npm install antlr4` |
| TypeScript | `npm install antlr4ts` |
| C++ | ANTLR4 C++ runtime |
| C# | `Antlr4.Runtime.Standard` |
| Go | `github.com/antlr4-go/antlr/v4` |
| Swift | ANTLR4 Swift runtime |
| PHP | `antlr/antlr4-php-runtime` |
| Dart | `antlr4` package |

---

## Constraints

| Constraint | Limit | Reason |
|------------|-------|--------|
| Grammar size | 10 MB | Memory bounds |
| Input size | 1 MB | Parse performance |
| Timeout | 30 seconds | Code generation limit |
| Imports | Not supported | Inline all rules |

**Why no imports?** MCP tools receive grammar text directly. Use a single combined grammar file with all rules inlined.

---

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    MCP Client                           │
│              (Claude Desktop / Cursor)                  │
└─────────────────────┬───────────────────────────────────┘
                      │ JSON-RPC over stdio
┌─────────────────────▼───────────────────────────────────┐
│              ANTLR4 MCP Server                          │
├─────────────────────────────────────────────────────────┤
│  Tools (9)          │  Services (6)                     │
│  ─────────────      │  ───────────                      │
│  validate_grammar   │  GrammarCompiler                  │
│  parse_sample       │  InterpreterParser                │
│  detect_ambiguity   │  AmbiguityDetector                │
│  analyze_*          │  LeftRecursionAnalyzer            │
│  visualize_atn      │  FirstFollowAnalyzer              │
│  compile_grammar    │  GrammarProfiler                  │
│  profile_grammar    │                                   │
└─────────────────────────────────────────────────────────┘
```

**Key Design Decisions:**
- **Interpreter Mode**: Uses `ParserInterpreter` for ~100ms parsing vs ~2000ms compilation
- **Stateless**: No caching — each request is independent
- **Single Grammar**: No import resolution — inline all rules

---

## Development

```bash
# Build
./mvnw clean package

# Run tests (240 tests, ~6 seconds)
./mvnw test

# Test MCP protocol
echo '{"jsonrpc":"2.0","method":"tools/list","id":1}' | \
  java -jar target/antlr4-mcp-server-0.2.0.jar
```

<details>
<summary><b>Docker Deployment</b></summary>

```bash
# Build image
./docker/build.sh

# Configure Claude Desktop
{
  "mcpServers": {
    "antlr4": {
      "command": "docker",
      "args": ["run", "-i", "--rm", "antlr4-mcp-server:0.2.0"]
    }
  }
}
```

</details>

---

## Documentation

| Document | Purpose |
|----------|---------|
| [CLAUDE.md](CLAUDE.md) | AI assistant guidance for this repo |
| [User Guide](docs/USER_GUIDE.md) | Complete tool reference with parameters |
| [Development Guide](docs/DEVELOPMENT.md) | Build, test, contribute |
| [Calculator Example](docs/examples/CALCULATOR.md) | Step-by-step calculator grammar |
| [JSON Example](docs/examples/JSON.md) | Complete JSON parser walkthrough |

---

## License

Apache License 2.0

---
