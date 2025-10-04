package com.github.sshailabh.antlr4mcp.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AmbiguityReport {
    private boolean hasAmbiguities;

    @Builder.Default
    private List<Ambiguity> ambiguities = new ArrayList<>();

    public AmbiguityReport() {
        this.ambiguities = new ArrayList<>();
    }

    public AmbiguityReport(boolean hasAmbiguities, List<Ambiguity> ambiguities) {
        this.hasAmbiguities = hasAmbiguities;
        this.ambiguities = ambiguities != null ? ambiguities : new ArrayList<>();
    }

    public static AmbiguityReport noAmbiguities() {
        return AmbiguityReport.builder()
            .hasAmbiguities(false)
            .ambiguities(new ArrayList<>())
            .build();
    }

    public static AmbiguityReport withAmbiguities(List<Ambiguity> ambiguities) {
        return AmbiguityReport.builder()
            .hasAmbiguities(!ambiguities.isEmpty())
            .ambiguities(ambiguities)
            .build();
    }

    public static AmbiguityReport error(String message) {
        return AmbiguityReport.builder()
            .hasAmbiguities(false)
            .ambiguities(new ArrayList<>())
            .build();
    }
}
