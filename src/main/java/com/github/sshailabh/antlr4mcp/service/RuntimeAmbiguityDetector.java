package com.github.sshailabh.antlr4mcp.service;

import com.github.sshailabh.antlr4mcp.model.Ambiguity;
import com.github.sshailabh.antlr4mcp.model.AmbiguityReport;
import com.github.sshailabh.antlr4mcp.model.CoverageInfo;
import com.github.sshailabh.antlr4mcp.model.InterpreterResult;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.misc.IntervalSet;
import org.antlr.v4.tool.Grammar;
import org.antlr.v4.tool.Rule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Runtime ambiguity detector using ANTLR's ProfilingATNSimulator (Phase 2).
 *
 * This replaces the static AST analysis approach which doesn't work due to
 * ANTLR's grammar transformation pipeline (LeftRecursiveRuleTransformer runs
 * before any analysis can occur).
 *
 * Uses the same approach as IntelliJ ANTLR plugin - parse sample inputs with
 * profiling enabled and extract actual runtime ambiguities from DecisionInfo.
 */
@Service
@Slf4j
public class RuntimeAmbiguityDetector {

    private final GrammarInterpreter grammarInterpreter;
    private final ParseTimeoutManager timeoutManager;

    @Autowired
    public RuntimeAmbiguityDetector(
        GrammarInterpreter grammarInterpreter,
        ParseTimeoutManager timeoutManager
    ) {
        this.grammarInterpreter = grammarInterpreter;
        this.timeoutManager = timeoutManager;
    }

    /**
     * Detect ambiguities by parsing sample inputs with profiling enabled.
     *
     * @param grammarText Grammar content
     * @param startRule Starting rule name
     * @param sampleInputs List of sample inputs to parse
     * @param timeoutPerSample Timeout in seconds per sample
     * @return Report of detected ambiguities
     */
    public AmbiguityReport detectWithSamples(
        String grammarText,
        String startRule,
        List<String> sampleInputs,
        int timeoutPerSample
    ) {
        log.info("Detecting ambiguities with {} samples for rule: {}",
                 sampleInputs != null ? sampleInputs.size() : 0, startRule);

        // 1. Load grammar using Phase 1 infrastructure
        InterpreterResult interpreterResult;
        try {
            interpreterResult = grammarInterpreter.createInterpreter(grammarText);
        } catch (Exception e) {
            log.error("Failed to load grammar: {}", e.getMessage());
            throw new RuntimeException("Failed to load grammar for ambiguity detection", e);
        }
        Grammar grammar = interpreterResult.getGrammar();

        // 2. Get rule index
        Rule rule = grammar.getRule(startRule);
        if (rule == null) {
            throw new IllegalArgumentException("Rule not found: " + startRule);
        }
        int ruleIndex = rule.index;

        // 3. Parse each sample with profiling
        List<Ambiguity> allAmbiguities = new ArrayList<>();
        int samplesParsed = 0;
        long totalTime = 0;
        CoverageInfo coverage = CoverageInfo.builder().build();

        if (sampleInputs == null || sampleInputs.isEmpty()) {
            log.warn("No sample inputs provided, returning empty report");
            return AmbiguityReport.builder()
                .hasAmbiguities(false)
                .ambiguities(new ArrayList<>())
                .ambiguitiesPerRule(new HashMap<>())
                .totalSamplesParsed(0)
                .totalParseTimeMs(0L)
                .coverage(coverage)
                .build();
        }

        for (String input : sampleInputs) {
            try {
                long startTime = System.currentTimeMillis();

                List<Ambiguity> ambiguities = timeoutManager.executeWithTimeout(
                    () -> parseWithProfiling(grammar, input, ruleIndex, coverage),
                    timeoutPerSample
                );

                allAmbiguities.addAll(ambiguities);
                samplesParsed++;
                totalTime += System.currentTimeMillis() - startTime;

                log.debug("Parsed sample {} of {}: {} ambiguities detected",
                         samplesParsed, sampleInputs.size(), ambiguities.size());

            } catch (Exception e) {
                // Log timeout or other errors, continue with other samples
                log.warn("Error parsing sample '{}': {}",
                        input.substring(0, Math.min(50, input.length())),
                        e.getMessage());
            }
        }

        // 4. Build report
        Map<String, Integer> ambiguitiesPerRule = new HashMap<>();
        for (Ambiguity amb : allAmbiguities) {
            if (amb.getRuleName() != null) {
                ambiguitiesPerRule.merge(amb.getRuleName(), 1, Integer::sum);
            }
        }

        AmbiguityReport report = AmbiguityReport.builder()
            .hasAmbiguities(!allAmbiguities.isEmpty())
            .ambiguities(allAmbiguities)
            .ambiguitiesPerRule(ambiguitiesPerRule)
            .totalSamplesParsed(samplesParsed)
            .totalParseTimeMs(totalTime)
            .coverage(coverage)
            .build();

        log.info("Ambiguity detection complete: {} ambiguities in {} rules from {} samples",
                 allAmbiguities.size(), ambiguitiesPerRule.size(), samplesParsed);

        return report;
    }

