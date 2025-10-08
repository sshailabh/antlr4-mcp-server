package com.github.sshailabh.antlr4mcp.infrastructure.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.sshailabh.antlr4mcp.config.AntlrMcpProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for Caffeine-based caching with custom key generation.
 *
 * Key improvements:
 * - Uses SHA-256 content hashing instead of String.hashCode()
 * - Deterministic cache keys across JVM restarts
 * - Custom key generator for grammar-specific caching
 */
@Configuration
@EnableCaching
@Slf4j
public class CacheConfiguration implements CachingConfigurer {

    private final AntlrMcpProperties properties;
    private final GrammarCacheKeyGenerator grammarCacheKeyGenerator;

    @Autowired
    public CacheConfiguration(AntlrMcpProperties properties, GrammarCacheKeyGenerator grammarCacheKeyGenerator) {
        this.properties = properties;
        this.grammarCacheKeyGenerator = grammarCacheKeyGenerator;
    }

    @Bean
    @Override
    public CacheManager cacheManager() {
        if (!properties.getCache().isEnabled()) {
            log.info("Caching is disabled");
            return null;
        }

        log.info("Configuring Caffeine cache with maxSize={}, ttl={}s",
                properties.getCache().getMaxSize(),
                properties.getCache().getTtlSeconds());

        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
            "grammars",
            "compiledGrammars",
            "parseResults",
            "analysisResults",
            "resolvedGrammars",
            "grammarInterpreterCache"
        );

        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(properties.getCache().getMaxSize())
            .expireAfterWrite(properties.getCache().getTtlSeconds(), TimeUnit.SECONDS)
            .recordStats());

        log.info("Cache manager initialized with {} caches",
                cacheManager.getCacheNames().size());

        return cacheManager;
    }

    /**
     * Custom key generator for grammar caching.
     * Uses SHA-256 content hashing for deterministic, collision-free keys.
     */
    @Bean("grammarKeyGenerator")
    @Override
    public KeyGenerator keyGenerator() {
        return new KeyGenerator() {
            @Override
            public Object generate(Object target, Method method, Object... params) {
                if (params.length == 0) {
                    return "empty";
                }

                // For grammar content (first parameter is typically String grammarContent)
                if (params[0] instanceof String) {
                    String grammarContent = (String) params[0];

                    // If second parameter exists (e.g., startRule, targetLanguage)
                    if (params.length > 1 && params[1] != null) {
                        return grammarCacheKeyGenerator.generateKey(
                            grammarContent,
                            params[1].toString(),
                            null
                        );
                    }

                    // Just grammar content
                    return grammarCacheKeyGenerator.generateKey(grammarContent);
                }

                // Fallback to default key generation
                StringBuilder key = new StringBuilder();
                for (Object param : params) {
                    if (param != null) {
                        key.append(param.toString()).append(":");
                    }
                }
                return key.toString();
            }
        };
    }

    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        return new SimpleCacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                log.error("Cache get error in cache '{}' for key '{}'", cache.getName(), key, exception);
                super.handleCacheGetError(exception, cache, key);
            }

            @Override
            public void handleCachePutError(RuntimeException exception, org.springframework.cache.Cache cache, Object key, Object value) {
                log.error("Cache put error in cache '{}' for key '{}'", cache.getName(), key, exception);
                super.handleCachePutError(exception, cache, key, value);
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                log.error("Cache evict error in cache '{}' for key '{}'", cache.getName(), key, exception);
                super.handleCacheEvictError(exception, cache, key);
            }
        };
    }
}
