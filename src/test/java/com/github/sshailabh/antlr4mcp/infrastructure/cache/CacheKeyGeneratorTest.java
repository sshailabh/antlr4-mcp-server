package com.github.sshailabh.antlr4mcp.infrastructure.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CacheKeyGeneratorTest {

    private CacheKeyGenerator keyGenerator;

    @BeforeEach
    void setUp() {
        keyGenerator = new CacheKeyGenerator();
    }

    @Test
    void testGenerateKeyFromString() {
        String content = "grammar Test;";
        String key = keyGenerator.generateKey(content);

        assertNotNull(key);
        assertFalse(key.isEmpty());
        assertTrue(key.length() > 0);
    }

    @Test
    void testSameContentGeneratesSameKey() {
        String content = "grammar Test; rule: 'test';";
        String key1 = keyGenerator.generateKey(content);
        String key2 = keyGenerator.generateKey(content);

        assertEquals(key1, key2);
    }

    @Test
    void testDifferentContentGeneratesDifferentKeys() {
        String content1 = "grammar Test1;";
        String content2 = "grammar Test2;";

        String key1 = keyGenerator.generateKey(content1);
        String key2 = keyGenerator.generateKey(content2);

        assertNotEquals(key1, key2);
    }

    @Test
    void testGenerateKeyWithNullContent() {
        assertThrows(IllegalArgumentException.class, () -> {
            keyGenerator.generateKey((String) null);
        });
    }

    @Test
    void testGenerateKeyFromMultipleInputs() {
        String key = keyGenerator.generateKey("grammar", "Test", "rule");

        assertNotNull(key);
        assertFalse(key.isEmpty());
    }

    @Test
    void testGenerateKeyWithNoInputs() {
        assertThrows(IllegalArgumentException.class, () -> {
            keyGenerator.generateKey();
        });
    }

    @Test
    void testGenerateParseKey() {
        String grammar = "grammar Test;";
        String startRule = "expr";
        String input = "test input";

        String key = keyGenerator.generateParseKey(grammar, startRule, input);

        assertNotNull(key);
        assertFalse(key.isEmpty());
    }

    @Test
    void testGenerateUriKey() {
        String uri = "file:///path/to/grammar.g4";
        String key = keyGenerator.generateUriKey(uri);

        assertNotNull(key);
        assertFalse(key.isEmpty());
    }

    @Test
    void testSameParseKeyForSameInputs() {
        String grammar = "grammar Test;";
        String startRule = "expr";
        String input = "test";

        String key1 = keyGenerator.generateParseKey(grammar, startRule, input);
        String key2 = keyGenerator.generateParseKey(grammar, startRule, input);

        assertEquals(key1, key2);
    }

    @Test
    void testDifferentParseKeyForDifferentInputs() {
        String grammar = "grammar Test;";

        String key1 = keyGenerator.generateParseKey(grammar, "rule1", "input1");
        String key2 = keyGenerator.generateParseKey(grammar, "rule2", "input2");

        assertNotEquals(key1, key2);
    }
}
