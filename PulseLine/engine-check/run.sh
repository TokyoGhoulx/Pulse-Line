#!/bin/sh
# Compiles and runs the engine self-check with plain Java (no Android SDK needed).
set -e
cd "$(dirname "$0")"
OUT="${TMPDIR:-/tmp}/pulseline-engine-check"
rm -rf "$OUT" && mkdir -p "$OUT"
javac -encoding UTF-8 -d "$OUT" ../app/src/main/java/com/pulseline/app/engine/*.java EngineCheck.java
java -cp "$OUT" EngineCheck
