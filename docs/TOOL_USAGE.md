# Tool Usage Guide

Complete reference for all 9 ANTLR4 MCP tools with parameters and examples.

---

## 1. `validate_grammar`

Validates ANTLR4 grammar syntax and reports errors with actionable suggestions.

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `grammar_text` | string | Yes | Complete ANTLR4 grammar |
| `grammar_name` | string | No | Expected name (for validation) |

**Example:**
```
Validate this grammar:

grammar Hello;
greeting: 'hello' name ;
name: WORD ;
WORD: [a-zA-Z]+ ;
WS: [ \t\r\n]+ -> skip ;
```

<details>
<summary>Response</summary>

```json
{
  "success": true,
  "grammarName": "Hello",
  "lexerRules": 2,
  "parserRules": 2,
  "errors": []
}
```
</details>

---

## 2. `parse_sample`

Parses input text using the grammar and returns the parse tree.

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `grammar_text` | string | Yes | Complete ANTLR4 grammar |
| `sample_input` | string | Yes | Input to parse |
| `start_rule` | string | Yes | Parser rule to start from |
| `show_tokens` | boolean | No | Include token list (default: true) |

**Example:**
```
Parse "hello world" with start rule "greeting":

grammar Hello;
greeting: 'hello' name ;
name: WORD ;
WORD: [a-zA-Z]+ ;
WS: [ \t\r\n]+ -> skip ;
```

<details>
<summary>Response</summary>

```json
{
  "success": true,
  "parseTree": "(greeting hello (name world))",
  "tokens": "[@0,0:4='hello',<'hello'>,1:0]\n[@1,6:10='world',<WORD>,1:6]\n"
}
```
</details>

---

## 3. `detect_ambiguity`

Detects parsing ambiguities in the grammar.

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `grammar_text` | string | Yes | Complete ANTLR4 grammar |
| `sample_inputs` | array | No | Inputs to test for ambiguities |
| `start_rule` | string | No | Start rule for testing |

**Example:**
```
Check for ambiguities:

grammar Ambiguous;
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
<summary>Response</summary>

```json
{
  "hasAmbiguities": true,
  "ambiguities": [
    {
      "ruleName": "stat",
      "conflictingAlternatives": [1, 2],
      "explanation": "Dangling else ambiguity",
      "suggestedFix": "Restructure the rule or use a predicate"
    }
  ]
}
```
</details>

---

## 4. `analyze_left_recursion`

Detects direct and indirect left recursion patterns.

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `grammar_text` | string | Yes | Complete ANTLR4 grammar |

**Example:**
```
Analyze left recursion:

grammar Expr;
expr: expr '+' term | term ;
term: term '*' factor | factor ;
factor: NUMBER | '(' expr ')' ;
NUMBER: [0-9]+ ;
```

<details>
<summary>Response</summary>

```json
{
  "hasLeftRecursion": true,
  "leftRecursiveRules": ["expr", "term"],
  "cycles": [
    {"rules": ["expr"], "direct": true},
    {"rules": ["term"], "direct": true}
  ]
}
```
</details>

---

## 5. `analyze_first_follow`

Computes FIRST and FOLLOW sets for grammar rules.

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `grammar_text` | string | Yes | Complete ANTLR4 grammar |
| `rule_name` | string | No | Specific rule (default: all) |

**Example:**
```
Compute FIRST/FOLLOW sets:

grammar Simple;
prog: stmt+ ;
stmt: ID '=' expr ';' ;
expr: term (('+' | '-') term)* ;
term: NUMBER | ID ;
ID: [a-z]+ ;
NUMBER: [0-9]+ ;
WS: [ \t\r\n]+ -> skip ;
```

<details>
<summary>Response</summary>

```json
{
  "success": true,
  "rules": [
    {
      "ruleName": "expr",
      "firstSet": ["NUMBER", "ID"],
      "followSet": ["';'"],
      "nullable": false
    }
  ]
}
```
</details>

---

## 6. `analyze_call_graph`

Analyzes rule dependencies, detects cycles, and identifies unused rules.

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `grammar_text` | string | Yes | Complete ANTLR4 grammar |
| `output_format` | string | No | `json`, `dot`, or `mermaid` |

**Example:**
```
Show rule dependencies in Mermaid:

grammar Calc;
prog: expr+ ;
expr: term (('+' | '-') term)* ;
term: factor (('*' | '/') factor)* ;
factor: NUMBER | '(' expr ')' ;
NUMBER: [0-9]+ ;
```

<details>
<summary>Response</summary>

```json
{
  "success": true,
  "format": "mermaid",
  "content": "graph LR\n  prog --> expr\n  expr --> term\n  term --> factor\n  factor --> expr"
}
```
</details>

---

## 7. `visualize_atn`

Generates ATN (Augmented Transition Network) state machine diagrams.

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `grammar_text` | string | Yes | Complete ANTLR4 grammar |
| `rule_name` | string | Yes | Rule to visualize |
| `format` | string | No | `dot`, `mermaid`, `svg`, or `all` |

**Example:**
```
Generate ATN diagram for expr rule:

