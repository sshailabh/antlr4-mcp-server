package com.github.sshailabh.antlr4mcp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Report of detected ambiguities from runtime profiling (Phase 2).
 * Enhanced with coverage information and parse statistics.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AmbiguityReport {
    private boolean hasAmbiguities;

    @Builder.Default
    private List<Ambiguity> ambiguities = new ArrayList<>();

    /**
     * Map of rule name to number of ambiguities detected in that rule (Phase 2)
     */
    @Builder.Default
    private Map<String, Integer> ambiguitiesPerRule = new HashMap<>();

    /**
     * Total number of sample inputs parsed (Phase 2)
     */
    private Integer totalSamplesParsed;

    /**
     * Total time spent parsing all samples in milliseconds (Phase 2)
     */
    private Long totalParseTimeMs;

    /**
     * Coverage information - which rules/alternatives were exercised (Phase 2)
     */
    private CoverageInfo coverage;

    public AmbiguityReport() {
        this.ambiguities = new ArrayList<>();
        this.ambiguitiesPerRule = new HashMap<>();
    }

    public AmbiguityReport(boolean hasAmbiguities, List<Ambiguity> ambiguities,
                           Map<String, Integer> ambiguitiesPerRule,
                           Integer totalSamplesParsed, Long totalParseTimeMs,
                           CoverageInfo coverage) {
        this.hasAmbiguities = hasAmbiguities;
        this.ambiguities = ambiguities != null ? ambiguities : new ArrayList<>();
        this.ambiguitiesPerRule = ambiguitiesPerRule != null ? ambiguitiesPerRule : new HashMap<>();
        this.totalSamplesParsed = totalSamplesParsed;
        this.totalParseTimeMs = totalParseTimeMs;
        this.coverage = coverage;
    }

    public static AmbiguityReport noAmbiguities() {
        return AmbiguityReport.builder()
            .hasAmbiguities(false)
            .ambiguities(new ArrayList<>())
            .ambiguitiesPerRule(new HashMap<>())
            .build();
    }

    public static AmbiguityReport withAmbiguities(List<Ambiguity> ambiguities) {
        boolean hasAmb = ambiguities != null && !ambiguities.isEmpty();
        Map<String, Integer> perRule = new HashMap<>();
        if (ambiguities != null) {
            for (Ambiguity amb : ambiguities) {
                if (amb.getRuleName() != null) {
                    perRule.merge(amb.getRuleName(), 1, Integer::sum);
                }
            }
        }
        return AmbiguityReport.builder()
            .hasAmbiguities(hasAmb)
            .ambiguities(ambiguities != null ? ambiguities : new ArrayList<>())
            .ambiguitiesPerRule(perRule)
            .build();
    }

    public static AmbiguityReport error(String message) {
        return AmbiguityReport.builder()
            .hasAmbiguities(false)
            .ambiguities(new ArrayList<>())
            .ambiguitiesPerRule(new HashMap<>())
            .build();
    }

    /**
     * Add an ambiguity to the report
     */
    public void addAmbiguity(Ambiguity ambiguity) {
        if (ambiguities == null) {
            ambiguities = new ArrayList<>();
        }
        ambiguities.add(ambiguity);
        this.hasAmbiguities = true;

        // Update per-rule count
        if (ambiguitiesPerRule == null) {
            ambiguitiesPerRule = new HashMap<>();
        }
        String ruleName = ambiguity.getRuleName();
        if (ruleName != null) {
            ambiguitiesPerRule.merge(ruleName, 1, Integer::sum);
        }
    }

    /**
     * Check if a specific rule has ambiguities
     */
    public boolean hasAmbiguityInRule(String ruleName) {
        return ambiguities != null && ambiguities.stream()
            .anyMatch(a -> ruleName.equals(a.getRuleName()));
    }

    /**
     * Get number of unique ambiguities
     */
    @JsonIgnore
    public int getAmbiguityCount() {
        return ambiguities != null ? ambiguities.size() : 0;
    }

    /**
     * Get number of rules with ambiguities
     */
    @JsonIgnore
    public int getAffectedRuleCount() {
        return ambiguitiesPerRule != null ? ambiguitiesPerRule.size() : 0;
    }
}
