# ANTLR4 MCP Server - Usage Guide

This guide provides comprehensive examples of using the ANTLR4 MCP Server with AI assistants like Claude and Cursor.

## Table of Contents

- [Getting Started](#getting-started)
- [Tool Reference](#tool-reference)
- [Common Workflows](#common-workflows)
- [Example Grammars](#example-grammars)
- [Best Practices](#best-practices)
- [Troubleshooting](#troubleshooting)

## Getting Started

### Basic Validation

**User**: Can you validate this calculator grammar?

```antlr
grammar Calculator;

expr : expr ('*'|'/') expr
     | expr ('+'|'-') expr
     | NUMBER
     | '(' expr ')'
     ;

NUMBER : [0-9]+ ('.' [0-9]+)? ;
WS     : [ \t\r\n]+ -> skip ;
```

**Claude Response**: Your grammar is valid! Grammar name: `Calculator`

However, I notice this grammar has left-recursion which will cause ambiguity. Let me check that for you...

## Tool Reference

### 1. validate_grammar

Validates ANTLR4 grammar syntax and reports errors.

**Parameters**:
- `grammar_text` (required): Complete ANTLR4 grammar content
- `grammar_name` (optional): Expected grammar name for validation

**Example**:
```
User: Validate this grammar:
grammar Test;
start: 'hello' 'world'
NUMBER : [0-9]+ ;
```

**Response**: Error found - missing semicolon after rule definition at line 2.

### 2. parse_sample

Parses sample input using provided grammar and returns parse tree.

**Parameters**:
- `grammar_text` (required): Complete ANTLR4 grammar
- `sample_input` (required): Input text to parse
- `start_rule` (required): Parser rule to start from
- `show_tokens` (optional): Include token stream in output (default: false)
- `tree_format` (optional): 'tree' (LISP) or 'tokens' (default: tree)

**Example**:
```
User: Parse "42 + 10" using my Calculator grammar
```

**Response**:
```
Parse successful!
Tree: (expr (expr (NUMBER 42)) + (expr (NUMBER 10)))
```

### 3. detect_ambiguity

Analyzes grammar for ambiguities, conflicts, and prediction issues.

**Parameters**:
- `grammar_text` (required): Complete ANTLR4 grammar to analyze

**Example**:
```
User: Check for ambiguities in my expression grammar
```

**Response**: Found left-recursion ambiguity in rule 'expr' at line 3. Both alternatives can match with same lookahead.

### 4. visualize_rule

Generates visual representation of grammar rule (M1: Not fully implemented).

**Parameters**:
- `grammar_text` (required): Complete ANTLR4 grammar
- `rule_name` (required): Name of rule to visualize
- `format` (optional): 'svg' or 'dot' (default: svg)

##Human: continue