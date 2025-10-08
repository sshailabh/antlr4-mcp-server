# ANTLR4 MCP Server - Usage Guide

This guide provides comprehensive examples of using the ANTLR4 MCP Server with AI assistants like Claude and Cursor.

## Table of Contents

- [Getting Started](#getting-started)
- [Core Tools Reference](#core-tools-reference)
- [Advanced Tools Reference](#advanced-tools-reference)
- [Common Workflows](#common-workflows)
- [Performance Guide](#performance-guide)
- [Best Practices](#best-practices)
- [Troubleshooting](#troubleshooting)

## Tool Overview

**Current Status**: **10 tools optimized** for optimal LLM usage (redundant tools removed).

### Complete Tool Set (10 tools)

**Core Tools (8 essential)**:
- `validate_grammar` - Grammar syntax validation (⚡ fast)
- `parse_sample` - Sample input parsing & testing (⚡ fast)
- `detect_ambiguity` - Ambiguity detection with examples (⚡ fast)
- `analyze_call_graph` - Rule dependencies & structure (⚡ fast)
- `analyze_complexity` - Grammar complexity metrics (⚡ fast)
- `analyze_left_recursion` - Left-recursion analysis (⚡ fast)
- `compile_grammar_multi_target` - Multi-language code generation
- `generate_test_inputs` - Automatic test case generation (⚡ fast)

**Advanced Tools (2 specialized)**:
- `visualize_atn` - Internal ATN structure visualization (🐌 slow)
- `visualize_dfa` - Decision point & DFA analysis (🐌 slow)

⚡ **Fast tools** (10-50ms) use GrammarInterpreter with caching
🐌 **Slow tools** (500-2000ms) require full compilation

See **[Tool Analysis](TOOL_ANALYSIS.md)** for detailed recommendations.

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

## Core Tools Reference

### 1. validate_grammar ⚡

Validates ANTLR4 grammar syntax and reports errors with actionable fixes.

**Performance**: Fast path (10-50ms) via GrammarInterpreter + caching

**Parameters**:
- `grammar_text` (required): Complete ANTLR4 grammar content

**Example**:
```
User: Validate this grammar:
grammar Calculator;
expr : expr '+' expr | NUMBER ;
NUMBER : [0-9]+ ;
```

**Response**: Grammar validation successful! However, detected left-recursion in rule 'expr' - ANTLR will automatically transform this.

### 2. parse_sample ⚡

Parses sample input using provided grammar, returns parse tree and tokens.

**Performance**: Fast path (10-50ms) via GrammarInterpreter + caching

**Parameters**:
- `grammar_text` (required): Complete ANTLR4 grammar
- `sample_input` (required): Input text to parse
- `start_rule` (required): Parser rule to start from
- `show_tokens` (optional): Include token stream (default: false)

**Example**:
```
User: Parse "2+3*4" with this calculator grammar using rule 'expr'
```

**Response**: Parse successful! Tree: `(expr (expr 2) + (expr (expr 3) * (expr 4)))`

### 3. detect_ambiguity ⚡

Detects ambiguous parsing scenarios with specific examples.

**Performance**: Fast path (10-50ms) via GrammarInterpreter + caching

**Parameters**:
- `grammar_text` (required): Complete ANTLR4 grammar
- `sample_inputs` (optional): Specific inputs to test
- `max_inputs` (optional): Maximum test inputs to generate (default: 10)

**Example**:
```
User: Check for ambiguities in this expression grammar
```

**Response**: Found ambiguity in rule 'expr' with input "1+2*3" - can parse as ((1+2)*3) or (1+(2*3))

### 4. analyze_call_graph ⚡

Shows rule dependencies, detects cycles, identifies unused rules.

**Performance**: Fast path (10-50ms) via GrammarInterpreter + caching

**Parameters**:
- `grammar_text` (required): Grammar to analyze
- `output_format` (optional): json (default), dot, or mermaid

**Example**:
```
User: Analyze the rule dependencies in my JSON grammar
```

**Response**: Found 5 rules, 2 unused rules (whitespace, comment), no circular dependencies.

### 5. analyze_complexity ⚡

Provides metrics on grammar complexity including decision points and alternatives.

**Performance**: Fast path (10-50ms) via GrammarInterpreter + caching

**Parameters**:
- `grammar_text` (required): Grammar to analyze

**Example**:
```
User: How complex is this grammar?
```

**Response**: Complexity Score: 7.2/10. 15 rules, 42 decision points, 3 recursive rules, max alternatives: 8

### 6. analyze_left_recursion ⚡

Analyzes left-recursion patterns and ANTLR's automatic transformations.

**Performance**: Fast path (10-50ms) via GrammarInterpreter + caching

**Parameters**:
- `grammar_text` (required): Grammar to analyze

**Example**:
```
User: Explain the left-recursion in my expression grammar
```

**Response**: Found left-recursion in 'expr' rule. ANTLR transforms to precedence levels: [*, /] > [+, -]

### 7. compile_grammar_multi_target

Generates parser code for multiple target languages.

**Performance**: Varies by target language and grammar complexity

**Parameters**:
- `grammarText` (required): Grammar to compile
- `targetLanguage` (required): java, python3, javascript, typescript, cpp, csharp, go, swift, php, dart
- `includeGeneratedCode` (optional): Include full code in response (default: false)

**Example**:
```
User: Generate Python parser code for my JSON grammar
```

**Response**: Generated 4 files: JSONLexer.py, JSONParser.py, JSONListener.py, JSONBaseListener.py

### 8. generate_test_inputs ⚡

Automatically generates sample test inputs for grammar rules.

**Performance**: Fast path (10-50ms) via GrammarInterpreter + caching

**Parameters**:
- `grammar_text` (required): Grammar to generate tests for
- `rule_name` (required): Rule to generate inputs for
- `max_inputs` (optional): Maximum inputs to generate (default: 5)

**Example**:
```
User: Generate test inputs for the 'expr' rule in my calculator grammar
```

**Response**: Generated 5 test inputs: "42", "(123)", "1+2", "3*4", "(5+6)*7"

## Advanced Tools Reference

### 9. visualize_atn 🐌

Shows internal ATN (Augmented Transition Network) structure for advanced debugging.

**Performance**: Slow path (500-2000ms) - requires full compilation

**Parameters**:
- `grammarText` (required): Grammar to visualize
- `ruleName` (required): Rule to visualize ATN for
- `format` (optional): dot (default) or json

**Use Case**: Understanding ANTLR's internal representation for complex grammar debugging.

### 10. visualize_dfa 🐌

Shows decision points and DFA states for grammar analysis.

**Performance**: Slow path (500-2000ms) - requires full compilation

**Parameters**:
- `grammar_text` (required): Grammar to analyze
- `rule_name` (optional): Specific rule to focus on
- `format` (optional): dot (default) or json

**Use Case**: Understanding parsing decisions and optimization opportunities.

## Performance Guide

### Fast Path Tools (⚡ 10-50ms)

These tools use **GrammarInterpreter** with caching for optimal performance:

- `validate_grammar` - Grammar syntax validation
- `parse_sample` - Sample input parsing
- `detect_ambiguity` - Ambiguity detection
- `analyze_call_graph` - Rule dependency analysis
- `analyze_complexity` - Complexity metrics
- `analyze_left_recursion` - Left-recursion analysis
- `generate_test_inputs` - Test case generation

**Benefits**:
- ✅ 10-100x faster than full compilation
- ✅ Automatic caching with 1-hour TTL
- ✅ Suitable for interactive LLM usage
- ✅ No code generation overhead

### Slow Path Tools (🐌 500-2000ms)

These tools require **full compilation** for advanced analysis:

- `visualize_atn` - Internal ATN structure
- `visualize_dfa` - Decision point analysis
- `compile_grammar_multi_target` - Code generation (varies by target)

**When to Use**:
- 🔧 Advanced grammar debugging
- 🔧 Production code generation
- 🔧 Deep performance analysis

### Optimization Strategy

The server automatically uses the **fastest approach possible**:

1. **Try GrammarInterpreter first** (fast path)
2. **Fallback to full compilation** only when needed
3. **Cache results** to avoid repeated work
4. **Log performance** for transparency

Example log output:
```
Using GrammarInterpreter for complexity analysis (fast path)
```

## Common Workflows

### 1. Grammar Development Workflow

**Step 1: Validate Syntax**
```
User: Validate this calculator grammar:
grammar Calculator;
expr : expr '+' expr | NUMBER ;
NUMBER : [0-9]+ ;
```

**Step 2: Test with Samples**
```
User: Parse "2+3" with the calculator grammar using rule 'expr'
```

**Step 3: Check for Issues**
```
User: Check for ambiguities in the calculator grammar
User: Analyze left-recursion in the calculator grammar
```

**Step 4: Optimize Structure**
```
User: Analyze the complexity of the calculator grammar
User: Show rule dependencies in the calculator grammar
```

### 2. Grammar Debugging Workflow

**Step 1: Identify Problem**
```
User: My grammar isn't parsing correctly. Validate this grammar: [grammar text]
```

**Step 2: Test Specific Cases**
```
User: Parse this problematic input: "complex expression" with rule 'expr'
```

**Step 3: Deep Analysis** (if needed)
```
User: Show ATN structure for rule 'expr' in my grammar
User: Visualize DFA for decision points in my grammar
```

### 3. Production Deployment Workflow

**Step 1: Final Validation**
```
User: Validate my production grammar and check for all issues
```

**Step 2: Generate Code**
```
User: Generate Java parser code for my grammar
User: Generate Python parser code for my grammar
```

**Step 3: Create Test Suite**
```
User: Generate test inputs for all major rules in my grammar
```

## Best Practices

### For LLMs and AI Assistants

1. **Start with Core Tools**: Use the 8 essential tools for 90% of use cases
2. **Leverage Fast Path**: Prefer tools marked with ⚡ for interactive usage
3. **Validate Early**: Always validate grammar syntax before other operations
4. **Test Incrementally**: Use parse_sample to test small examples first
5. **Check for Common Issues**: Run detect_ambiguity and analyze_left_recursion

### For Grammar Development

1. **Follow the Workflow**: Validate → Test → Analyze → Optimize
2. **Use Meaningful Examples**: Provide realistic sample inputs for testing
3. **Address Ambiguities**: Fix ambiguities before moving to production
4. **Optimize Structure**: Use call graph analysis to remove unused rules
5. **Generate Comprehensive Tests**: Use generate_test_inputs for coverage

### Performance Optimization

1. **Prefer Fast Tools**: Use ⚡ tools for interactive development
2. **Cache Results**: The server automatically caches for 1 hour
3. **Batch Operations**: Combine multiple analyses in single requests when possible
4. **Use Slow Tools Sparingly**: Reserve 🐌 tools for final debugging/analysis

## Troubleshooting

### Common Issues

**"Grammar validation failed"**
- Check grammar syntax with validate_grammar
- Ensure proper grammar declaration: `grammar GrammarName;`
- Verify rule definitions and token patterns

**"Parse failed"**
- Verify sample input matches expected format
- Check if start rule exists in grammar
- Test with simpler inputs first

**"Ambiguity detected"**
- Use detect_ambiguity to see specific examples
- Consider adding precedence rules or reordering alternatives
- Use analyze_left_recursion for expression grammars

**"Tool timeout"**
- Large grammars may hit timeout limits
- Try breaking down into smaller components
- Use fast path tools when possible

### Performance Issues

**Slow responses**
- Check if using 🐌 slow path tools unnecessarily
- Verify grammar size is reasonable (< 10MB)
- Consider grammar simplification for better performance

**Memory issues**
- Reduce grammar complexity
- Limit sample input size
- Use generate_test_inputs instead of manual large inputs
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

## Advanced Tools Reference

The advanced tools require full grammar compilation and are slower but provide specialized visualization capabilities.

### 1. visualize_atn

Generates ATN (Augmented Transition Network) visualization for grammar rules (🐌 slow - requires compilation).

**Parameters**:
- `grammar_text` (required): Complete ANTLR4 grammar
- `rule_name` (required): Name of rule to visualize
- `format` (optional): 'svg' or 'dot' (default: svg)

**Example**:
```
User: Show me the ATN for the 'expr' rule in my calculator grammar
```

**Response**: Generated ATN visualization showing state transitions and decision points.

### 2. visualize_dfa

Generates DFA (Deterministic Finite Automaton) visualization for parser decision points (🐌 slow - requires compilation).

**Parameters**:
- `grammar_text` (required): Complete ANTLR4 grammar
- `rule_name` (required): Name of rule to analyze
- `format` (optional): 'svg' or 'dot' (default: svg)

**Example**:
```
User: Visualize decision points in my 'statement' rule
```

**Response**: Generated DFA visualization showing decision states and transitions.

##Human: continue