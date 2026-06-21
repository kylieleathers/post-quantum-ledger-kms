#!/usr/bin/env bash
# Convenience wrapper around `mvn test` for core_kms.
# Run from anywhere: ./test.sh (resolves its own directory below).
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"
mvn clean test