grammar Expr;
expr: term (('+' | '-') term)* ;
term: NUMBER ;
NUMBER: [0-9]+ ;
```

<details>
<summary>Response</summary>

```json
{
  "success": true,
  "ruleName": "expr",
  "mermaid": "stateDiagram-v2\n    [*] --> s0\n    s0 --> s1: term\n    s1 --> s2: '+' | '-'\n    s2 --> s1: term\n    s1 --> [*]",
  "stateCount": 3
}
```
</details>

---

## 8. `compile_grammar_multi_target`

Generates parser/lexer code for multiple target languages.

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `grammar_text` | string | Yes | Complete ANTLR4 grammar |
| `target_language` | string | No | `java`, `python`, `javascript`, `typescript`, `cpp`, `csharp`, `go`, `swift`, `php`, `dart` |
| `generate_listener` | boolean | No | Generate listener (default: true) |
| `generate_visitor` | boolean | No | Generate visitor (default: false) |
| `include_generated_code` | boolean | No | Include source in response (default: false) |

**Example:**
```
Generate Python parser:

grammar Hello;
greeting: 'hello' name ;
name: WORD ;
WORD: [a-zA-Z]+ ;
WS: [ \t\r\n]+ -> skip ;
```

<details>
<summary>Response</summary>

```json
{
  "success": true,
  "targetLanguage": "Python3",
  "fileCount": 4,
  "files": [
    {"fileName": "HelloLexer.py", "fileType": "lexer"},
    {"fileName": "HelloParser.py", "fileType": "parser"},
    {"fileName": "HelloListener.py", "fileType": "listener"},
    {"fileName": "HelloVisitor.py", "fileType": "visitor"}
  ]
}
```
</details>

### Target Runtimes

| Language | Install |
|----------|---------|
| Python | `pip install antlr4-python3-runtime` |
| JavaScript | `npm install antlr4` |
| TypeScript | `npm install antlr4ts` |
| Java | `org.antlr:antlr4-runtime:4.13.2` |
| Go | `github.com/antlr4-go/antlr/v4` |
| C# | `Antlr4.Runtime.Standard` |

---

## 9. `profile_grammar`

Profiles grammar performance with decision statistics and lookahead analysis.

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `grammar_text` | string | Yes | Complete ANTLR4 grammar |
| `sample_input` | string | Yes | Input for profiling |
| `start_rule` | string | Yes | Start rule |

**Example:**
```
Profile with input "1 + 2 * 3":

grammar Expr;
prog: expr+ ;
expr: expr ('*'|'/') expr
    | expr ('+'|'-') expr
    | NUMBER
    ;
NUMBER: [0-9]+ ;
WS: [ \t\r\n]+ -> skip ;

Start rule: prog
```

<details>
<summary>Response</summary>

```json
{
  "success": true,
  "grammarName": "Expr",
  "totalTimeNanos": 1250000,
  "decisions": [
    {"decisionNumber": 0, "ruleName": "prog", "invocations": 1},
    {"decisionNumber": 1, "ruleName": "expr", "invocations": 3}
  ],
  "optimizationHints": ["Expression has left recursion - ANTLR handles automatically"]
}
```
</details>

---

## Workflows

### Grammar Development

```
1. validate_grammar       → Check syntax
2. parse_sample           → Test with inputs
3. detect_ambiguity       → Find issues
4. analyze_left_recursion → Check recursion
5. profile_grammar        → Performance check
6. compile_grammar_multi_target → Generate code
```

### Full Analysis

```
Give me a complete analysis of this grammar:
- Validation
- Left recursion check
- FIRST/FOLLOW sets
- Ambiguity detection
- Call graph

grammar Expr;
expr: expr '+' term | term ;
term: term '*' factor | factor ;
factor: NUMBER | '(' expr ')' ;
NUMBER: [0-9]+ ;
WS: [ \t\r\n]+ -> skip ;
```

---

## Constraints

| Constraint | Value |
|------------|-------|
| Max grammar size | 10 MB |
| Max input size | 1 MB |
| Timeout | 30 seconds |
| Imports | Not supported (inline all rules) |

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| "Grammar not found" | Ensure `grammar Name;` declaration exists |
| "Start rule not found" | Parser rules are lowercase |
| "Ambiguity detected" | Use `detect_ambiguity` for details |
| Slow performance | Use `profile_grammar` to identify bottlenecks |
