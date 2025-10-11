#!/bin/bash

echo "Running ANTLR4 MCP Server v0.2.0..."

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
