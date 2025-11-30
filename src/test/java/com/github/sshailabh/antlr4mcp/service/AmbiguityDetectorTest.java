package com.github.sshailabh.antlr4mcp.service;

import com.github.sshailabh.antlr4mcp.model.AmbiguityReport;
import com.github.sshailabh.antlr4mcp.support.BaseServiceTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AmbiguityDetectorTest extends BaseServiceTest {

    @Autowired
    private AmbiguityDetector ambiguityDetector;

    @Test
    void testAnalyzeSimpleGrammar_NoAmbiguities() {
        AmbiguityReport report = ambiguityDetector.analyze(SIMPLE_GRAMMAR);

        assertThat(report.isHasAmbiguities()).isFalse();
        assertThat(report.getAmbiguities()).isEmpty();
    }

    @Test
    void testAnalyzeWithSamples_DetectsAmbiguity() {
        AmbiguityReport report = ambiguityDetector.analyzeWithSamples(
            AMBIGUOUS_GRAMMAR,
            List.of("1+2+3")
        );

        assertThat(report).isNotNull();
    }

    @Test
    void testAnalyzeInvalidGrammar() {
        AmbiguityReport report = ambiguityDetector.analyze(INVALID_GRAMMAR);

        assertThat(report).isNotNull();
    }
}
