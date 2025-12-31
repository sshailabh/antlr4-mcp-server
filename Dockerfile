# syntax=docker/dockerfile:1.6
#
# Multi-architecture Dockerfile for ANTLR4 MCP Server
# Supports: linux/amd64, linux/arm64
# MCP server runs over stdio (JSON-RPC). Avoid logging to stdout in the app config.

# Build stage - Eclipse Temurin JDK 17 with Maven (supports amd64 + arm64)
FROM eclipse-temurin:17-jdk AS build

WORKDIR /workspace

# Install Maven manually (eclipse-temurin doesn't include it)
RUN apt-get update && apt-get install -y --no-install-recommends maven \
    && rm -rf /var/lib/apt/lists/*

# Leverage Docker layer caching for dependencies
COPY pom.xml /workspace/pom.xml
RUN mvn -q -DskipTests dependency:go-offline

COPY src /workspace/src
RUN mvn -q -DskipTests package && \
    JAR="$(ls -1 target/antlr4-mcp-server-*.jar | grep -v -- '-sources.jar' | head -n 1)" && \
    cp "$JAR" /workspace/app.jar

# Create custom minimal JRE using jlink
# Include modules needed for Spring Boot + ANTLR
RUN $JAVA_HOME/bin/jlink \
    --add-modules java.base,java.logging,java.naming,java.desktop,java.management,java.security.jgss,java.instrument,java.compiler,java.sql,jdk.unsupported \
    --strip-debug \
    --no-man-pages \
    --no-header-files \
    --compress=2 \
    --output /javaruntime

# Runtime stage - Debian Slim (glibc-based, supports amd64 + arm64)
FROM debian:bookworm-slim AS runtime

LABEL maintainer="ANTLR4 MCP Server"
LABEL description="Model Context Protocol server for ANTLR4 grammar validation and analysis"
LABEL org.opencontainers.image.source="https://github.com/sshailabh/antlr4-mcp-server"

# Install minimal dependencies (fontconfig for java.desktop module)
# Security: upgrade all packages to latest versions
RUN apt-get update && apt-get upgrade -y && apt-get install -y --no-install-recommends \
    fontconfig libfreetype6 \
    && rm -rf /var/lib/apt/lists/* \
    && apt-get clean

# Copy custom JRE (architecture-specific, built in previous stage)
ENV JAVA_HOME=/opt/java
COPY --from=build /javaruntime $JAVA_HOME
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# Create non-root user for security (must be done before removing passwd tools)
RUN groupadd -g 1000 antlr && \
    useradd -u 1000 -g antlr -d /home/antlr -s /sbin/nologin -m antlr

WORKDIR /app

# Writable temp directory (ANTLR/JVM) - can be mounted as tmpfs when using --read-only
RUN mkdir -p /tmp/antlr && \
    chown antlr:antlr /tmp/antlr && \
    chmod 700 /tmp/antlr

COPY --from=build --chown=antlr:antlr /workspace/app.jar /app/app.jar

# Make app.jar read-only
RUN chmod 444 /app/app.jar

# Security hardening: Remove package management tools to reduce attack surface
# This mitigates vulnerabilities in gpgv, apt, dpkg that are not needed at runtime
RUN rm -rf /var/lib/dpkg /var/lib/apt /var/cache/apt /var/log/apt \
    && rm -f /usr/bin/apt* /usr/bin/dpkg* /usr/bin/gpg* /usr/bin/gpgv* \
    && rm -rf /etc/apt /etc/dpkg \
    && rm -rf /usr/share/doc /usr/share/man /usr/share/info \
    && find /var/log -type f -delete

# JVM tuning for container environments
# Security: disable attach API, restrict reflection
ENV JAVA_OPTS="-Xms128m -Xmx512m -XX:MaxMetaspaceSize=128m -XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:+UseContainerSupport -Djava.security.egd=file:/dev/urandom -XX:+DisableAttachMechanism"

USER antlr

# Health check (optional, for orchestration)
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD echo '{}' | timeout 2 java -jar /app/app.jar 2>/dev/null || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Djava.io.tmpdir=/tmp/antlr -jar /app/app.jar"]
