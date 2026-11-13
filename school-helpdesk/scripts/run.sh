#!/usr/bin/env bash
# ============================================================
#  School HelpDesk — быстрый запуск для разработки/демонстрации
#  без PostgreSQL: встроенная H2 + демонстрационные данные.
#
#  http://localhost:8080   (admin / admin123)
#  Swagger UI: http://localhost:8080/swagger-ui.html
#
#  Требования: Java 21+, Maven; Node.js — только если фронтенд
#  ещё не собран в backend/src/main/resources/static.
# ============================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# если статика фронтенда отсутствует — собрать
if [ ! -f "$ROOT/backend/src/main/resources/static/index.html" ]; then
  echo "==> Фронтенд не собран — собираю…"
  cd "$ROOT/frontend"
  npm install --no-audit --no-fund
  npm run build:only
fi

cd "$ROOT/backend"
if [ -x ./mvnw ]; then MVN=./mvnw; else MVN=mvn; fi

echo "==> Запуск на http://localhost:8080 (профиль dev: H2 + демо-данные)"
exec "$MVN" spring-boot:run -Dspring-boot.run.profiles=dev