    /**
     * Parse input with profiling enabled and extract ambiguities.
     */
    private List<Ambiguity> parseWithProfiling(
        Grammar grammar,
        String input,
        int ruleIndex,
        CoverageInfo coverage
    ) {
        // 1. Create lexer interpreter
        CharStream charStream = CharStreams.fromString(input);
        LexerInterpreter lexer = grammar.createLexerInterpreter(charStream);
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        // 2. Create parser interpreter with profiling
        ParserInterpreter parser = grammar.createParserInterpreter(tokens);
        parser.setProfile(true); // Enable ProfilingATNSimulator

        // 3. Parse
        try {
            parser.parse(ruleIndex);
        } catch (Exception e) {
            log.debug("Parse error (expected for ambiguous grammars): {}", e.getMessage());
            // Continue - we still want profiling data
        }

        // 4. Extract profiling data
        ProfilingATNSimulator profiler =
            (ProfilingATNSimulator) parser.getInterpreter();
        DecisionInfo[] decisions = profiler.getDecisionInfo();

        // 5. Transform to Ambiguity objects
        List<Ambiguity> ambiguities = new ArrayList<>();
        for (DecisionInfo decision : decisions) {
            if (decision.ambiguities != null && !decision.ambiguities.isEmpty()) {
                for (AmbiguityInfo ambigInfo : decision.ambiguities) {
                    Ambiguity ambiguity = transformAmbiguityInfo(
                        ambigInfo, tokens, grammar, decision
                    );
                    ambiguities.add(ambiguity);

                    log.debug("Found ambiguity in rule {}: alternatives {} at tokens {}-{}",
                             ambiguity.getRuleName(),
                             ambiguity.getConflictingAlternatives(),
                             ambigInfo.startIndex, ambigInfo.stopIndex);
                }
            }

            // Track coverage
            String ruleName = getRuleNameForDecision(grammar, decision.decision);
            if (ruleName != null) {
                coverage.addVisitedRule(ruleName);
            }
        }

        return ambiguities;
    }

    /**
     * Transform ANTLR's AmbiguityInfo to our Ambiguity model.
     */
    private Ambiguity transformAmbiguityInfo(
        AmbiguityInfo info,
        TokenStream tokens,
        Grammar grammar,
        DecisionInfo decision
    ) {
        // Map token indices to line/column
        Token startToken = null;
        Token stopToken = null;
        try {
            startToken = tokens.get(info.startIndex);
            stopToken = tokens.get(info.stopIndex);
        } catch (Exception e) {
            log.debug("Could not get tokens for ambiguity: {}", e.getMessage());
        }

        // Get rule name for this decision
        String ruleName = getRuleNameForDecision(grammar, decision.decision);

        // Extract conflicting alternatives
        List<Integer> conflictingAlts = new ArrayList<>();
        if (info.ambigAlts != null) {
            for (int i = info.ambigAlts.nextSetBit(0); i >= 0;
                 i = info.ambigAlts.nextSetBit(i + 1)) {
                conflictingAlts.add(i);
            }
        }

        // Get input text
        String inputText = null;
        if (startToken != null && stopToken != null) {
            try {
                inputText = tokens.getText(startToken, stopToken);
            } catch (Exception e) {
                log.debug("Could not extract input text: {}", e.getMessage());
            }
        }

        // Build ambiguity
        Ambiguity.AmbiguityBuilder builder = Ambiguity.builder()
            .ruleName(ruleName)
            .conflictingAlternatives(conflictingAlts)
            .isFullContext(info.fullCtx)
            .startIndex(info.startIndex)
            .stopIndex(info.stopIndex)
            .inputText(inputText);

        // Add line/column if available
        if (startToken != null) {
            builder.line(startToken.getLine())
                   .column(startToken.getCharPositionInLine());
        }

        // Generate explanation
        String explanation = generateExplanation(ruleName, conflictingAlts, inputText);
        builder.explanation(explanation);

        // Generate suggested fix
        String suggestedFix = generateSuggestedFix(ruleName, conflictingAlts);
        builder.suggestedFix(suggestedFix);

        return builder.build();
    }

