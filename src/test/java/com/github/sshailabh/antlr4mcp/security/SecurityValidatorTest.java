package com.github.sshailabh.antlr4mcp.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Security Validator Tests")
class SecurityValidatorTest {

    private SecurityValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SecurityValidator();
        // Enable validation for tests using reflection
        org.springframework.test.util.ReflectionTestUtils.setField(validator, "validationEnabled", true);
    }

    @Test
    @DisplayName("Should accept valid grammar names")
    void testValidGrammarNames() {
        assertDoesNotThrow(() -> validator.validateGrammarName("Calculator"));
        assertDoesNotThrow(() -> validator.validateGrammarName("JSON_Parser"));
        assertDoesNotThrow(() -> validator.validateGrammarName("SQL2003"));
        assertDoesNotThrow(() -> validator.validateGrammarName("MyGrammar123"));
    }

    @ParameterizedTest
    @DisplayName("Should reject grammar names with command injection attempts")
    @ValueSource(strings = {
        "Grammar; rm -rf /",
        "Test`cat /etc/passwd`",
        "Grammar$(whoami)",
        "Test&&ls",
        "Grammar|grep secret",
        "Test>output.txt",
        "Grammar<input.txt",
        "Test\\necho pwned",
        "Grammar\"drop table\"",
        "Test'or'1'='1",
        "../../../etc/passwd",
        "Grammar;--",
        "Test{echo,pwned}",
        "Grammar[0]",
        "Test()",
        "Grammar$USER"
    })
    void testGrammarNameInjectionAttempts(String maliciousName) {
        assertThrows(SecurityException.class,
            () -> validator.validateGrammarName(maliciousName),
            "Should reject: " + maliciousName);
    }

    @Test
    @DisplayName("Should reject grammar names exceeding length limit")
    void testGrammarNameLengthLimit() {
        String longName = "A".repeat(51);
        assertThrows(SecurityException.class,
            () -> validator.validateGrammarName(longName));
    }

    @Test
    @DisplayName("Should reject null or empty grammar names")
    void testNullEmptyGrammarNames() {
        assertThrows(SecurityException.class, () -> validator.validateGrammarName(null));
        assertThrows(SecurityException.class, () -> validator.validateGrammarName(""));
        assertThrows(SecurityException.class, () -> validator.validateGrammarName("   "));
    }

    @Test
    @DisplayName("Should accept valid rule names")
    void testValidRuleNames() {
        assertDoesNotThrow(() -> validator.validateRuleName("expr"));
        assertDoesNotThrow(() -> validator.validateRuleName("NUMBER"));
        assertDoesNotThrow(() -> validator.validateRuleName("start_rule"));
        assertDoesNotThrow(() -> validator.validateRuleName("prog123"));
    }

    @ParameterizedTest
    @DisplayName("Should reject rule names with injection attempts")
    @ValueSource(strings = {
        "expr; cat /etc/passwd",
        "rule`id`",
        "start$(pwd)",
        "prog&&whoami",
        "rule|ls",
        "expr>file",
        "rule<input",
        "prog;echo test",
        "rule\"test\"",
        "expr'test'",
        "../../rule",
        "prog\\ncommand"
    })
    void testRuleNameInjectionAttempts(String maliciousRule) {
        assertThrows(SecurityException.class,
            () -> validator.validateRuleName(maliciousRule),
            "Should reject: " + maliciousRule);
    }

    @Test
    @DisplayName("Should detect path traversal attempts")
    void testPathTraversalDetection() throws Exception {
        // Create a real temp directory for testing
        Path baseDir = java.nio.file.Files.createTempDirectory("test-antlr-");
        Path testFile = baseDir.resolve("test.g4");
        Path subDir = baseDir.resolve("subdir");
        java.nio.file.Files.createDirectory(subDir);
        Path subFile = subDir.resolve("file.txt");
        java.nio.file.Files.createFile(testFile);
        java.nio.file.Files.createFile(subFile);

        try {
            // Valid paths - should not throw
            assertDoesNotThrow(() ->
                validator.validatePath(testFile, baseDir));
            assertDoesNotThrow(() ->
                validator.validatePath(subFile, baseDir));

            // Path traversal attempts - should throw
            assertThrows(SecurityException.class, () ->
                validator.validatePath(baseDir.resolve("../../../etc/passwd"), baseDir));
            assertThrows(SecurityException.class, () ->
                validator.validatePath(Paths.get("/etc/passwd"), baseDir));
            assertThrows(SecurityException.class, () ->
                validator.validatePath(Paths.get("/tmp/other/file.txt"), baseDir));
        } finally {
            // Cleanup
            java.nio.file.Files.deleteIfExists(subFile);
            java.nio.file.Files.deleteIfExists(testFile);
            java.nio.file.Files.deleteIfExists(subDir);
            java.nio.file.Files.deleteIfExists(baseDir);
        }
    }

    @Test
    @DisplayName("Should validate file extensions")
    void testFileExtensionValidation() {
        // Valid extensions
        assertDoesNotThrow(() -> validator.validateFileExtension("g4"));
        assertDoesNotThrow(() -> validator.validateFileExtension("txt"));
        assertDoesNotThrow(() -> validator.validateFileExtension("json"));
        assertDoesNotThrow(() -> validator.validateFileExtension(null));
        assertDoesNotThrow(() -> validator.validateFileExtension(""));

        // Invalid extensions
        assertThrows(SecurityException.class,
            () -> validator.validateFileExtension("sh;echo"));
        assertThrows(SecurityException.class,
            () -> validator.validateFileExtension("txt|ls"));
        assertThrows(SecurityException.class,
            () -> validator.validateFileExtension("../etc"));
    }

    @ParameterizedTest
    @DisplayName("Should detect shell metacharacters")
    @ValueSource(strings = {
        "test;ls",
        "input&command",
        "file|grep",
        "test`pwd`",
        "data$(whoami)",
        "test>output",
        "input<file",
        "test\"quote",
        "data'quote",
        "test[array]",
        "data{brace}",
        "test(paren)",
        "data\\escape"
    })
    void testShellMetacharacterDetection(String input) {
        assertThrows(SecurityException.class,
            () -> validator.validateNoShellMetacharacters(input),
            "Should detect metacharacters in: " + input);
    }

    @Test
    @DisplayName("Should accept clean input without metacharacters")
    void testCleanInput() {
        assertDoesNotThrow(() -> validator.validateNoShellMetacharacters("cleanInput123"));
        assertDoesNotThrow(() -> validator.validateNoShellMetacharacters("test_file.txt"));
        assertDoesNotThrow(() -> validator.validateNoShellMetacharacters("MyGrammar"));
        assertDoesNotThrow(() -> validator.validateNoShellMetacharacters(null));
    }

    @ParameterizedTest
    @MethodSource("provideEdgeCases")
    @DisplayName("Should handle edge cases correctly")
    void testEdgeCases(String input, String method, boolean shouldThrow) {
        if ("grammar".equals(method)) {
            if (shouldThrow) {
                assertThrows(SecurityException.class, () -> validator.validateGrammarName(input));
            } else {
                assertDoesNotThrow(() -> validator.validateGrammarName(input));
            }
        } else if ("rule".equals(method)) {
            if (shouldThrow) {
                assertThrows(SecurityException.class, () -> validator.validateRuleName(input));
            } else {
                assertDoesNotThrow(() -> validator.validateRuleName(input));
            }
        }
    }

    static Stream<Arguments> provideEdgeCases() {
        return Stream.of(
            // Grammar name edge cases
            Arguments.of("A", "grammar", false),  // Single letter - valid
            Arguments.of("_test", "grammar", true),  // Starting with underscore - invalid
            Arguments.of("1Grammar", "grammar", true),  // Starting with number - invalid
            Arguments.of("Grammar-Name", "grammar", true),  // Contains hyphen - invalid
            Arguments.of("Grammar.Name", "grammar", true),  // Contains dot - invalid
            Arguments.of("A" + "B".repeat(49), "grammar", false),  // Max length - valid
            Arguments.of("A" + "B".repeat(50), "grammar", true),  // Over max length - invalid

            // Rule name edge cases
            Arguments.of("a", "rule", false),  // Single letter - valid
            Arguments.of("Z", "rule", false),  // Capital single letter - valid
            Arguments.of("_rule", "rule", true),  // Starting with underscore - invalid
            Arguments.of("9rule", "rule", true),  // Starting with number - invalid
            Arguments.of("rule-name", "rule", true),  // Contains hyphen - invalid
            Arguments.of("rule.name", "rule", true)  // Contains dot - invalid
        );
    }
}