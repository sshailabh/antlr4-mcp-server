package com.github.sshailabh.antlr4mcp.analysis;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Analyzes and handles embedded language code in ANTLR grammars
 */
@Component
@Slf4j
public class EmbeddedCodeAnalyzer {

    private static final Pattern ACTION_PATTERN = Pattern.compile(
        "@(header|members|init|after)\\s*\\{([^}]+)\\}",
        Pattern.DOTALL
    );

    private static final Pattern SEMANTIC_PREDICATE_PATTERN = Pattern.compile(
        "\\{([^}]+)\\}\\?",
        Pattern.DOTALL
    );

    private static final Pattern INLINE_ACTION_PATTERN = Pattern.compile(
        "\\{([^}]+?)\\}",
        Pattern.DOTALL
    );

    /**
     * Analyze embedded code in grammar
     */
    public EmbeddedCodeReport analyze(String grammarText) {
        log.info("Analyzing embedded code in grammar");

        EmbeddedCodeReport report = new EmbeddedCodeReport();

        // Extract actions
        Matcher actionMatcher = ACTION_PATTERN.matcher(grammarText);
        while (actionMatcher.find()) {
            String actionType = actionMatcher.group(1);
            String actionCode = actionMatcher.group(2).trim();
            report.addAction(actionType, actionCode, actionMatcher.start());
        }

        // Extract semantic predicates
        Matcher predicateMatcher = SEMANTIC_PREDICATE_PATTERN.matcher(grammarText);
        while (predicateMatcher.find()) {
            String predicateCode = predicateMatcher.group(1).trim();
            report.addPredicate(predicateCode, predicateMatcher.start());
        }

        // Count inline actions (excluding predicates)
        int inlineActionCount = countInlineActions(grammarText);
        report.setInlineActionCount(inlineActionCount);

        // Detect language from code patterns
        report.setDetectedLanguage(detectLanguage(grammarText));

        log.info("Found {} actions, {} predicates, {} inline actions, detected language: {}",
                report.getActionCount(), report.getPredicateCount(),
                inlineActionCount, report.getDetectedLanguage());

        return report;
    }

    /**
     * Detect programming language from embedded code
     */
    private String detectLanguage(String grammarText) {
        // C# indicators (check before Java due to similarity)
        if (grammarText.contains("using System") ||
            grammarText.contains("Console.WriteLine")) {
            return "csharp";
        }

        // C++ indicators (check before Java due to similarity)
        if (grammarText.contains("std::") ||
            grammarText.contains("#include") ||
            grammarText.contains("cout <<")) {
            return "cpp";
        }

        // Java indicators
        if (grammarText.contains("import java.") ||
            grammarText.contains("System.out.") ||
            grammarText.contains("public class") ||
            (grammarText.contains("private ") && grammarText.contains("int ")) ||
            grammarText.contains("ArrayList<")) {
            return "java";
        }

        // Python indicators
        if (grammarText.contains("import ") &&
            (grammarText.contains("def ") ||
             grammarText.contains("self.") ||
             grammarText.contains("print("))) {
            return "python";
        }

        // JavaScript/TypeScript indicators
        if (grammarText.contains("console.log") ||
            grammarText.contains("const ") ||
            grammarText.contains("let ") ||
            grammarText.contains("function ") ||
            grammarText.contains("=>")) {
            return "javascript";
        }

        return "unknown";
    }

    /**
     * Count inline actions (excluding predicates)
     */
    private int countInlineActions(String grammarText) {
        Matcher actionMatcher = INLINE_ACTION_PATTERN.matcher(grammarText);
        Matcher predicateMatcher = SEMANTIC_PREDICATE_PATTERN.matcher(grammarText);

        Set<Integer> actionPositions = new HashSet<>();
        Set<Integer> predicatePositions = new HashSet<>();

        while (actionMatcher.find()) {
            actionPositions.add(actionMatcher.start());
        }

        while (predicateMatcher.find()) {
            predicatePositions.add(predicateMatcher.start());
        }

        // Remove predicate positions from action positions
        actionPositions.removeAll(predicatePositions);

        // Also remove @action positions
        Matcher atActionMatcher = ACTION_PATTERN.matcher(grammarText);
        while (atActionMatcher.find()) {
            actionPositions.remove(atActionMatcher.start());
        }

        return actionPositions.size();
    }

