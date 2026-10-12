#!/bin/sh
# Entrypoint of the testsystrik/grading-node container in deploy/compose.grading-node.yml.
# The node reads its settings only on the first gRPC call, so wrong values are reported here at startup.

case "${HOST_DIRECTORY:-}" in
  /*) ;;
  *) echo "HOST_DIRECTORY must be an absolute path, got '${HOST_DIRECTORY:-}'" >&2; exit 1 ;;
esac

case "$HOST_DIRECTORY" in
  *[[:space:]]*) echo "HOST_DIRECTORY must not contain whitespace, got '$HOST_DIRECTORY'" >&2; exit 1 ;;
esac

require_number() {
  case "$2" in
    ''|*[!0-9]*) echo "$1 must be a non-negative integer, got '$2'" >&2; exit 1 ;;
  esac
}

require_number WORKERS_COUNT "${WORKERS_COUNT:-}"
require_number INNER_TIMEOUT_SECONDS "${INNER_TIMEOUT_SECONDS:-}"
require_number NODE_ID "${NODE_ID:-}"

exec dotnet TestSys.Trik.GradingNode.Runner.dll
