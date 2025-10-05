package com.github.sshailabh.antlr4mcp.infrastructure.imports;

import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Tracks grammar import dependencies and detects circular references
 */
@Slf4j
public class ImportGraph {

    private final Map<String, Set<String>> dependencies = new HashMap<>();
    private final int maxDepth;

    public ImportGraph(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    /**
     * Add an import dependency
     *
     * @param from Grammar that imports
     * @param to   Grammar being imported
     */
    public void addDependency(String from, String to) {
        dependencies.computeIfAbsent(from, k -> new HashSet<>()).add(to);
        log.debug("Added dependency: {} -> {}", from, to);
    }

    /**
     * Check if adding this dependency would create a cycle
     *
     * @param from Grammar that would import
     * @param to   Grammar to be imported
     * @return true if cycle would be created
     */
    public boolean wouldCreateCycle(String from, String to) {
        if (from.equals(to)) {
            return true;
        }

        Set<String> visited = new HashSet<>();
        return hasCycle(to, from, visited, 0);
    }

    /**
     * Get all dependencies for a grammar
     */
    public Set<String> getDependencies(String grammar) {
        return dependencies.getOrDefault(grammar, Collections.emptySet());
    }

    /**
     * Get dependency depth for a grammar
     */
    public int getDepth(String grammar) {
        Set<String> visited = new HashSet<>();
        return calculateDepth(grammar, visited, 0);
    }

    /**
     * Validate that adding a dependency doesn't exceed max depth
     */
    public void validateDepth(String grammar) {
        int depth = getDepth(grammar);
        if (depth > maxDepth) {
            throw new IllegalStateException(
                String.format("Import depth %d exceeds maximum allowed depth %d for grammar %s",
                    depth, maxDepth, grammar)
            );
        }
    }

    /**
     * Get topological order of grammars (for correct compilation order)
     */
    public List<String> getTopologicalOrder() {
        List<String> result = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> tempMarked = new HashSet<>();

        for (String grammar : dependencies.keySet()) {
            if (!visited.contains(grammar)) {
                visit(grammar, visited, tempMarked, result);
            }
        }

        // DFS post-order gives us dependencies before dependents
        return result;
    }

    private void visit(String grammar, Set<String> visited, Set<String> tempMarked, List<String> result) {
        if (tempMarked.contains(grammar)) {
            throw new IllegalStateException("Circular dependency detected involving: " + grammar);
        }
        if (visited.contains(grammar)) {
            return;
        }

        tempMarked.add(grammar);

        Set<String> deps = dependencies.getOrDefault(grammar, Collections.emptySet());
        for (String dep : deps) {
            visit(dep, visited, tempMarked, result);
        }

        tempMarked.remove(grammar);
        visited.add(grammar);
        result.add(grammar);
    }

    private boolean hasCycle(String current, String target, Set<String> visited, int depth) {
        if (depth > maxDepth) {
            return true; // Treat excessive depth as a cycle
        }

        if (current.equals(target)) {
            return true;
        }

        if (visited.contains(current)) {
            return false;
        }

        visited.add(current);

        Set<String> deps = dependencies.getOrDefault(current, Collections.emptySet());
        for (String dep : deps) {
            if (hasCycle(dep, target, visited, depth + 1)) {
                return true;
            }
        }

        return false;
    }

    private int calculateDepth(String grammar, Set<String> visited, int currentDepth) {
        if (visited.contains(grammar)) {
            return currentDepth;
        }

        visited.add(grammar);

        Set<String> deps = dependencies.getOrDefault(grammar, Collections.emptySet());
        if (deps.isEmpty()) {
            return currentDepth;
        }

        int maxChildDepth = currentDepth;
        for (String dep : deps) {
            int childDepth = calculateDepth(dep, visited, currentDepth + 1);
            maxChildDepth = Math.max(maxChildDepth, childDepth);
        }

        return maxChildDepth;
    }

    /**
     * Clear all dependencies
     */
    public void clear() {
        dependencies.clear();
    }
}