    /**
     * Strip all embedded code from grammar
     */
    public String stripEmbeddedCode(String grammarText) {
        log.info("Stripping embedded code from grammar");

        String result = grammarText;

        // Remove @actions
        result = ACTION_PATTERN.matcher(result).replaceAll("");

        // Remove semantic predicates (keep structure with always-true predicate)
        result = SEMANTIC_PREDICATE_PATTERN.matcher(result).replaceAll("");

        // Remove inline actions
        result = INLINE_ACTION_PATTERN.matcher(result).replaceAll("");

        log.info("Stripped embedded code, size reduced from {} to {} bytes",
                grammarText.length(), result.length());

        return result;
    }

    /**
     * Check if grammar has embedded code
     */
    public boolean hasEmbeddedCode(String grammarText) {
        return ACTION_PATTERN.matcher(grammarText).find() ||
               SEMANTIC_PREDICATE_PATTERN.matcher(grammarText).find() ||
               countInlineActions(grammarText) > 0;
    }

    /**
     * Report of embedded code analysis
     */
    @Data
    public static class EmbeddedCodeReport {
        private Map<String, List<CodeFragment>> actions = new HashMap<>();
        private List<CodeFragment> predicates = new ArrayList<>();
        private int inlineActionCount = 0;
        private String detectedLanguage = "unknown";

        public void addAction(String type, String code, int position) {
            actions.computeIfAbsent(type, k -> new ArrayList<>())
                   .add(new CodeFragment(code, position));
        }

        public void addPredicate(String code, int position) {
            predicates.add(new CodeFragment(code, position));
        }

        public int getActionCount() {
            return actions.values().stream()
                         .mapToInt(List::size)
                         .sum();
        }

        public int getPredicateCount() {
            return predicates.size();
        }

        public boolean hasEmbeddedCode() {
            return getActionCount() > 0 || getPredicateCount() > 0 || inlineActionCount > 0;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("hasEmbeddedCode", hasEmbeddedCode());
            map.put("detectedLanguage", detectedLanguage);
            map.put("actionCount", getActionCount());
            map.put("predicateCount", getPredicateCount());
            map.put("inlineActionCount", inlineActionCount);

            if (!actions.isEmpty()) {
                Map<String, List<Map<String, Object>>> actionsMap = new HashMap<>();
                for (Map.Entry<String, List<CodeFragment>> entry : actions.entrySet()) {
                    List<Map<String, Object>> fragments = new ArrayList<>();
                    for (CodeFragment fragment : entry.getValue()) {
                        Map<String, Object> fragMap = new HashMap<>();
                        fragMap.put("code", fragment.getCode());
                        fragMap.put("position", fragment.getPosition());
                        fragments.add(fragMap);
                    }
                    actionsMap.put(entry.getKey(), fragments);
                }
                map.put("actions", actionsMap);
            }

            if (!predicates.isEmpty()) {
                List<Map<String, Object>> predicatesList = new ArrayList<>();
                for (CodeFragment fragment : predicates) {
                    Map<String, Object> fragMap = new HashMap<>();
                    fragMap.put("code", fragment.getCode());
                    fragMap.put("position", fragment.getPosition());
                    predicatesList.add(fragMap);
                }
                map.put("predicates", predicatesList);
            }

            return map;
        }
    }

    /**
     * Code fragment with position
     */
    @Data
    @AllArgsConstructor
    public static class CodeFragment {
        private String code;
        private int position;
    }
}
