package com.github.sshailabh.antlr4mcp.service;

import com.github.sshailabh.antlr4mcp.model.GrammarError;
import com.github.sshailabh.antlr4mcp.model.ParseResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.tool.Grammar;
import org.antlr.v4.tool.LexerGrammar;
import org.antlr.v4.tool.Rule;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Fast interpreter-based parser using ANTLR4's ParserInterpreter.
 * 
 * This is 10-100x faster than compiling to Java and running TestRig
 * because it uses ANTLR4's built-in interpreter mode.
 * 
 * From ANTLR4 documentation:
 * "For small parsing tasks it is sometimes convenient to use ANTLR in 
 * interpreted mode, rather than generating a parser in a particular target."
 * 
 * Key classes:
 * - LexerInterpreter: Executes lexer rules without code generation
 * - ParserInterpreter: Executes parser rules without code generation
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class InterpreterParser {

    private final GrammarCompiler grammarCompiler;

    /**
     * Parse input using interpreter mode (fast path).
     * 
     * @param grammarText Complete ANTLR4 grammar text
     * @param input Input text to parse
     * @param startRule Name of the start rule
     * @return ParseResult with parse tree and any errors
     */
    public ParseResult parseInterpreted(String grammarText, String input, String startRule) {
        log.info("Parsing with interpreter mode, startRule: {}", startRule);

        try {
            // Load grammar
            Grammar grammar = grammarCompiler.loadGrammar(grammarText);
            if (grammar == null) {
                return ParseResult.builder()
                    .success(false)
                    .errors(List.of(GrammarError.builder()
                        .type("grammar_error")
                        .message("Failed to load grammar")
                        .build()))
                    .build();
            }

            // Validate start rule
            Rule rule = grammar.getRule(startRule);
            if (rule == null) {
                List<String> availableRules = new ArrayList<>();
                for (Rule r : grammar.rules.values()) {
                    if (Character.isLowerCase(r.name.charAt(0))) {
                        availableRules.add(r.name);
                    }
                }
                
                return ParseResult.builder()
                    .success(false)
                    .errors(List.of(GrammarError.builder()
                        .type("unknown_rule")
                        .message("Unknown start rule: " + startRule + 
                                ". Available parser rules: " + String.join(", ", availableRules))
                        .build()))
                    .build();
            }

            // Get lexer grammar (implicit for combined grammars)
            LexerGrammar lexerGrammar = grammar.getImplicitLexer();
            if (lexerGrammar == null) {
                return ParseResult.builder()
                    .success(false)
                    .errors(List.of(GrammarError.builder()
                        .type("grammar_error")
                        .message("Grammar does not have lexer rules (lexer grammar required)")
                        .build()))
                    .build();
            }

            // Create lexer and tokenize
            Lexer lexer = lexerGrammar.createLexerInterpreter(CharStreams.fromString(input));
            
            // Collect lexer errors
            List<GrammarError> errors = new ArrayList<>();
            lexer.removeErrorListeners();
            lexer.addErrorListener(new BaseErrorListener() {
                @Override
                public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                       int line, int charPositionInLine, String msg,
                                       RecognitionException e) {
                    errors.add(GrammarError.builder()
                        .type("lexer_error")
                        .line(line)
                        .column(charPositionInLine)
                        .message("Lexer error: " + msg)
                        .build());
                }
            });

            CommonTokenStream tokens = new CommonTokenStream(lexer);

            // Create parser
            ParserInterpreter parser = grammar.createParserInterpreter(tokens);
            parser.removeErrorListeners();
            parser.addErrorListener(new BaseErrorListener() {
                @Override
                public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                       int line, int charPositionInLine, String msg,
                                       RecognitionException e) {
                    errors.add(GrammarError.builder()
                        .type("syntax_error")
                        .line(line)
                        .column(charPositionInLine)
                        .message("Parse error at line " + line + ":" + charPositionInLine + " - " + msg)
                        .suggestedFix(getSuggestedFix(msg))
                        .build());
                }
            });

            // Parse
            int ruleIndex = rule.index;
            ParseTree tree = parser.parse(ruleIndex);

            // Convert to LISP-style string
            String parseTree = tree.toStringTree(parser);

            // Get tokens for optional output
            tokens.fill();
            StringBuilder tokenOutput = new StringBuilder();
            for (Token token : tokens.getTokens()) {
                if (token.getType() != Token.EOF) {
                    String tokenName = lexerGrammar.getTokenDisplayName(token.getType());
                    tokenOutput.append(String.format("[@%d,%d:%d='%s',<%s>,%d:%d]%n",
                        token.getTokenIndex(),
                        token.getStartIndex(),
                        token.getStopIndex(),
                        token.getText(),
                        tokenName,
                        token.getLine(),
                        token.getCharPositionInLine()));
                }
            }

            return ParseResult.builder()
                .success(errors.isEmpty())
                .parseTree(parseTree)
                .tokens(tokenOutput.toString())
                .errors(errors.isEmpty() ? null : errors)
                .build();

        } catch (Exception e) {
            log.error("Interpreter parse failed", e);
            return ParseResult.builder()
                .success(false)
                .errors(List.of(GrammarError.builder()
                    .type("internal_error")
                    .message("Parse error: " + e.getMessage())
                    .build()))
                .build();
        }
    }

    private String getSuggestedFix(String message) {
        if (message.contains("missing")) {
            return "Add the expected token at this location";
        } else if (message.contains("extraneous")) {
            return "Remove the unexpected token";
        } else if (message.contains("no viable alternative")) {
            return "Check syntax - the input doesn't match any grammar rule at this point";
        } else if (message.contains("mismatched input")) {
            return "The token at this position doesn't match what the grammar expects";
        }
        return "Review the input at this location";
    }
}

