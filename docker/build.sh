#!/bin/bash
set -e

echo "Building ANTLR4 MCP Server Docker image..."

cd "$(dirname "$0")/.."

echo "Step 1: Building Maven artifact..."
mvn clean package -DskipTests

echo "Step 2: Building Docker image..."
docker build -t antlr4-mcp-server:0.1.0-M1 .
docker tag antlr4-mcp-server:0.1.0-M1 antlr4-mcp-server:latest

echo "Build complete!"
echo "Image: antlr4-mcp-server:0.1.0-M1"
echo "Image: antlr4-mcp-server:latest"