    /**
     * Get rule name for a decision number.
     */
    private String getRuleNameForDecision(Grammar grammar, int decisionNumber) {
        try {
            DecisionState decisionState = grammar.atn.getDecisionState(decisionNumber);
            if (decisionState == null) {
                return "unknown";
            }

            // Find rule containing this decision
            for (Rule rule : grammar.rules.values()) {
                ATNState startState = grammar.atn.ruleToStartState[rule.index];
                if (containsDecision(startState, decisionState, new HashSet<>())) {
                    return rule.name;
                }
            }
        } catch (Exception e) {
            log.debug("Could not find rule for decision {}: {}", decisionNumber, e.getMessage());
        }

        return "unknown";
    }

    /**
     * Check if a rule's ATN subgraph contains a decision.
     */
    private boolean containsDecision(
        ATNState start,
        DecisionState target,
        Set<ATNState> visited
    ) {
        if (start == target) {
            return true;
        }

        if (visited.contains(start)) {
            return false;
        }
        visited.add(start);

        // Stop at rule boundaries
        if (start instanceof RuleStopState) {
            return false;
        }

        // Check all transitions
        for (Transition transition : start.getTransitions()) {
            if (containsDecision(transition.target, target, visited)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Generate human-readable explanation of ambiguity.
     */
    private String generateExplanation(
        String ruleName,
        List<Integer> conflictingAlts,
        String inputText
    ) {
        if (conflictingAlts == null || conflictingAlts.isEmpty()) {
            return "Ambiguity detected in rule " + ruleName;
        }

        StringBuilder explanation = new StringBuilder();
        explanation.append("In rule '").append(ruleName).append("', ");
        explanation.append("alternatives ");
        explanation.append(conflictingAlts.toString());
        explanation.append(" are ambiguous");

        if (inputText != null && !inputText.isEmpty()) {
            explanation.append(" for input: \"").append(inputText).append("\"");
        }

        return explanation.toString();
    }

    /**
     * Generate suggested fix for ambiguity.
     */
    private String generateSuggestedFix(String ruleName, List<Integer> conflictingAlts) {
        StringBuilder fix = new StringBuilder();
        fix.append("Consider:\n");
        fix.append("1. Reordering alternatives to resolve ambiguity\n");
        fix.append("2. Adding semantic predicates to disambiguate\n");
        fix.append("3. Using precedence for operator expressions\n");
        fix.append("4. Factoring common prefixes if possible");
        return fix.toString();
    }

    /**
     * Detect ambiguities using auto-generated test inputs (Phase 2 future).
     * Currently placeholder - full implementation requires TestInputGenerator.
     */
    public AmbiguityReport detectWithAutoGeneration(
        String grammarText,
        String startRule,
        int numSamples
    ) {
        log.info("Auto-generation requested but TestInputGenerator not yet implemented");
        log.info("Returning empty report - provide sample inputs manually for now");

        // Placeholder: Will be implemented with TestInputGenerator in Week 4
        return AmbiguityReport.builder()
            .hasAmbiguities(false)
            .ambiguities(new ArrayList<>())
            .ambiguitiesPerRule(new HashMap<>())
            .totalSamplesParsed(0)
            .totalParseTimeMs(0L)
            .build();
    }
}
