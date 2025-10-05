package com.github.sshailabh.antlr4mcp.service;

import com.github.sshailabh.antlr4mcp.model.AmbiguityVisualization;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.tool.Grammar;
import org.antlr.v4.tool.LexerGrammar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * Service for visualizing grammar ambiguities with parse tree alternatives
 */
@Service
@Slf4j
public class AmbiguityVisualizer {

    @Autowired
    private GrammarCompiler grammarCompiler;

    /**
     * Visualize ambiguities in grammar parsing
     */
    public AmbiguityVisualization visualize(String grammarText, String input, String startRule) {
        log.info("Visualizing ambiguities for input: {}", input);

        try {
            // Load grammar
            Grammar grammar = grammarCompiler.loadGrammar(grammarText);
            if (grammar == null) {
                return AmbiguityVisualization.builder()
                    .success(false)
                    .error("Failed to load grammar")
                    .build();
            }

            // Determine start rule (use first parser rule if not specified)
            if (startRule == null || startRule.isEmpty()) {
                startRule = grammar.getRule(0).name;
            }

            // Verify start rule exists
            if (grammar.getRule(startRule) == null) {
                return AmbiguityVisualization.builder()
                    .success(false)
                    .error("Start rule '" + startRule + "' not found in grammar")
                    .build();
            }

            // Create lexer and parser
            LexerGrammar lexerGrammar = grammar.getImplicitLexer();
            if (lexerGrammar == null) {
                return AmbiguityVisualization.builder()
                    .success(false)
                    .error("Grammar does not have lexer rules")
                    .build();
            }

            Lexer lexer = lexerGrammar.createLexerInterpreter(CharStreams.fromString(input));
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            ParserInterpreter parser = grammar.createParserInterpreter(tokens);

            // Enable exact ambiguity detection
            parser.getInterpreter().setPredictionMode(PredictionMode.LL_EXACT_AMBIG_DETECTION);

            // Collect ambiguities
            List<AmbiguityVisualization.AmbiguityInstance> ambiguities = new ArrayList<>();
            parser.addErrorListener(new ANTLRErrorListener() {
                @Override
                public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
                                      int charPositionInLine, String msg, RecognitionException e) {
                    // Syntax errors handled separately
                }

                @Override
                public void reportAmbiguity(Parser recognizer, DFA dfa, int startIndex, int stopIndex,
                                          boolean exact, BitSet ambigAlts, ATNConfigSet configs) {
                    List<Integer> alts = new ArrayList<>();
                    for (int i = ambigAlts.nextSetBit(0); i >= 0; i = ambigAlts.nextSetBit(i + 1)) {
                        alts.add(i);
                    }

                    String ruleName = recognizer.getRuleNames()[dfa.decision];
                    String ambigText = recognizer.getTokenStream().getText(
                        Interval.of(startIndex, stopIndex)
                    );

                    String explanation = String.format(
                        "Rule '%s' has %d alternative interpretations for input: '%s'",
                        ruleName, alts.size(), ambigText
                    );

                    ambiguities.add(AmbiguityVisualization.AmbiguityInstance.builder()
                        .decision(dfa.decision)
                        .ruleName(ruleName)
                        .startIndex(startIndex)
                        .stopIndex(stopIndex)
                        .ambiguousText(ambigText)
                        .alternativeNumbers(alts)
                        .fullContext(!exact)
                        .explanation(explanation)
                        .build());
                }

                @Override
                public void reportAttemptingFullContext(Parser recognizer, DFA dfa, int startIndex, int stopIndex,
                                                       BitSet conflictingAlts, ATNConfigSet configs) {
                    // Tracked via ambiguity reports
                }

                @Override
                public void reportContextSensitivity(Parser recognizer, DFA dfa, int startIndex, int stopIndex,
                                                    int prediction, ATNConfigSet configs) {
                    // Tracked via ambiguity reports
                }
            });

            // Parse to get primary interpretation
            int ruleIndex = grammar.getRule(startRule).index;
            ParseTree tree = parser.parse(ruleIndex);
            String primaryInterpretation = tree.toStringTree(parser);

            // Build alternative interpretations if ambiguities were found
            List<AmbiguityVisualization.AlternativeInterpretation> alternatives = new ArrayList<>();
            if (!ambiguities.isEmpty()) {
                // For now, we'll note that full alternative tree generation would require
                // custom parsing logic or ANTLR's getAllPossibleParseTrees if available
                // This is a simplified version showing the structure
                log.debug("Found {} ambiguities, primary parse tree generated", ambiguities.size());
            }

            return AmbiguityVisualization.builder()
                .success(true)
                .input(input)
                .startRule(startRule)
                .hasAmbiguities(!ambiguities.isEmpty())
                .ambiguities(ambiguities)
                .primaryInterpretation(primaryInterpretation)
                .alternatives(alternatives)
                .build();

        } catch (Exception e) {
            log.error("Error visualizing ambiguities", e);
            return AmbiguityVisualization.builder()
                .success(false)
                .error("Visualization failed: " + e.getMessage())
                .build();
        }
    }
}
