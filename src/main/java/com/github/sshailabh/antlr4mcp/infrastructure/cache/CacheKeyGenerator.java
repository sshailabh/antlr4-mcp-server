package com.github.sshailabh.antlr4mcp.infrastructure.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.stream.Collectors;

/**
 * Generates cache keys for grammar content and operations
 */
@Component
@Slf4j
public class CacheKeyGenerator {

    /**
     * Generate cache key from grammar content using SHA-256 hash
     *
     * @param content Grammar content
     * @return Base64-encoded SHA-256 hash
     */
    public String generateKey(String content) {
        if (content == null) {
            throw new IllegalArgumentException("Content cannot be null");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            String key = Base64.getEncoder().encodeToString(hash);
            log.trace("Generated cache key: {} (length: {})", key.substring(0, 16) + "...", content.length());
            return key;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Generate cache key from multiple inputs
     *
     * @param inputs Variable number of inputs
     * @return Generated cache key
     */
    public String generateKey(Object... inputs) {
        if (inputs == null || inputs.length == 0) {
            throw new IllegalArgumentException("At least one input required");
        }

        String combined = Arrays.stream(inputs)
            .map(obj -> obj == null ? "null" : obj.toString())
            .collect(Collectors.joining(":"));

        return generateKey(combined);
    }

    /**
     * Generate key for grammar + start rule combination
     */
    public String generateParseKey(String grammarContent, String startRule, String input) {
        return generateKey(grammarContent, startRule, input);
    }

    /**
     * Generate key for grammar URI
     */
    public String generateUriKey(String uri) {
        return generateKey(uri);
    }
}
