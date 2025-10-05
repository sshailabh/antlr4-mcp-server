package com.github.sshailabh.antlr4mcp.infrastructure.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.sshailabh.antlr4mcp.config.AntlrMcpProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Configuration for Caffeine-based caching
 */
@Configuration
@EnableCaching
@Slf4j
public class CacheConfiguration {

    private final AntlrMcpProperties properties;

    @Autowired
    public CacheConfiguration(AntlrMcpProperties properties) {
        this.properties = properties;
    }

    @Bean
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
            "resolvedGrammars"
        );

        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(properties.getCache().getMaxSize())
            .expireAfterWrite(properties.getCache().getTtlSeconds(), TimeUnit.SECONDS)
            .recordStats());

        log.info("Cache manager initialized with {} caches",
                cacheManager.getCacheNames().size());

        return cacheManager;
    }
}
