#!/usr/bin/env bash
# Build and verify the sandbox runner image.
# Usage: ./sandbox-image/build.sh [--no-verify]
set -euo pipefail

IMAGE="java-ai-academy/runner:21"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VERIFY=true

for arg in "$@"; do
  [[ "$arg" == "--no-verify" ]] && VERIFY=false
done

echo "==> Building $IMAGE"
docker build --pull=false -t "$IMAGE" "$SCRIPT_DIR"

if [[ "$VERIFY" == true ]]; then
  echo "==> Verifying: java -version with all §6.2 hardening flags"
  docker run --rm \
    --network=none \
    --memory=128m \
    --memory-swap=128m \
    --cpus=0.5 \
    --pids-limit=64 \
    --read-only \
    --tmpfs /work:size=32m,noexec \
    --cap-drop=ALL \
    --security-opt=no-new-privileges \
    --user=1000:0 \
    "$IMAGE" java -version

  echo "==> Verifying: JUnit Console Standalone jar is present and loadable"
  docker run --rm \
    --network=none \
    --memory=128m \
    --memory-swap=128m \
    --cpus=0.5 \
    --pids-limit=64 \
    --read-only \
    --tmpfs /work:size=32m,noexec \
    --cap-drop=ALL \
    --security-opt=no-new-privileges \
    --user=1000:0 \
    "$IMAGE" sh -c 'test -f /opt/junit-platform-console-standalone.jar && echo "JUnit Console Standalone jar present ✓"'

  echo "==> Checking for leaked containers"
  LEAKED=$(docker ps -a --filter "ancestor=$IMAGE" --format "{{.ID}}" 2>/dev/null || true)
  if [[ -n "$LEAKED" ]]; then
    echo "ERROR: leaked containers detected: $LEAKED" >&2
    exit 1
  fi
  echo "    no leaked containers ✓"
fi

echo "==> Done: $IMAGE"
