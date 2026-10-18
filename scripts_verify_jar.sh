#!/usr/bin/env bash
set -euo pipefail

# scripts_verify_jar.sh
# Verifies that a built JAR exists and optionally prints a short listing of its contents.
# Usage:
#   ./scripts_verify_jar.sh /path/to/jar
#   ./scripts_verify_jar.sh           # will look for target/AstraxPetRPG-*.jar

JAR_ARG="${1:-}"

if [ -n "$JAR_ARG" ] && [ -f "$JAR_ARG" ]; then
  echo "Using JAR from arg: $JAR_ARG"
  # optionally print a quick size and first lines of jar listing
  ls -lh "$JAR_ARG" || true
  if command -v jar >/dev/null 2>&1; then
    echo "Listing jar contents (first 20 entries):"
    jar tf "$JAR_ARG" | head -n 20 || true
  fi
  exit 0
fi

# Try to locate the JAR in the expected target/ directory
FOUND_JAR=$(ls target/AstraxPetRPG-*.jar 2>/dev/null | head -n1 || true)
if [ -z "$FOUND_JAR" ]; then
  echo "ERROR: No JAR found matching target/AstraxPetRPG-*.jar"
  echo "Make sure the build step produced a JAR and that the filename starts with 'AstraxPetRPG-'."
  exit 1
fi

echo "Found JAR: $FOUND_JAR"
ls -lh "$FOUND_JAR" || true
if command -v jar >/dev/null 2>&1; then
  echo "Listing jar contents (first 20 entries):"
  jar tf "$FOUND_JAR" | head -n 20 || true
fi

exit 0
