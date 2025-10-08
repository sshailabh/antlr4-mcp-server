package com.github.sshailabh.antlr4mcp.service;

import com.github.sshailabh.antlr4mcp.model.ParseTimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.concurrent.*;

/**
 * Manages timeout protection for parsing operations.
 *
 * Provides a mechanism to execute parsing operations with a configurable timeout
 * to prevent infinite loops or pathological grammars from hanging the server.
 *
 * Uses a cached thread pool to handle concurrent parsing requests efficiently.
 */
@Slf4j
@Service
public class ParseTimeoutManager {

    private final ExecutorService executor;

    @Value("${antlr.parsing-timeout-seconds:5}")
    private int defaultTimeoutSeconds;

    public ParseTimeoutManager() {
        this.executor = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r);
            thread.setName("parse-timeout-" + thread.getId());
            thread.setDaemon(true);
            return thread;
        });
        log.info("ParseTimeoutManager initialized");
    }

    /**
     * Execute an operation with the default timeout.
     *
     * @param operation The operation to execute
     * @return The result of the operation
     * @throws Exception if the operation fails or times out
     */
    public <T> T executeWithTimeout(Callable<T> operation) throws Exception {
        return executeWithTimeout(operation, defaultTimeoutSeconds);
    }

    /**
     * Execute an operation with a custom timeout.
     *
     * @param operation The operation to execute
     * @param timeoutSeconds Timeout in seconds
     * @return The result of the operation
     * @throws ParseTimeoutException if the operation exceeds the timeout
     * @throws Exception if the operation fails for other reasons
     */
    public <T> T executeWithTimeout(Callable<T> operation, int timeoutSeconds) throws Exception {
        log.debug("Executing operation with timeout of {} seconds", timeoutSeconds);

        Future<T> future = executor.submit(operation);

        try {
            T result = future.get(timeoutSeconds, TimeUnit.SECONDS);
            log.debug("Operation completed successfully within timeout");
            return result;

        } catch (TimeoutException e) {
            future.cancel(true);
            String message = String.format(
                "Parsing exceeded timeout of %d seconds. " +
                "This may indicate a pathological grammar or very large input.",
                timeoutSeconds
            );
            log.warn(message);
            throw new ParseTimeoutException(message, e);

        } catch (ExecutionException e) {
            // Unwrap the actual exception
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            } else {
                throw new RuntimeException("Parsing failed", cause);
            }
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw new ParseTimeoutException("Parsing was interrupted", e);
        }
    }

    /**
     * Shutdown the executor service when the application context is destroyed.
     */
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down ParseTimeoutManager");
        executor.shutdownNow();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("Executor did not terminate in time");
            }
        } catch (InterruptedException e) {
            log.warn("Interrupted while waiting for executor termination");
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Get the default timeout in seconds.
     */
    public int getDefaultTimeoutSeconds() {
        return defaultTimeoutSeconds;
    }
}
