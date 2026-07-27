#!/usr/bin/env bash
set -euo pipefail

echo "== Search duplicate quiz paths =="
grep -R "createQuizAttempt" -n \
  src/main/java/com/ojtsu26/elearning || true

grep -R '"type", "QUIZ"' -n \
  src/main/java/com/ojtsu26/elearning || true

grep -R "findByQuizIdOrderByDisplayOrderAscIdAsc" -n \
  src/main/java/com/ojtsu26/elearning || true

echo "== Compile and tests =="
./mvnw test
./mvnw clean package

echo "== Safety check: chatbot client must not be in quiz runtime =="
if grep -R "OpenAiResponsesClient" -n \
  src/main/java/com/ojtsu26/elearning/service/quiz; then
  echo "ERROR: chatbot client referenced by Student quiz runtime"
  exit 1
fi

echo "Verification completed."
