FROM amazoncorretto:17-alpine

LABEL maintainer="ANTLR4 MCP Server"
LABEL description="Model Context Protocol server v0.2.0 for ANTLR4 grammar validation and analysis"
LABEL version="0.2.0"

# Create non-root user for security
RUN addgroup -g 1000 antlr && \
    adduser -D -u 1000 -G antlr -h /home/antlr -s /bin/sh antlr

WORKDIR /app

# Install necessary packages
RUN apk add --no-cache bash

# Copy application files with proper ownership
COPY --chown=antlr:antlr target/antlr4-mcp-server-0.2.0.jar /app/app.jar
COPY --chown=antlr:antlr src/main/resources/application.yml /app/application.yml

# Create temp directory with restricted permissions
RUN mkdir -p /tmp/antlr && \
    chown antlr:antlr /tmp/antlr && \
    chmod 700 /tmp/antlr

ENV JAVA_OPTS="-Xms128m -Xmx512m -XX:MaxMetaspaceSize=128m -XX:+UseG1GC -XX:MaxGCPauseMillis=100"

USER antlr

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.io.tmpdir=/tmp/antlr -jar /app/app.jar"]
