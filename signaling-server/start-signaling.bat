@echo off
REM Запуск сервера сигналинга «Роман Александров» на Windows
REM Дважды кликни по этому файлу или запусти из cmd.

cd /d "%~dp0"

REM Установка зависимостей (один раз или при изменениях)
if not exist node_modules (
  echo Installing npm dependencies...
  npm install
)

echo Starting signaling server on port 9090...
echo (Не закрывай это окно, пока сервер нужен.)
npm start

