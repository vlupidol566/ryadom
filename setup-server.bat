@echo off
chcp 65001 >nul
title Ryadom Server Setup

echo.
echo  ==========================================
echo   RYADOM - Server Setup
echo   Backend :3010  ^|  Signaling :9090
echo  ==========================================
echo.

:: Check Node.js
echo [1/8] Checking Node.js...
node --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [!!] Node.js not found!
    echo      Download from https://nodejs.org
    pause
    exit /b 1
)
for /f %%v in ('node --version') do echo [OK] Node.js: %%v
for /f %%v in ('npm --version') do echo [OK] npm: %%v

:: Get script directory
set ROOT=%~dp0
set ROOT=%ROOT:~0,-1%
set BACKEND=%ROOT%\backend
set SIGNALING=%ROOT%\signaling-server

echo.
echo [2/8] Checking folders...
echo      Backend:   %BACKEND%
echo      Signaling: %SIGNALING%

if not exist "%BACKEND%" (
    echo [!!] backend folder not found
    pause & exit /b 1
)
if not exist "%SIGNALING%" (
    echo [!!] signaling-server folder not found
    pause & exit /b 1
)

:: Install backend deps
echo.
echo [3/8] Installing backend dependencies...
cd /d "%BACKEND%"
call npm install --silent
echo [OK] Backend deps installed

:: Create .env
if not exist "%BACKEND%\.env" (
    echo PORT=3010 > "%BACKEND%\.env"
    echo API_TOKEN=roman_alex_8f3a2b1c9d4e5f6a7b8c9d0e1f2a3b4c5d >> "%BACKEND%\.env"
    echo [OK] .env created
) else (
    :: Make sure API_TOKEN is in the file
    findstr /c:"API_TOKEN" "%BACKEND%\.env" >nul 2>&1
    if %errorlevel% neq 0 (
        echo API_TOKEN=roman_alex_8f3a2b1c9d4e5f6a7b8c9d0e1f2a3b4c5d >> "%BACKEND%\.env"
        echo [OK] API_TOKEN added to existing .env
    ) else (
        echo [OK] .env already exists
    )
    :: Make sure PORT=3010 is set
    findstr /c:"PORT=3010" "%BACKEND%\.env" >nul 2>&1
    if %errorlevel% neq 0 (
        echo PORT=3010 >> "%BACKEND%\.env"
        echo [OK] PORT=3010 added to .env
    )
)

:: Install signaling deps
echo.
echo [4/8] Installing signaling-server dependencies...
cd /d "%SIGNALING%"
call npm install --silent
echo [OK] Signaling deps installed

:: Install PM2
echo.
echo [5/8] Checking PM2...
pm2 --version >nul 2>&1
if %errorlevel% neq 0 (
    echo      Installing PM2...
    call npm install -g pm2
    call npm install -g pm2-windows-startup
    echo [OK] PM2 installed
) else (
    for /f %%v in ('pm2 --version') do echo [OK] PM2 already installed: %%v
)

:: Stop old processes
echo.
echo [6/8] Stopping old PM2 processes...
pm2 delete all >nul 2>&1
echo [OK] Done

:: Start servers
echo.
echo [7/8] Starting servers...
pm2 start "%BACKEND%\server.js" --name "ryadom-backend"
pm2 start "%SIGNALING%\server.js" --name "ryadom-signaling"
pm2 save
echo [OK] Both servers started

:: Firewall rules
echo.
echo [8/8] Opening ports in Windows Firewall...
netsh advfirewall firewall delete rule name="Ryadom Backend 3010" >nul 2>&1
netsh advfirewall firewall delete rule name="Ryadom Signaling 9090" >nul 2>&1
netsh advfirewall firewall add rule name="Ryadom Backend 3010"   dir=in action=allow protocol=TCP localport=3010 >nul
netsh advfirewall firewall add rule name="Ryadom Signaling 9090" dir=in action=allow protocol=TCP localport=9090 >nul
echo [OK] Ports 3010 and 9090 opened

:: Autostart via Task Scheduler
schtasks /query /tn "Ryadom PM2 Autostart" >nul 2>&1
if %errorlevel% neq 0 (
    schtasks /create /tn "Ryadom PM2 Autostart" /tr "pm2 resurrect" /sc onlogon /rl highest /f >nul
    echo [OK] Autostart on boot configured
) else (
    echo [OK] Autostart already configured
)

:: Final status
echo.
echo  ==========================================
echo   SERVERS ARE RUNNING
echo  ==========================================
echo.
echo   Backend  REST API:  http://176.99.158.181:3010
echo   Signaling WebSocket: ws://176.99.158.181:9090
echo.
echo   Health check (open in browser):
echo   http://176.99.158.181:3010/health
echo.
echo   PM2 commands:
echo   pm2 status        - show processes
echo   pm2 logs          - live logs
echo   pm2 restart all   - restart
echo   pm2 stop all      - stop
echo.

pm2 status

echo.
pause
