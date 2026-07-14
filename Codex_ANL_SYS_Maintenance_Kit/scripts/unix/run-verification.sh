#!/usr/bin/env bash
set +e

echo "=== Git status ==="
git status
git branch --show-current
git log -5 --oneline

echo
echo "=== Searching ANL/SYS files ==="
for root in src/main/java src/main/resources/templates src/main/resources/static src/test; do
  if [ -d "$root" ]; then
    grep -RInE "analytics|dashboard|revenue|earning|income|payment|order|transaction|active student|system setting|configuration" "$root" 2>/dev/null
  fi
done

echo
echo "=== Compile and tests ==="
if [ -x "./mvnw" ]; then
  ./mvnw -q -DskipTests compile
  ./mvnw test
elif command -v mvn >/dev/null 2>&1; then
  mvn -q -DskipTests compile
  mvn test
else
  echo "Không tìm thấy Maven Wrapper hoặc mvn."
fi

echo
echo "=== Final git status ==="
git status
