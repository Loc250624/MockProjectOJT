#!/usr/bin/env bash
set -euo pipefail

echo "=== Git status ==="
git status --short

echo
echo "=== Java ==="
java -version

echo
echo "=== Maven wrapper ==="
if [[ -x "./mvnw" ]]; then
  ./mvnw -version
else
  echo "WARNING: ./mvnw not found or not executable. Run from repository root."
fi

echo
echo "=== Assessment-related files ==="
find . \
  -path './.git' -prune -o \
  -path './target' -prune -o \
  -type f \
  \( -iname '*grading*' -o -iname '*assignment*' -o -iname '*submission*' -o \
     -iname '*quiz*' -o -iname '*question*' -o -iname '*answer*' -o \
     -iname '*testcase*' -o -iname '*attempt*' -o -iname '*result*' \) \
  -print

echo
echo "Preflight complete. This script does not modify project files."
