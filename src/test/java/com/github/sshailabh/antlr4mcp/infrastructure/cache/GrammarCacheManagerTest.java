package com.github.sshailabh.antlr4mcp.infrastructure.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GrammarCacheManagerTest {

    private GrammarCacheManager cacheManager;
    private CacheManager mockCacheManager;
    private CacheKeyGenerator keyGenerator;

    @BeforeEach
    void setUp() {
        mockCacheManager = mock(CacheManager.class);
        keyGenerator = new CacheKeyGenerator();
        cacheManager = new GrammarCacheManager(mockCacheManager, keyGenerator);
    }

    @Test
    void testGetCacheHit() {
        String cacheName = "testCache";
        String key = "testKey";
        String expectedValue = "testValue";

        Cache mockCache = mock(Cache.class);
        when(mockCacheManager.getCache(cacheName)).thenReturn(mockCache);
        when(mockCache.get(key, String.class)).thenReturn(expectedValue);

        Optional<String> result = cacheManager.get(cacheName, key, String.class);

        assertTrue(result.isPresent());
        assertEquals(expectedValue, result.get());
    }

    @Test
    void testGetCacheMiss() {
        String cacheName = "testCache";
        String key = "testKey";

        Cache mockCache = mock(Cache.class);
        when(mockCacheManager.getCache(cacheName)).thenReturn(mockCache);
        when(mockCache.get(key, String.class)).thenReturn(null);

        Optional<String> result = cacheManager.get(cacheName, key, String.class);

        assertFalse(result.isPresent());
    }

    @Test
    void testGetWithNullCacheManager() {
        GrammarCacheManager nullCacheManager = new GrammarCacheManager(null, keyGenerator);
        Optional<String> result = nullCacheManager.get("cache", "key", String.class);

        assertFalse(result.isPresent());
    }

    @Test
    void testPut() {
        String cacheName = "testCache";
        String key = "testKey";
        String value = "testValue";

        Cache mockCache = mock(Cache.class);
        when(mockCacheManager.getCache(cacheName)).thenReturn(mockCache);

        cacheManager.put(cacheName, key, value);

        verify(mockCache).put(key, value);
    }

    @Test
    void testPutWithNullCacheManager() {
        GrammarCacheManager nullCacheManager = new GrammarCacheManager(null, keyGenerator);

        // Should not throw exception
        assertDoesNotThrow(() -> {
            nullCacheManager.put("cache", "key", "value");
        });
    }

    @Test
    void testGetCompiledGrammar() {
        String grammarContent = "grammar Test;";
        Object compiled = new Object();

        Cache mockCache = mock(Cache.class);
        when(mockCacheManager.getCache("compiledGrammars")).thenReturn(mockCache);

        String expectedKey = keyGenerator.generateKey(grammarContent);
        when(mockCache.get(expectedKey, Object.class)).thenReturn(compiled);

        Optional<Object> result = cacheManager.getCompiledGrammar(grammarContent, Object.class);

        assertTrue(result.isPresent());
        assertEquals(compiled, result.get());
    }

    @Test
    void testPutCompiledGrammar() {
        String grammarContent = "grammar Test;";
        Object compiled = new Object();

        Cache mockCache = mock(Cache.class);
        when(mockCacheManager.getCache("compiledGrammars")).thenReturn(mockCache);

        cacheManager.putCompiledGrammar(grammarContent, compiled);

        String expectedKey = keyGenerator.generateKey(grammarContent);
        verify(mockCache).put(expectedKey, compiled);
    }

    @Test
    void testInvalidate() {
        String cacheName = "testCache";
        String key = "testKey";

        Cache mockCache = mock(Cache.class);
        when(mockCacheManager.getCache(cacheName)).thenReturn(mockCache);

        cacheManager.invalidate(cacheName, key);

        verify(mockCache).evict(key);
    }

    @Test
    void testClearAll() {
        Cache mockCache1 = mock(Cache.class);
        Cache mockCache2 = mock(Cache.class);

        when(mockCacheManager.getCacheNames())
            .thenReturn(java.util.Arrays.asList("cache1", "cache2"));
        when(mockCacheManager.getCache("cache1")).thenReturn(mockCache1);
        when(mockCacheManager.getCache("cache2")).thenReturn(mockCache2);

        cacheManager.clearAll();

        verify(mockCache1).clear();
        verify(mockCache2).clear();
    }

    @Test
    void testClear() {
        String cacheName = "testCache";
        Cache mockCache = mock(Cache.class);

        when(mockCacheManager.getCache(cacheName)).thenReturn(mockCache);

        cacheManager.clear(cacheName);

        verify(mockCache).clear();
    }

    @Test
    void testGetCacheStatistics() {
        CaffeineCache caffeineCache = new CaffeineCache("testCache",
            Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .recordStats()
                .build());

        when(mockCacheManager.getCacheNames())
            .thenReturn(java.util.Arrays.asList("testCache"));
        when(mockCacheManager.getCache("testCache")).thenReturn(caffeineCache);

        Map<String, CacheStats> stats = cacheManager.getCacheStatistics();

        assertNotNull(stats);
        assertTrue(stats.containsKey("testCache"));
    }

    @Test
    void testIsEnabled() {
        assertTrue(cacheManager.isEnabled());

        GrammarCacheManager nullCacheManager = new GrammarCacheManager(null, keyGenerator);
        assertFalse(nullCacheManager.isEnabled());
    }
}
