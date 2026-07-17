#!/usr/bin/env bash
# =============================================================================
# TCC — FAILURE INJECTION TEST
#
# Runs the k6 'failure' scenario (constant probes on all 5 business domains,
# 6 minutes) while injecting a partial failure:
#   t=0min  test starts, all domains healthy
#   t=2min  target container is stopped  (failure window opens)
#   t=4min  target container is started  (failure window closes)
#   t=6min  test ends
#
# Usage:
#   ./run-failure-test.sh micro   # stops comment-ms in the microservices stack
#   ./run-failure-test.sh mono    # stops the whole monolith application
#
# The comparison for the thesis: in the microservices run only
# availability_comment (and any domain that depends on it) drops during the
# window; in the monolith run every availability_* rate drops to zero, since
# all domains live in the same process.
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
K6_SCRIPT="$SCRIPT_DIR/k6-load-test.js"
RESULTS_DIR="$SCRIPT_DIR/test-results"

ARCH="${1:-}"
case "$ARCH" in
  micro)
    BASE_URL="${BASE_URL:-http://localhost:18765}"
    TARGET_CONTAINER="${TARGET_CONTAINER:-mstcc_comment_service}"
    ;;
  mono)
    BASE_URL="${BASE_URL:-http://localhost:8080}"
    TARGET_CONTAINER="${TARGET_CONTAINER:-mono_app}"
    ;;
  *)
    echo "Usage: $0 <micro|mono>"
    echo "  micro -> BASE_URL=http://localhost:18765, stops mstcc_comment_service"
    echo "  mono  -> BASE_URL=http://localhost:8080,  stops mono_app"
    exit 1
    ;;
esac

STOP_AFTER_SECONDS=120
OUTAGE_SECONDS=120

command -v k6 >/dev/null || { echo "ERROR: k6 not found in PATH"; exit 1; }
docker inspect "$TARGET_CONTAINER" >/dev/null 2>&1 || {
  echo "ERROR: container '$TARGET_CONTAINER' not found. Is the $ARCH stack up?"
  exit 1
}
curl -fs "$BASE_URL/actuator/health" >/dev/null 2>&1 || {
  echo "ERROR: $BASE_URL is not answering. Start the $ARCH stack first."
  exit 1
}

mkdir -p "$RESULTS_DIR"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
SUMMARY_FILE="$RESULTS_DIR/failure-$ARCH-$TIMESTAMP.json"
LOG_FILE="$RESULTS_DIR/failure-$ARCH-$TIMESTAMP.log"

echo "=== Failure injection test ($ARCH) ==="
echo "Base URL:         $BASE_URL"
echo "Target container: $TARGET_CONTAINER"
echo "Timeline:         stop at ${STOP_AFTER_SECONDS}s, restart at $((STOP_AFTER_SECONDS + OUTAGE_SECONDS))s"
echo "Summary:          $SUMMARY_FILE"
echo ""

SCENARIO=failure BASE_URL="$BASE_URL" \
  k6 run --summary-export "$SUMMARY_FILE" "$K6_SCRIPT" | tee "$LOG_FILE" &
K6_PID=$!

# Ensure the container comes back even if the test is interrupted.
cleanup() {
  docker start "$TARGET_CONTAINER" >/dev/null 2>&1 || true
}
trap cleanup EXIT INT TERM

sleep "$STOP_AFTER_SECONDS"
echo ""
echo ">>> $(date '+%H:%M:%S') Stopping $TARGET_CONTAINER (failure window opens)"
docker stop "$TARGET_CONTAINER" >/dev/null

sleep "$OUTAGE_SECONDS"
echo ">>> $(date '+%H:%M:%S') Starting $TARGET_CONTAINER (failure window closes)"
docker start "$TARGET_CONTAINER" >/dev/null

wait "$K6_PID"
echo ""
echo "Done. Aggregate availability per domain is in $SUMMARY_FILE"
echo "(look for availability_user/post/comment/like/friendship rates)."
echo "For the time-series view of the outage window, use the Grafana dashboards."
