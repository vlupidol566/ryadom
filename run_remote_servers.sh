#!/usr/bin/env bash

set -e

# Конфигурация
REMOTE_HOST="176.99.158.181"
# ЗАМЕНИ на своё имя пользователя на сервере:
REMOTE_USER="${REMOTE_USER:-ubuntu}"

# Путь к проекту на удалёнке (поправь, если у тебя другой)
REMOTE_PROJECT_DIR="~/RomanAlexandrov"

# Токен должен совпадать с backend/.env на сервере
API_TOKEN="roman_alex_8f3a2b1c9d4e5f6a7b8c9d0e1f2a3b4c5d"

echo "===> Подключаюсь к ${REMOTE_USER}@${REMOTE_HOST} и запускаю серверы..."

ssh "${REMOTE_USER}@${REMOTE_HOST}" bash -lc "set -e
  echo '===> Перехожу в проект: ${REMOTE_PROJECT_DIR}'
  cd ${REMOTE_PROJECT_DIR}

  echo '===> Запуск signaling-server (WebRTC)...'
  cd signaling-server
  npm install
  nohup PORT=9090 node server.js > signaling.log 2>&1 &

  echo '===> Запуск backend (пользователи + help-requests)...'
  cd ../backend
  npm install
  nohup PORT=3001 API_TOKEN=${API_TOKEN} node server.js > backend.log 2>&1 &

  echo '===> Серверы запущены в фоне'
"

echo "===> Проверяю доступность backend /health..."
curl -sS "http://${REMOTE_HOST}:3001/health" || echo 'Не удалось получить ответ от backend'

echo "Готово. Приложение \"Рядом\" теперь должно работать с удалённым сервером ${REMOTE_HOST}."

