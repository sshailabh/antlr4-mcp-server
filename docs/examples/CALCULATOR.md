# Calculator Grammar Example

## Grammar Definition

```antlr
grammar Calculator;

expr
    : expr ('*'|'/') expr       # MulDiv
    | expr ('+'|'-') expr       # AddSub
    | NUMBER                     # Num
    | '(' expr ')'              # Parens
    ;

NUMBER : [0-9]+ ('.' [0-9]+)? ;
WS     : [ \t\r\n]+ -> skip ;
```

## Features

- Arithmetic operations: +, -, *, /
- Decimal numbers
- Parentheses for grouping
- Standard operator precedence (multiplication/division before addition/subtraction)

## Example Conversations

### Validation

**User**: Validate my calculator grammar

**Claude**: ✅ Grammar is syntactically valid! Name: `Calculator`

⚠️ Note: This grammar contains left-recursion which creates ambiguity. ANTLR will handle this, but parse results may be unexpected for complex expressions.

### Parsing Simple Expression

**User**: Parse "42 + 10" with Calculator grammar starting from 'expr'

**Claude**:
```
✅ Parse successful!

Parse Tree (LISP format):
(expr
  (expr (NUMBER 42))
  +
  (expr (NUMBER 10)))
```

### Parsing Complex Expression

**User**: Parse "(10 + 20) * 3" with Calculator grammar

**Claude**:
```
✅ Parse successful!

Parse Tree:
(expr
  (expr
    (
    (expr
      (expr (NUMBER 10))
      +
      (expr (NUMBER 20)))
    ))
  *
  (expr (NUMBER 3)))
```

### Ambiguity Detection

**User**: Are there ambiguities in my Calculator grammar?

**Claude**:
```
✅ Ambiguity Analysis Complete

Found 2 ambiguities:

1. **Left-Recursion in 'expr' rule** (Line 3-7)
   - Both MulDiv and AddSub alternatives start with 'expr'
   - Causes non-determinism in parse decisions

   Suggestion: ANTLR handles this automatically, but you can make precedence explicit:

   expr
       : expr ('*'|'/') term    # MulDiv
       | term ('+'|'-') term    # AddSub
       ;

   term
       : NUMBER
       | '(' expr ')'
       ;
```

## Common Issues

### Issue: Unexpected Parse Results

**Symptom**: Expression "2 + 3 * 4" parses as "(2 + 3) * 4" instead of "2 + (3 * 4)"

**Cause**: Ambiguous grammar doesn't enforce precedence

**Solution**: Refactor to use separate rules for different precedence levels (see suggestion above)

### Issue: Parse Errors on Valid Input

**Symptom**: Valid expression fails to parse

**Check**:
1. Verify input matches grammar expectations
2. Check for missing whitespace handling
3. Ensure start rule is correct ('expr' not 'start')

## Testing Checklist

- [ ] Simple arithmetic: "5 + 3"
- [ ] Multiplication: "4 * 7"
- [ ] Parentheses: "(2 + 3) * 4"
- [ ] Decimals: "3.14 + 2.71"
- [ ] Complex: "((10 + 5) * 2) / 3"
- [ ] Edge cases: "0", "0.0", "999.999"
