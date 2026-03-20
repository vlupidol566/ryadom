@echo off
chcp 65001 >nul
title Ryadom Server Setup

echo.
echo  ==========================================
echo   RYADOM - Server Setup
echo   Backend :3010  ^|  Signaling :9090
echo  ==========================================
echo.

:: ── 1. Node.js ──────────────────────────────────────────────────────
echo [1/7] Checking Node.js...
node --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [!!] Node.js not found! Download from https://nodejs.org
    pause & exit /b 1
)
for /f "tokens=*" %%v in ('node --version 2^>nul') do echo [OK] Node.js %%v

:: ── 2. Folders ──────────────────────────────────────────────────────
set "ROOT=%~dp0"
if "%ROOT:~-1%"=="\" set "ROOT=%ROOT:~0,-1%"
set "BACKEND=%ROOT%\backend"
set "SIGNALING=%ROOT%\signaling-server"
set "PM2=%APPDATA%\npm\node_modules\pm2\bin\pm2"

echo.
echo [2/7] Checking folders...
if not exist "%BACKEND%"   ( echo [!!] backend not found   & pause & exit /b 1 )
if not exist "%SIGNALING%" ( echo [!!] signaling not found & pause & exit /b 1 )
echo [OK] Backend:   %BACKEND%
echo [OK] Signaling: %SIGNALING%

:: ── 3. npm install backend ──────────────────────────────────────────
echo.
echo [3/7] Installing backend dependencies...
cd /d "%BACKEND%"
call npm install --silent 2>nul
echo [OK] Backend deps installed

:: Fix .env — always ensure API_TOKEN is present
if not exist "%BACKEND%\.env" (
    echo PORT=3010 > "%BACKEND%\.env"
    echo API_TOKEN=roman_alex_8f3a2b1c9d4e5f6a7b8c9d0e1f2a3b4c5d >> "%BACKEND%\.env"
    echo [OK] .env created
) else (
    findstr /c:"API_TOKEN" "%BACKEND%\.env" >nul 2>&1
    if %errorlevel% neq 0 (
        echo API_TOKEN=roman_alex_8f3a2b1c9d4e5f6a7b8c9d0e1f2a3b4c5d >> "%BACKEND%\.env"
        echo [OK] API_TOKEN added to .env
    ) else (
        echo [OK] .env OK
    )
)

:: ── 4. npm install signaling ────────────────────────────────────────
echo.
echo [4/7] Installing signaling-server dependencies...
cd /d "%SIGNALING%"
call npm install --silent 2>nul
echo [OK] Signaling deps installed

:: ── 5. Install PM2 if missing ───────────────────────────────────────
echo.
echo [5/7] Checking PM2...
if not exist "%PM2%" (
    echo      Installing PM2 via npm...
    call npm install -g pm2 2>nul
    echo [OK] PM2 installed
) else (
    echo [OK] PM2 found
)

:: ── 6. Start servers via node pm2 directly ─────────────────────────
echo.
echo [6/7] Starting servers...

:: Stop old instances (ignore errors)
node "%PM2%" delete ryadom-backend   >nul 2>&1
node "%PM2%" delete ryadom-signaling >nul 2>&1

:: Start backend
node "%PM2%" start "%BACKEND%\server.js" --name "ryadom-backend"
if %errorlevel% neq 0 (
    echo [!!] Failed to start backend
    pause & exit /b 1
)
echo [OK] Backend started on port 3010

:: Start signaling
node "%PM2%" start "%SIGNALING%\server.js" --name "ryadom-signaling"
if %errorlevel% neq 0 (
    echo [!!] Failed to start signaling
    pause & exit /b 1
)
echo [OK] Signaling started on port 9090

:: Save process list
node "%PM2%" save >nul 2>&1
echo [OK] PM2 list saved

:: ── 7. Firewall + Autostart ─────────────────────────────────────────
echo.
echo [7/7] Firewall and autostart...

netsh advfirewall firewall delete rule name="Ryadom Backend 3010"   >nul 2>&1
netsh advfirewall firewall delete rule name="Ryadom Signaling 9090" >nul 2>&1
netsh advfirewall firewall add rule name="Ryadom Backend 3010"   dir=in action=allow protocol=TCP localport=3010 >nul
netsh advfirewall firewall add rule name="Ryadom Signaling 9090" dir=in action=allow protocol=TCP localport=9090 >nul
echo [OK] Ports 3010 and 9090 opened in Firewall

schtasks /query /tn "Ryadom PM2 Autostart" >nul 2>&1
if %errorlevel% neq 0 (
    schtasks /create /tn "Ryadom PM2 Autostart" /tr "node \"%PM2%\" resurrect" /sc onlogon /rl highest /f >nul
    echo [OK] Autostart on boot configured
) else (
    echo [OK] Autostart already configured
)

:: ── Done ────────────────────────────────────────────────────────────
echo.
echo  ==========================================
echo   SERVERS ARE RUNNING
echo  ==========================================
echo.
echo   Backend  : http://176.99.158.181:3010
echo   Signaling: ws://176.99.158.181:9090
echo.
echo   Health check (open in browser):
echo   http://176.99.158.181:3010/health
echo.
echo   Useful commands:
echo   node "%PM2%" status
echo   node "%PM2%" logs
echo   node "%PM2%" restart all
echo.

node "%PM2%" status

echo.
pause
