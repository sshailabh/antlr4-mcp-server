#!/bin/bash
set -euo pipefail

VERSION="${1:-0.2.0}"
IMAGE_NAME="antlr4-mcp-server"
IMAGE="${IMAGE_NAME}:${VERSION}"

# Parse options
PUSH=false
MULTI_ARCH=false
PLATFORMS="linux/amd64,linux/arm64"

while [[ $# -gt 0 ]]; do
    case $1 in
        --push)
            PUSH=true
            shift
            ;;
        --multi-arch)
            MULTI_ARCH=true
            shift
            ;;
        --platforms)
            PLATFORMS="$2"
            shift 2
            ;;
        *)
            # First positional arg is version (already captured)
            shift
            ;;
    esac
done

cd "$(dirname "$0")/.."

echo "=============================================="
echo "Building ANTLR4 MCP Server Docker Image"
echo "=============================================="
echo "Version:    ${VERSION}"
echo "Image:      ${IMAGE}"
echo "Multi-arch: ${MULTI_ARCH}"
if [[ "$MULTI_ARCH" == "true" ]]; then
    echo "Platforms:  ${PLATFORMS}"
fi
echo "Push:       ${PUSH}"
echo "=============================================="

if [[ "$MULTI_ARCH" == "true" ]]; then
    # Multi-architecture build using buildx
    echo ""
    echo "Step 1: Setting up Docker buildx..."
    
    # Create builder if it doesn't exist
    if ! docker buildx inspect multiarch-builder &>/dev/null; then
        docker buildx create --name multiarch-builder --driver docker-container --use
    else
        docker buildx use multiarch-builder
    fi
    
    echo ""
    echo "Step 2: Building multi-architecture image..."
    
    BUILD_ARGS="--platform ${PLATFORMS} -t ${IMAGE} -t ${IMAGE_NAME}:latest"
    
    if [[ "$PUSH" == "true" ]]; then
        # Push to registry (required for multi-arch manifests)
        docker buildx build ${BUILD_ARGS} --push .
        echo ""
        echo "✓ Multi-arch image pushed: ${IMAGE}"
    else
        # Load locally (only works for single platform)
        echo "Note: Multi-arch images can only be loaded locally for the current platform."
        echo "Use --push to push to a registry for full multi-arch support."
        docker buildx build ${BUILD_ARGS} --load .
    fi
else
    # Single-architecture build (current platform)
    echo ""
    echo "Step 1: Building Docker image for current platform..."
    docker build -t "${IMAGE}" .
    docker tag "${IMAGE}" "${IMAGE_NAME}:latest"
    
    if [[ "$PUSH" == "true" ]]; then
        echo ""
        echo "Step 2: Pushing image..."
        docker push "${IMAGE}"
        docker push "${IMAGE_NAME}:latest"
    fi
fi

echo ""
echo "=============================================="
echo "Build Complete!"
echo "=============================================="
echo "Local image:  ${IMAGE}"
echo "Latest tag:   ${IMAGE_NAME}:latest"
echo ""
echo "Quick test:"
echo "  python3 dsl-starter/scripts/mcp_calculator_demo.py --server docker"
echo ""
echo "Multi-arch build (for registry push):"
echo "  ./docker/build.sh ${VERSION} --multi-arch --push"
echo "=============================================="
