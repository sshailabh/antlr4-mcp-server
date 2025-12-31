#!/bin/bash

set -euo pipefail

IMAGE="${1:-antlr4-mcp-server:latest}"

echo "Running ANTLR4 MCP Server (${IMAGE})..."

docker run -i --rm \
  --name antlr4-mcp-server \
  --memory="512m" \
  --memory-swap="512m" \
  --cpu-shares="512" \
  --security-opt="no-new-privileges:true" \
  --read-only \
  --tmpfs /tmp/antlr:rw,noexec,nosuid,size=256m \
  "${IMAGE}"

echo "Server stopped."
