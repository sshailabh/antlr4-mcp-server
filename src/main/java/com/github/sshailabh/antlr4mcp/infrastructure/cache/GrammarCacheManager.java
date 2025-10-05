package com.github.sshailabh.antlr4mcp.infrastructure.cache;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Manages caching of compiled grammars and related data
 */
@Service
@Slf4j
public class GrammarCacheManager {

    private final CacheManager cacheManager;
    private final CacheKeyGenerator keyGenerator;

    @Autowired
    public GrammarCacheManager(CacheManager cacheManager, CacheKeyGenerator keyGenerator) {
        this.cacheManager = cacheManager;
        this.keyGenerator = keyGenerator;
    }

    /**
     * Get compiled grammar from cache
     */
    public <T> Optional<T> get(String cacheName, String key, Class<T> type) {
        if (cacheManager == null) {
            return Optional.empty();
        }

        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            T cached = cache.get(key, type);
            if (cached != null) {
                log.debug("Cache hit for {}: {}", cacheName, key.substring(0, Math.min(16, key.length())));
                return Optional.of(cached);
            }
        }

        log.debug("Cache miss for {}: {}", cacheName, key.substring(0, Math.min(16, key.length())));
        return Optional.empty();
    }

    /**
     * Put value in cache
     */
    public void put(String cacheName, String key, Object value) {
        if (cacheManager == null) {
            return;
        }

        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.put(key, value);
            log.debug("Cached in {}: {}", cacheName, key.substring(0, Math.min(16, key.length())));
        }
    }

    /**
     * Get compiled grammar from cache by content
     */
    public <T> Optional<T> getCompiledGrammar(String grammarContent, Class<T> type) {
        String key = keyGenerator.generateKey(grammarContent);
        return get("compiledGrammars", key, type);
    }

    /**
     * Store compiled grammar in cache
     */
    public void putCompiledGrammar(String grammarContent, Object compiled) {
        String key = keyGenerator.generateKey(grammarContent);
        put("compiledGrammars", key, compiled);
    }

    /**
     * Invalidate cache entry
     */
    public void invalidate(String cacheName, String key) {
        if (cacheManager == null) {
            return;
        }

        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
            log.info("Invalidated cache entry in {}: {}", cacheName, key);
        }
    }

    /**
     * Clear all caches
     */
    public void clearAll() {
        if (cacheManager == null) {
            return;
        }

        cacheManager.getCacheNames().forEach(name -> {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        });
        log.info("Cleared all caches");
    }

    /**
     * Clear specific cache
     */
    public void clear(String cacheName) {
        if (cacheManager == null) {
            return;
        }

        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            log.info("Cleared cache: {}", cacheName);
        }
    }

    /**
     * Get cache statistics
     */
    public Map<String, CacheStats> getCacheStatistics() {
        Map<String, CacheStats> stats = new HashMap<>();

        if (cacheManager == null) {
            return stats;
        }

        for (String cacheName : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache instanceof CaffeineCache) {
                com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache =
                    ((CaffeineCache) cache).getNativeCache();

                CacheStats cacheStats = nativeCache.stats();
                stats.put(cacheName, cacheStats);
            }
        }

        return stats;
    }

    /**
     * Check if caching is enabled
     */
    public boolean isEnabled() {
        return cacheManager != null;
    }
}
