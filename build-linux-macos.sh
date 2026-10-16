#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
if ! command -v java >/dev/null 2>&1; then echo 'ERROR: Install JDK 21 first.' >&2; exit 1; fi
if ! command -v mvn >/dev/null 2>&1; then echo 'ERROR: Install Apache Maven first.' >&2; exit 1; fi
mvn --batch-mode clean package
echo "JAR: $(pwd)/target/AstraxPetRPG-1.2.0.jar"
