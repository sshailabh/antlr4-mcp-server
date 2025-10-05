package com.github.sshailabh.antlr4mcp.service;

import com.github.sshailabh.antlr4mcp.model.GrammarError;
import com.github.sshailabh.antlr4mcp.model.ProfileResult;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.atn.DecisionInfo;
import org.antlr.v4.runtime.atn.ParseInfo;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.tool.Grammar;
import org.antlr.v4.tool.LexerGrammar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for profiling grammar performance during parsing
 */
@Service
@Slf4j
public class GrammarProfiler {

    @Autowired
    private GrammarCompiler grammarCompiler;

    /**
     * Profile grammar parsing performance
     */
    public ProfileResult profile(String grammarText, String input, String startRule) {
        log.info("Profiling grammar parsing, start rule: {}", startRule);

        try {
            // Load grammar
            Grammar grammar = grammarCompiler.loadGrammar(grammarText);
            if (grammar == null) {
                return ProfileResult.builder()
                    .success(false)
                    .error("Failed to load grammar")
                    .parsingTimeMs(0)
                    .build();
            }

            // Determine start rule (use first parser rule if not specified)
            if (startRule == null || startRule.isEmpty()) {
                startRule = grammar.getRule(0).name;
            }

            // Verify start rule exists
            if (grammar.getRule(startRule) == null) {
                return ProfileResult.builder()
                    .success(false)
                    .error("Start rule '" + startRule + "' not found in grammar")
                    .parsingTimeMs(0)
                    .build();
            }

            // Create lexer and parser with profiling enabled
            LexerGrammar lexerGrammar = grammar.getImplicitLexer();
            if (lexerGrammar == null) {
                return ProfileResult.builder()
                    .success(false)
                    .error("Grammar does not have lexer rules")
                    .parsingTimeMs(0)
                    .build();
            }

            Lexer lexer = lexerGrammar.createLexerInterpreter(CharStreams.fromString(input));
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            ParserInterpreter parser = grammar.createParserInterpreter(tokens);

            // Enable profiling
            parser.setProfile(true);

            // Track ambiguities
            List<ProfileResult.AmbiguityInfo> ambiguities = new ArrayList<>();
            parser.addErrorListener(new ANTLRErrorListener() {
                @Override
                public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
                                      int charPositionInLine, String msg, RecognitionException e) {
                    // Syntax errors handled separately
                }

                @Override
                public void reportAmbiguity(Parser recognizer, DFA dfa, int startIndex, int stopIndex,
                                          boolean exact, java.util.BitSet ambigAlts, ATNConfigSet configs) {
                    List<Integer> alts = new ArrayList<>();
                    for (int i = ambigAlts.nextSetBit(0); i >= 0; i = ambigAlts.nextSetBit(i + 1)) {
                        alts.add(i);
                    }

                    String ruleName = recognizer.getRuleNames()[dfa.decision];
                    String ambigText = recognizer.getTokenStream().getText(
                        Interval.of(startIndex, stopIndex)
                    );

                    ambiguities.add(ProfileResult.AmbiguityInfo.builder()
                        .decision(dfa.decision)
                        .ruleName(ruleName)
                        .startIndex(startIndex)
                        .stopIndex(stopIndex)
                        .ambigAlts(alts)
                        .ambigText(ambigText)
                        .fullContext(!exact)
                        .build());
                }

                @Override
                public void reportAttemptingFullContext(Parser recognizer, DFA dfa, int startIndex, int stopIndex,
                                                       java.util.BitSet conflictingAlts, ATNConfigSet configs) {
                    // Tracked via DecisionInfo
                }

                @Override
                public void reportContextSensitivity(Parser recognizer, DFA dfa, int startIndex, int stopIndex,
                                                    int prediction, ATNConfigSet configs) {
                    // Tracked via DecisionInfo
                }
            });

            // Parse with timing
            long startTime = System.currentTimeMillis();
            int ruleIndex = grammar.getRule(startRule).index;
            parser.parse(ruleIndex);
            long parseTime = System.currentTimeMillis() - startTime;

            // Get parse info
            ParseInfo parseInfo = parser.getParseInfo();
            DecisionInfo[] decisions = parseInfo.getDecisionInfo();

            // Build decision stats
            List<ProfileResult.DecisionStats> decisionStatsList = new ArrayList<>();
            long totalInvocations = 0;
            long totalTimeInPrediction = 0;
            long totalAmbiguities = 0;
            long totalLlFallbacks = 0;
            long totalFullContextFallbacks = 0;

            for (int i = 0; i < decisions.length; i++) {
                DecisionInfo info = decisions[i];
                if (info.invocations == 0) {
                    continue; // Skip unused decisions
                }

                String ruleName = getRuleNameForDecision(parser, i);

                List<Integer> conflictingAlts = new ArrayList<>();
                if (info.SLL_TotalLook > 0 && info.LL_Fallback > 0) {
                    // Has conflicts
                    conflictingAlts.add(0); // Placeholder - actual alts need more analysis
                }

                decisionStatsList.add(ProfileResult.DecisionStats.builder()
                    .decisionNumber(i)
                    .ruleName(ruleName)
                    .invocations(info.invocations)
                    .timeInPrediction(info.timeInPrediction)
                    .llFallbacks(info.LL_Fallback)
                    .fullContextFallbacks((long) info.contextSensitivities.size())
                    .ambiguities(info.ambiguities.size())
                    .maxLook(info.SLL_MaxLook)
                    .avgLook(info.SLL_TotalLook / (double) Math.max(info.invocations, 1))
                    .totalLook(info.SLL_TotalLook + info.LL_TotalLook)
                    .minLook(info.SLL_MinLook)
                    .maxAlt(0L)  // Not available in DecisionInfo
                    .minAlt(0L)  // Not available in DecisionInfo
                    .conflictingAlts(conflictingAlts)
                    .build());

                totalInvocations += info.invocations;
                totalTimeInPrediction += info.timeInPrediction;
                totalAmbiguities += info.ambiguities.size();
                totalLlFallbacks += info.LL_Fallback;
                totalFullContextFallbacks += info.contextSensitivities.size();
            }

            // Build parser stats
            ProfileResult.ParserStats parserStats = ProfileResult.ParserStats.builder()
                .totalDecisions(decisionStatsList.size())
                .totalInvocations(totalInvocations)
                .totalTimeInPrediction(totalTimeInPrediction)
                .totalAmbiguities(totalAmbiguities)
                .totalLlFallbacks(totalLlFallbacks)
                .totalFullContextFallbacks(totalFullContextFallbacks)
                .parseTimeMs(parseTime)
                .inputSize(input.length() + " characters")
                .build();

            return ProfileResult.builder()
                .success(true)
                .decisionStats(decisionStatsList)
                .parserStats(parserStats)
                .ambiguities(ambiguities)
                .parsingTimeMs(parseTime)
                .build();

        } catch (Exception e) {
            log.error("Error profiling grammar", e);
            return ProfileResult.builder()
                .success(false)
                .error("Profiling failed: " + e.getMessage())
                .parsingTimeMs(0)
                .build();
        }
    }

    /**
     * Get rule name for decision index
     */
    private String getRuleNameForDecision(ParserInterpreter parser, int decision) {
        try {
            int ruleIndex = parser.getATN().decisionToState.get(decision).ruleIndex;
            return parser.getRuleNames()[ruleIndex];
        } catch (Exception e) {
            return "decision_" + decision;
        }
    }
}
