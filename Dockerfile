FROM amazoncorretto:11-alpine

LABEL maintainer="ANTLR4 MCP Server"
LABEL description="Model Context Protocol server for ANTLR4 grammar validation and analysis"

# Create non-root user for security
RUN addgroup -g 1000 antlr && \
    adduser -D -u 1000 -G antlr -h /home/antlr -s /bin/sh antlr

WORKDIR /app

# Install necessary packages
RUN apk add --no-cache bash

# Copy application files with proper ownership
COPY --chown=antlr:antlr target/antlr4-mcp-server-0.1.0-M1.jar /app/app.jar
COPY --chown=antlr:antlr src/main/resources/application.yml /app/application.yml

# Create temp directory with restricted permissions
RUN mkdir -p /tmp/antlr && \
    chown antlr:antlr /tmp/antlr && \
    chmod 700 /tmp/antlr

# Set resource limits for JVM
ENV JAVA_OPTS="-Xms128m -Xmx256m -XX:MaxMetaspaceSize=128m -XX:+UseG1GC -XX:MaxGCPauseMillis=100"

# Security: Run as non-root user
USER antlr

# Remove port exposure - MCP uses stdio, not network
# EXPOSE 8080 - REMOVED

# Add temp directory configuration
# Note: Java Security Manager is disabled by default (deprecated in Java 17+)
# Security is now controlled via application.yml (antlr.security.*)
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.io.tmpdir=/tmp/antlr -jar /app/app.jar"]
