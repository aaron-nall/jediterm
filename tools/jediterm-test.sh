#!/usr/bin/env bash
cd "$(dirname "$0")/.." || exit 1
./gradlew -q :JediTerm:runTestApp --args="$*"
