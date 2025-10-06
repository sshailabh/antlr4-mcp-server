package com.github.sshailabh.antlr4mcp.service;

import com.github.sshailabh.antlr4mcp.model.GrammarError;
import com.github.sshailabh.antlr4mcp.model.ParseTrace;
import com.github.sshailabh.antlr4mcp.model.TraceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.tool.Grammar;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for generating parse traces with step-by-step execution details.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParseTracer {

    private final GrammarCompiler grammarCompiler;

    /**
     * Traces the parsing process for a given grammar and input.
     *
     * @param grammarText The complete ANTLR4 grammar text
     * @param sampleInput The input text to parse
     * @param startRule   The start rule name
     * @return ParseTrace containing all trace events
     */
    public ParseTrace trace(String grammarText, String sampleInput, String startRule) {
        log.info("Tracing parse for rule: {} with input length: {}", startRule, sampleInput.length());

        if (grammarText == null || grammarText.trim().isEmpty()) {
            return ParseTrace.builder()
                .success(false)
                .errors(List.of(GrammarError.builder()
                    .type("invalid_input")
                    .message("Grammar text cannot be null or empty")
                    .build()))
                .build();
        }

        if (startRule == null || startRule.trim().isEmpty()) {
            return ParseTrace.builder()
                .success(false)
                .errors(List.of(GrammarError.builder()
                    .type("invalid_input")
                    .message("Start rule cannot be null or empty")
                    .build()))
                .build();
        }

        if (sampleInput == null) {
            sampleInput = "";
        }

        try {
            // Load and process grammar
            Grammar grammar = grammarCompiler.loadGrammar(grammarText);
            if (grammar == null) {
                return ParseTrace.builder()
                    .success(false)
                    .errors(List.of(GrammarError.builder()
                        .type("grammar_error")
                        .message("Failed to load grammar")
                        .build()))
                    .build();
            }

            grammar.tool.process(grammar, false);

            // Verify rule exists
            org.antlr.v4.tool.Rule rule = grammar.getRule(startRule);
            if (rule == null) {
                return ParseTrace.builder()
                    .success(false)
                    .errors(List.of(GrammarError.builder()
                        .type("invalid_rule")
                        .message("Rule '" + startRule + "' not found in grammar")
                        .build()))
                    .build();
            }

            // Create lexer and parser interpreters
            LexerInterpreter lexer = grammar.createLexerInterpreter(CharStreams.fromString(sampleInput));
            if (lexer == null) {
                return ParseTrace.builder()
                    .success(false)
                    .errors(List.of(GrammarError.builder()
                        .type("lexer_error")
                        .message("Failed to create lexer for grammar")
                        .build()))
                    .build();
            }

            CommonTokenStream tokens = new CommonTokenStream(lexer);
            ParserInterpreter parser = grammar.createParserInterpreter(tokens);

            // Create custom trace listener
            TraceListener traceListener = new TraceListener(grammar);
            parser.addParseListener(traceListener);

            // Enable tracing (optional, for more detailed output)
            parser.setTrace(true);

            // Parse with the specified rule
            ParserRuleContext tree = parser.parse(rule.index);

            // Get trace events
            List<TraceEvent> events = traceListener.getEvents();
            log.info("Parse trace completed with {} events", events.size());

            return ParseTrace.builder()
                .success(true)
                .events(events)
                .grammarName(grammar.name)
                .startRule(startRule)
                .input(sampleInput)
                .totalSteps(events.size())
                .build();

        } catch (Exception e) {
            log.error("Parse tracing failed for rule '{}': {}", startRule, e.getMessage(), e);
            return ParseTrace.builder()
                .success(false)
                .errors(List.of(GrammarError.builder()
                    .type("trace_error")
                    .message("Parse trace error: " + e.getMessage())
                    .build()))
                .build();
        }
    }

    /**
     * Custom ParseTreeListener to capture trace events.
     */
    private static class TraceListener implements ParseTreeListener {
        private final List<TraceEvent> events = new ArrayList<>();
        private final Grammar grammar;
        private int depth = 0;
        private int step = 0;
        private long startTime = System.currentTimeMillis();

        public TraceListener(Grammar grammar) {
            this.grammar = grammar;
        }

        public List<TraceEvent> getEvents() {
            return events;
        }

        @Override
        public void enterEveryRule(ParserRuleContext ctx) {
            String ruleName = getRuleName(ctx);
            Token startToken = ctx.getStart();

            events.add(TraceEvent.builder()
                .step(step++)
                .type("ENTER_RULE")
                .ruleName(ruleName)
                .depth(depth)
                .line(startToken != null ? startToken.getLine() : null)
                .column(startToken != null ? startToken.getCharPositionInLine() : null)
                .timestamp(System.currentTimeMillis() - startTime)
                .message("Entering rule: " + ruleName)
                .build());

            depth++;
        }

        @Override
        public void exitEveryRule(ParserRuleContext ctx) {
            depth--;
            String ruleName = getRuleName(ctx);

            events.add(TraceEvent.builder()
                .step(step++)
                .type("EXIT_RULE")
                .ruleName(ruleName)
                .depth(depth)
                .timestamp(System.currentTimeMillis() - startTime)
                .message("Exiting rule: " + ruleName)
                .build());
        }

        @Override
        public void visitTerminal(TerminalNode node) {
            Token symbol = node.getSymbol();
            String tokenText = node.getText();

            // Special handling for EOF
            if (symbol.getType() == Token.EOF) {
                events.add(TraceEvent.builder()
                    .step(step++)
                    .type("CONSUME_TOKEN")
                    .tokenText("<EOF>")
                    .tokenType(symbol.getType())
                    .depth(depth)
                    .line(symbol.getLine())
                    .column(symbol.getCharPositionInLine())
                    .timestamp(System.currentTimeMillis() - startTime)
                    .message("End of input reached")
                    .build());
            } else {
                events.add(TraceEvent.builder()
                    .step(step++)
                    .type("CONSUME_TOKEN")
                    .tokenText(tokenText)
                    .tokenType(symbol.getType())
                    .depth(depth)
                    .line(symbol.getLine())
                    .column(symbol.getCharPositionInLine())
                    .timestamp(System.currentTimeMillis() - startTime)
                    .message("Consumed token: '" + tokenText + "'")
                    .build());
            }
        }

        @Override
        public void visitErrorNode(ErrorNode node) {
            Token symbol = node.getSymbol();
            String tokenText = node.getText();

            events.add(TraceEvent.builder()
                .step(step++)
                .type("ERROR")
                .tokenText(tokenText)
                .tokenType(symbol != null ? symbol.getType() : null)
                .depth(depth)
                .line(symbol != null ? symbol.getLine() : null)
                .column(symbol != null ? symbol.getCharPositionInLine() : null)
                .timestamp(System.currentTimeMillis() - startTime)
                .message("Error at token: '" + tokenText + "'")
                .build());
        }

        private String getRuleName(ParserRuleContext ctx) {
            int ruleIndex = ctx.getRuleIndex();
            if (ruleIndex >= 0 && grammar.rules != null) {
                // Get rule names from grammar
                org.antlr.v4.tool.Rule[] rulesArray = grammar.rules.values().toArray(new org.antlr.v4.tool.Rule[0]);
                if (ruleIndex < rulesArray.length) {
                    return rulesArray[ruleIndex].name;
                }
            }
            return "rule_" + ruleIndex;
        }
    }
}