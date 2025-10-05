package com.github.sshailabh.antlr4mcp.visualization;

import lombok.Builder;
import lombok.Data;

/**
 * Represents a state node in the ATN
 */
@Data
@Builder
public class AtnNode {
    private int stateNumber;
    private int stateType;
    private int ruleIndex;
    private boolean isAcceptState;
    private String label;
}
