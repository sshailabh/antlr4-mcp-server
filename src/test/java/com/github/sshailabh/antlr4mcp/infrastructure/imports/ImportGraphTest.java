package com.github.sshailabh.antlr4mcp.infrastructure.imports;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ImportGraphTest {

    private ImportGraph importGraph;

    @BeforeEach
    void setUp() {
        importGraph = new ImportGraph(10);
    }

    @Test
    void testAddDependency() {
        importGraph.addDependency("GrammarA", "GrammarB");

        Set<String> deps = importGraph.getDependencies("GrammarA");
        assertTrue(deps.contains("GrammarB"));
    }

    @Test
    void testWouldCreateCycleSelfReference() {
        assertTrue(importGraph.wouldCreateCycle("GrammarA", "GrammarA"));
    }

    @Test
    void testWouldCreateCycleSimple() {
        importGraph.addDependency("GrammarA", "GrammarB");
        importGraph.addDependency("GrammarB", "GrammarC");

        assertTrue(importGraph.wouldCreateCycle("GrammarC", "GrammarA"));
    }

    @Test
    void testWouldNotCreateCycle() {
        importGraph.addDependency("GrammarA", "GrammarB");
        importGraph.addDependency("GrammarB", "GrammarC");

        assertFalse(importGraph.wouldCreateCycle("GrammarA", "GrammarD"));
    }

    @Test
    void testGetDepth() {
        importGraph.addDependency("GrammarA", "GrammarB");
        importGraph.addDependency("GrammarB", "GrammarC");
        importGraph.addDependency("GrammarC", "GrammarD");

        assertEquals(3, importGraph.getDepth("GrammarA"));
        assertEquals(2, importGraph.getDepth("GrammarB"));
        assertEquals(1, importGraph.getDepth("GrammarC"));
        assertEquals(0, importGraph.getDepth("GrammarD"));
    }

    @Test
    void testValidateDepthSuccess() {
        importGraph.addDependency("GrammarA", "GrammarB");
        importGraph.addDependency("GrammarB", "GrammarC");

        assertDoesNotThrow(() -> importGraph.validateDepth("GrammarA"));
    }

    @Test
    void testValidateDepthExceedsMax() {
        ImportGraph shallowGraph = new ImportGraph(2);

        shallowGraph.addDependency("A", "B");
        shallowGraph.addDependency("B", "C");
        shallowGraph.addDependency("C", "D");

        assertThrows(IllegalStateException.class, () -> {
            shallowGraph.validateDepth("A");
        });
    }

    @Test
    void testGetTopologicalOrder() {
        importGraph.addDependency("GrammarA", "GrammarB");
        importGraph.addDependency("GrammarB", "GrammarD");

        List<String> order = importGraph.getTopologicalOrder();

        assertNotNull(order);
        System.out.println("Topological order: " + order);
        System.out.println("Index of GrammarD: " + order.indexOf("GrammarD"));
        System.out.println("Index of GrammarB: " + order.indexOf("GrammarB"));
        System.out.println("Index of GrammarA: " + order.indexOf("GrammarA"));

        // Dependencies should be before dependents
        // GrammarD has no dependencies, so it should be first
        // GrammarB depends on GrammarD, so it should come after D
        // GrammarA depends on GrammarB, so it should come after B
        assertTrue(order.indexOf("GrammarD") < order.indexOf("GrammarB"),
            "GrammarD should come before GrammarB");
        assertTrue(order.indexOf("GrammarB") < order.indexOf("GrammarA"),
            "GrammarB should come before GrammarA");
    }

    @Test
    void testGetTopologicalOrderWithCycle() {
        importGraph.addDependency("GrammarA", "GrammarB");
        importGraph.addDependency("GrammarB", "GrammarC");
        importGraph.addDependency("GrammarC", "GrammarA");

        assertThrows(IllegalStateException.class, () -> {
            importGraph.getTopologicalOrder();
        });
    }

    @Test
    void testClear() {
        importGraph.addDependency("GrammarA", "GrammarB");
        importGraph.clear();

        assertTrue(importGraph.getDependencies("GrammarA").isEmpty());
    }

    @Test
    void testGetDependenciesEmpty() {
        Set<String> deps = importGraph.getDependencies("NonExistent");
        assertTrue(deps.isEmpty());
    }

    @Test
    void testMultipleDependencies() {
        importGraph.addDependency("GrammarA", "GrammarB");
        importGraph.addDependency("GrammarA", "GrammarC");
        importGraph.addDependency("GrammarA", "GrammarD");

        Set<String> deps = importGraph.getDependencies("GrammarA");
        assertEquals(3, deps.size());
        assertTrue(deps.contains("GrammarB"));
        assertTrue(deps.contains("GrammarC"));
        assertTrue(deps.contains("GrammarD"));
    }

    @Test
    void testComplexCycleDetection() {
        importGraph.addDependency("A", "B");
        importGraph.addDependency("B", "C");
        importGraph.addDependency("C", "D");
        importGraph.addDependency("D", "E");

        assertTrue(importGraph.wouldCreateCycle("E", "A"));
        assertTrue(importGraph.wouldCreateCycle("E", "B"));
        assertFalse(importGraph.wouldCreateCycle("A", "F"));
    }
}
