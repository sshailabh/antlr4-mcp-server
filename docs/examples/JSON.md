# JSON Grammar Example

## Grammar Definition

```antlr
grammar JsonSubset;

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

## Features

- Objects with key-value pairs
- Arrays with mixed types
- Strings with escape sequences
- Numbers (integers, decimals, scientific notation)
- Boolean values (true/false)
- Null value
- Whitespace handling

## Example Conversations

### Validation

**User**: Validate this JSON grammar

**Claude**: ✅ Grammar is valid! Name: `JsonSubset`

No syntax errors or undefined rules detected. This grammar implements a subset of JSON specification.

### Parsing JSON Object

**User**: Parse this JSON: `{"name": "Alice", "age": 30}`

**Claude**:
```
✅ Parse successful!

Parse Tree:
(json
  (value
    (object
      {
      (pair "name" : (value "Alice"))
      ,
      (pair "age" : (value (NUMBER 30)))
      })))
```

### Parsing JSON Array

**User**: Parse `[1, 2, "test", true, null]` with JsonSubset grammar

**Claude**:
```
✅ Parse successful!

Parse Tree:
(json
  (value
    (array
      [
      (value (NUMBER 1))
      ,
      (value (NUMBER 2))
      ,
      (value "test")
      ,
      (value true)
      ,
      (value null)
      ])))
```

### Parsing Nested JSON

**User**: Parse nested JSON: `{"user": {"name": "Bob"}, "items": [1, 2, 3]}`

**Claude**:
```
✅ Parse successful!

Parse Tree shows proper nesting:
- Outer object contains two pairs
  - "user" maps to nested object
  - "items" maps to array of numbers
```

### Ambiguity Analysis

**User**: Check my JSON grammar for ambiguities

**Claude**: ✅ No ambiguities detected!

The grammar is well-structured with clear alternatives in the 'value' rule. Each alternative has distinct starting tokens, preventing parsing conflicts.

## Common Issues

### Issue: String Parsing Failures

**Symptom**: Valid JSON strings fail to parse

**Causes**:
- Missing escape for backslash in string content
- Unescaped quotes within strings

**Solution**: The grammar handles standard JSON escapes. Ensure input follows JSON string format.

### Issue: Number Format Errors

**Symptom**: Valid numbers like "0.5" or "1e10" fail

**Check**: The grammar supports:
- Integers: `42`, `-17`
- Decimals: `3.14`, `-0.5`
- Scientific: `1e10`, `2.5E-3`

### Issue: Trailing Commas

**Symptom**: Arrays/objects with trailing commas fail to parse

**Note**: This grammar follows strict JSON specification - trailing commas are not allowed:
- ❌ `[1, 2, 3,]`
- ✅ `[1, 2, 3]`

## Testing Checklist

### Basic Values
- [ ] String: `"hello"`
- [ ] Number: `42`
- [ ] Boolean: `true`, `false`
- [ ] Null: `null`

### Objects
- [ ] Empty: `{}`
- [ ] Single pair: `{"key": "value"}`
- [ ] Multiple pairs: `{"a": 1, "b": 2}`
- [ ] Nested: `{"outer": {"inner": "value"}}`

### Arrays
- [ ] Empty: `[]`
- [ ] Numbers: `[1, 2, 3]`
- [ ] Mixed types: `[1, "two", true, null]`
- [ ] Nested: `[[1, 2], [3, 4]]`

### Complex
- [ ] Mixed nesting: `{"users": [{"name": "Alice"}, {"name": "Bob"}]}`
- [ ] Scientific numbers: `{"value": 1.5e10}`
- [ ] Escaped strings: `{"path": "C:\\\\Users\\\\test"}`

## Best Practices

1. **Always validate input** before parsing
2. **Use proper escape sequences** in test data
3. **Test edge cases**: empty objects/arrays, deeply nested structures
4. **Check number formats**: decimals, negatives, scientific notation
