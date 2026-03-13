@echo off
setlocal

REM Корневая папка проекта (папка, где лежит этот .bat)
set PROJECT_DIR=%~dp0

REM Токен должен совпадать с backend\.env на сервере
set API_TOKEN=roman_alex_8f3a2b1c9d4e5f6a7b8c9d0e1f2a3b4c5d

echo ===> Запуск signaling-server (WebRTC)...
cd /d "%PROJECT_DIR%signaling-server"
call npm install

REM Запуск в отдельном окне, на порту 9090
start "signaling-server" cmd /c "set PORT=9090 && node server.js"

echo ===> Запуск backend (пользователи + help-requests)...
cd /d "%PROJECT_DIR%backend"
call npm install

REM Запуск в отдельном окне, на порту 3001 с API_TOKEN
start "backend" cmd /c "set PORT=3001 && set API_TOKEN=%API_TOKEN% && node server.js"

echo ===> Серверы запущены (два отдельных окна cmd).
echo ===> signaling: ws://<этот_IP>:9090, backend: http://<этот_IP>:3001
pause
endlocal

