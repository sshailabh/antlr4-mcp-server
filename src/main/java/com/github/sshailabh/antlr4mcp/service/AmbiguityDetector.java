package com.github.sshailabh.antlr4mcp.service;

import com.github.sshailabh.antlr4mcp.model.Ambiguity;
import com.github.sshailabh.antlr4mcp.model.AmbiguityReport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.tool.Grammar;
import org.antlr.v4.tool.LexerGrammar;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AmbiguityDetector {

    private final GrammarCompiler grammarCompiler;

    public AmbiguityReport analyze(String grammarText) {
        return analyzeWithSamples(grammarText, null);
    }

    public AmbiguityReport analyzeWithSamples(String grammarText, List<String> sampleInputs) {
        log.info("Analyzing grammar for ambiguities with {} samples",
            sampleInputs != null ? sampleInputs.size() : 0);

        try {
            Grammar grammar = grammarCompiler.loadGrammar(grammarText);
            if (grammar == null) {
                return AmbiguityReport.error("Failed to load grammar");
            }

            List<Ambiguity> allAmbiguities = new ArrayList<>();

            if (sampleInputs != null && !sampleInputs.isEmpty()) {
                for (String input : sampleInputs) {
                    List<Ambiguity> detected = detectAmbiguitiesInInput(grammar, input);
                    allAmbiguities.addAll(detected);
                }
            } else {
                String defaultInput = generateDefaultInput(grammar);
                List<Ambiguity> detected = detectAmbiguitiesInInput(grammar, defaultInput);
                allAmbiguities.addAll(detected);
            }

            return allAmbiguities.isEmpty()
                ? AmbiguityReport.noAmbiguities()
                : AmbiguityReport.withAmbiguities(allAmbiguities);

        } catch (Exception e) {
            log.error("Ambiguity detection failed", e);
            return AmbiguityReport.error("Analysis error: " + e.getMessage());
        }
    }

    private List<Ambiguity> detectAmbiguitiesInInput(Grammar grammar, String input) {
        List<Ambiguity> ambiguities = new ArrayList<>();

        try {
            LexerGrammar lexerGrammar = grammar.getImplicitLexer();
            if (lexerGrammar == null) {
                return ambiguities;
            }

            Lexer lexer = lexerGrammar.createLexerInterpreter(CharStreams.fromString(input));
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            ParserInterpreter parser = grammar.createParserInterpreter(tokens);

            parser.getInterpreter().setPredictionMode(PredictionMode.LL_EXACT_AMBIG_DETECTION);

            parser.addErrorListener(new ANTLRErrorListener() {
                @Override
                public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
                                      int charPositionInLine, String msg, RecognitionException e) {
                }

                @Override
                public void reportAmbiguity(Parser recognizer, DFA dfa, int startIndex, int stopIndex,
                                          boolean exact, BitSet ambigAlts, ATNConfigSet configs) {
                    List<Integer> alts = new ArrayList<>();
                    for (int i = ambigAlts.nextSetBit(0); i >= 0; i = ambigAlts.nextSetBit(i + 1)) {
                        alts.add(i);
                    }

                    String ruleName = recognizer.getRuleNames()[dfa.decision];
                    String ambigText = recognizer.getTokenStream().getText(Interval.of(startIndex, stopIndex));

                    String explanation = String.format(
                        "Rule '%s' has %d alternative interpretations for input '%s'",
                        ruleName, alts.size(), ambigText
                    );

                    ambiguities.add(Ambiguity.builder()
                        .ruleName(ruleName)
                        .conflictingAlternatives(alts)
                        .explanation(explanation)
                        .sampleInput(ambigText)
                        .build());
                }

                @Override
                public void reportAttemptingFullContext(Parser recognizer, DFA dfa, int startIndex, int stopIndex,
                                                       BitSet conflictingAlts, ATNConfigSet configs) {
                }

                @Override
                public void reportContextSensitivity(Parser recognizer, DFA dfa, int startIndex, int stopIndex,
                                                    int prediction, ATNConfigSet configs) {
                }
            });

            int ruleIndex = grammar.getRule(0).index;
            parser.parse(ruleIndex);

        } catch (Exception e) {
            log.warn("Error detecting ambiguities in input: {}", input, e);
        }

        return ambiguities;
    }

    private String generateDefaultInput(Grammar grammar) {
        return "test";
    }
}
