# Enhancements / Cleanup Notes (ANTLR4 MCP Server)

This document captures what’s **already sufficient** for real grammar/DSL work, what was cleaned up, and what enhancements would be most valuable next.

---

## What’s sufficient today

For many DSL projects, the current server is already useful if you stick to a tight loop:

- **Validate** grammar text (`validate_grammar`)
- **Parse** representative samples quickly (interpreter mode via `parse_sample`)
- **Analyze** when things get tricky:
  - `detect_ambiguity`
  - `analyze_left_recursion`
  - `analyze_first_follow`
  - `analyze_call_graph`
- **Generate** a real parser when the grammar stabilizes (`compile_grammar_multi_target`)
- **Visualize** complex rules with `visualize_atn`

If you keep your grammar in one file and keep sample inputs small, this workflow is already productive.

---

## Fixes / improvements made in this repo

- **Docker made runnable and safer-by-default**
  - Multi-stage Docker build (builds the JAR inside Docker)
  - Added `.dockerignore`
  - `docker/run.sh` now runs with `-i` (no TTY), suitable for stdio MCP
- **Logging made MCP-safe**
  - Default log file moved to `${java.io.tmpdir}` (writable in Docker and local runs)
  - Added `logback-spring.xml` to avoid stdout logging (stdout must stay clean for JSON-RPC)
- **Tool parameter naming consistency**
  - Standardized the public tool schemas to **snake_case** for `compile_grammar_multi_target` and `visualize_atn`
  - Kept backwards compatibility by still accepting legacy camelCase args
- **Codegen ergonomics**
  - `compile_grammar_multi_target` now supports:
    - `package_name` (Java)
    - `generate_listener`
    - `generate_visitor`
  - Target language parsing accepts friendly aliases like `python` → `Python3`
- **Project metadata cleanup**
  - `pom.xml` license metadata updated to Apache 2.0 to match `LICENSE`

---

## High-value enhancements to consider next

### 1) Multi-file grammars / import resolution

Right now, the common workaround is “inline everything into one grammar text.” That’s fine for small DSLs, but painful for larger grammars.

A next step could be:
- accept a **set of files** (name → content) in tool args, or
- accept a root grammar + an “imports map,” and resolve imports in a temp directory.

### 2) Better output ergonomics for generated code

Returning full generated source via MCP can create large JSON payloads.

Ideas:
- return a file manifest + content hashes by default
- support “fetch file content by name”
- support output chunking/paging for big targets

### 3) Caching across tool calls

Many workflows call multiple tools on the same grammar repeatedly.

Cache candidates:
- grammar parse/validation artifacts
- interpreter structures used by `parse_sample`

This can improve latency significantly for repeated iterations in an editor.

### 4) Richer parse diagnostics

`parse_sample` could be improved by returning:
- structured tokens (array of `{type,text,line,column,...}`)
- structured parse errors (offending token, expected tokens)
- optional parse tree formats (LISP vs JSON tree)

### 5) Observability controls

The “stdout must be clean” constraint is real, but debugging is also important.

A good approach is:
- keep logs off stdout always
- optionally emit logs to stderr (or file) when `DEBUG` mode is enabled

---

## Bottom line

The **core toolset is sufficient** for building a DSL today, especially with the included `dsl-starter/` example.

The next enhancements that provide the best ROI are:
- multi-file grammars/imports
- caching
- better codegen output ergonomics


