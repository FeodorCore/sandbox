#!/usr/bin/env bash
# ============================================================
#  School HelpDesk — полная production-сборка
#  1) собирает React-фронтенд в backend/src/main/resources/static
#  2) собирает исполняемый JAR: backend/target/school-helpdesk.jar
#
#  Требования: Node.js 18+, Java (JDK) 21+, Maven 3.6+
#  Запуск:     bash scripts/build.sh   (из корня проекта)
# ============================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

echo "==> [1/2] Сборка фронтенда (Vite → backend/src/main/resources/static)"
cd frontend
if command -v npm >/dev/null 2>&1; then
  npm ci --no-audit --no-fund 2>/dev/null || npm install --no-audit --no-fund
  npm run build          # tsc --noEmit + vite build (проверка типов обязательна)
else
  echo "ОШИБКА: npm не найден. Установите Node.js 18+." >&2
  exit 1
fi

echo "==> [2/2] Сборка backend (Maven → school-helpdesk.jar)"
cd "$ROOT/backend"
if [ -x ./mvnw ]; then
  MVN=./mvnw
elif command -v mvn >/dev/null 2>&1; then
  MVN=mvn
else
  echo "ОШИБКА: Maven не найден (mvn). Установите Maven 3.6+." >&2
  exit 1
fi
"$MVN" -B clean package -DskipTests

echo
echo "Готово: $ROOT/backend/target/school-helpdesk.jar"
echo "Запуск: SERVER_PORT=80 APP_JWT_SECRET='<секрет 32+ символа>' java -jar backend/target/school-helpdesk.jar"
