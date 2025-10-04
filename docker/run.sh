#!/bin/bash

echo "Running ANTLR4 MCP Server..."

# Run with security constraints
# - No port exposure (removed -p 8080:8080)
# - Memory limits
# - Read-only root filesystem
# - No new privileges
# - Temp directory mounted
docker run -it --rm \
  --name antlr4-mcp-server \
  --memory="512m" \
  --memory-swap="512m" \
  --cpu-shares="512" \
  --security-opt="no-new-privileges:true" \
  --read-only \
  --tmpfs /tmp/antlr:rw,noexec,nosuid,size=100m \
  antlr4-mcp-server:latest

echo "Server stopped."
