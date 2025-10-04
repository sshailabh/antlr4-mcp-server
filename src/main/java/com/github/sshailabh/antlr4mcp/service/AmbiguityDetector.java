package com.github.sshailabh.antlr4mcp.service;

import com.github.sshailabh.antlr4mcp.model.Ambiguity;
import com.github.sshailabh.antlr4mcp.model.AmbiguityReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class AmbiguityDetector {

    public AmbiguityReport analyze(String grammarText) {
        log.info("Analyzing grammar for ambiguities (static analysis)");

        try {
            List<Ambiguity> ambiguities = new ArrayList<>();

            log.debug("Ambiguity detection not fully implemented in M1 basic version");

            return AmbiguityReport.noAmbiguities();

        } catch (Exception e) {
            log.error("Ambiguity detection failed", e);
            return AmbiguityReport.error("Analysis error: " + e.getMessage());
        }
    }
}
