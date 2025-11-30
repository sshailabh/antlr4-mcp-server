#!/bin/bash
set -e

echo "Building ANTLR4 MCP Server v0.2.0 Docker image..."

cd "$(dirname "$0")/.."

echo "Step 1: Building Maven artifact..."
./mvnw clean package -DskipTests

echo "Step 2: Building Docker image..."
docker build -t antlr4-mcp-server:0.2.0 .
docker tag antlr4-mcp-server:0.2.0 antlr4-mcp-server:latest

echo "Build complete!"
echo "Image: antlr4-mcp-server:0.2.0"
echo "Image: antlr4-mcp-server:latest"
