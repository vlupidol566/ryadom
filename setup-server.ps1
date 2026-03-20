# =====================================================================
#  Рядом — Setup Server Script для Windows 11
#  Запуск: правой кнопкой -> "Запустить с помощью PowerShell"
#  Или в PowerShell от администратора: .\setup-server.ps1
# =====================================================================

$ErrorActionPreference = "Stop"
$Host.UI.RawUI.WindowTitle = "Ryadom Server Setup"

function Write-Step { param($text) Write-Host "`n==> $text" -ForegroundColor Cyan }
function Write-Ok   { param($text) Write-Host "    [OK] $text" -ForegroundColor Green }
function Write-Fail { param($text) Write-Host "    [!!] $text" -ForegroundColor Red }
function Write-Info { param($text) Write-Host "    $text" -ForegroundColor Gray }

Write-Host @"

  ██████╗ ██╗   ██╗ █████╗ ██████╗  ██████╗ ███╗   ███╗
  ██╔══██╗╚██╗ ██╔╝██╔══██╗██╔══██╗██╔═══██╗████╗ ████║
  ██████╔╝ ╚████╔╝ ███████║██║  ██║██║   ██║██╔████╔██║
  ██╔══██╗  ╚██╔╝  ██╔══██║██║  ██║██║   ██║██║╚██╔╝██║
  ██║  ██║   ██║   ██║  ██║██████╔╝╚██████╔╝██║ ╚═╝ ██║
  ╚═╝  ╚═╝   ╚═╝   ╚═╝  ╚═╝╚═════╝  ╚═════╝ ╚═╝     ╚═╝
              Server Setup  —  v1.0
"@ -ForegroundColor Blue

# ── 1. Проверяем Node.js ─────────────────────────────────────────────
Write-Step "Проверяем Node.js..."
try {
    $nodeVersion = node --version 2>&1
    Write-Ok "Node.js установлен: $nodeVersion"
} catch {
    Write-Fail "Node.js не найден!"
    Write-Info "Скачай и установи с https://nodejs.org (LTS версию)"
    Write-Info "После установки перезапусти этот скрипт."
    Read-Host "`nНажми Enter для выхода"
    exit 1
}

# ── 2. Проверяем npm ──────────────────────────────────────────────────
$npmVersion = npm --version 2>&1
Write-Ok "npm: $npmVersion"

# ── 3. Определяем корень проекта ─────────────────────────────────────
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$backendDir   = Join-Path $scriptDir "backend"
$signalingDir = Join-Path $scriptDir "signaling-server"

Write-Step "Пути к серверам..."
Write-Info "Backend:   $backendDir"
Write-Info "Signaling: $signalingDir"

if (-not (Test-Path $backendDir)) {
    Write-Fail "Папка backend не найдена: $backendDir"
    Read-Host "Нажми Enter для выхода"; exit 1
}
if (-not (Test-Path $signalingDir)) {
    Write-Fail "Папка signaling-server не найдена: $signalingDir"
    Read-Host "Нажми Enter для выхода"; exit 1
}

# ── 4. Устанавливаем зависимости ─────────────────────────────────────
Write-Step "Устанавливаем зависимости backend (порт 3010)..."
Push-Location $backendDir
npm install --silent
Write-Ok "Backend зависимости установлены"

# ── 5. Создаём .env для backend ──────────────────────────────────────
$envFile = Join-Path $backendDir ".env"
if (-not (Test-Path $envFile)) {
    @"
PORT=3010
API_TOKEN=roman_alex_8f3a2b1c9d4e5f6a7b8c9d0e1f2a3b4c5d
"@ | Set-Content $envFile -Encoding UTF8
    Write-Ok ".env создан (PORT=3010)"
} else {
    Write-Info ".env уже существует — не перезаписываем"
}
Pop-Location

Write-Step "Устанавливаем зависимости signaling-server (порт 9090)..."
Push-Location $signalingDir
npm install --silent
Write-Ok "Signaling зависимости установлены"
Pop-Location

# ── 6. Устанавливаем PM2 ─────────────────────────────────────────────
Write-Step "Проверяем PM2..."
try {
    $pm2Ver = pm2 --version 2>&1
    Write-Ok "PM2 уже установлен: $pm2Ver"
} catch {
    Write-Info "PM2 не найден, устанавливаем..."
    npm install -g pm2
    npm install -g pm2-windows-startup
    Write-Ok "PM2 установлен"
}

# ── 7. Останавливаем старые процессы если были ───────────────────────
Write-Step "Сбрасываем старые процессы PM2..."
pm2 delete all 2>$null | Out-Null
Write-Ok "Старые процессы удалены (или их не было)"

# ── 8. Запускаем серверы ─────────────────────────────────────────────
Write-Step "Запускаем Backend на порту 3010..."
pm2 start "$backendDir\server.js" --name "ryadom-backend" --watch "$backendDir" --ignore-watch "node_modules users.json"
Write-Ok "Backend запущен"

Write-Step "Запускаем Signaling Server на порту 9090..."
pm2 start "$signalingDir\server.js" --name "ryadom-signaling" --watch "$signalingDir" --ignore-watch "node_modules"
Write-Ok "Signaling запущен"

# ── 9. Сохраняем и настраиваем автозапуск ────────────────────────────
Write-Step "Настраиваем автозапуск при старте Windows..."
pm2 save

try {
    pm2-startup install 2>$null
    Write-Ok "Автозапуск настроен через pm2-windows-startup"
} catch {
    # Альтернатива через Task Scheduler
    Write-Info "pm2-startup не сработал — используем Планировщик задач..."
    $pm2Path = (Get-Command pm2).Source
    $action = New-ScheduledTaskAction -Execute "powershell.exe" -Argument "-WindowStyle Hidden -Command `"$pm2Path resurrect`""
    $trigger = New-ScheduledTaskTrigger -AtLogOn
    $settings = New-ScheduledTaskSettingsSet -ExecutionTimeLimit (New-TimeSpan -Minutes 5)
    Register-ScheduledTask -TaskName "Ryadom PM2 Autostart" -Action $action -Trigger $trigger -Settings $settings -RunLevel Highest -Force | Out-Null
    Write-Ok "Автозапуск через Планировщик задач настроен"
}

# ── 10. Открываем порты в брандмауэре ────────────────────────────────
Write-Step "Открываем порты в Windows Firewall..."
try {
    netsh advfirewall firewall delete rule name="Ryadom Backend 3010" 2>$null | Out-Null
    netsh advfirewall firewall delete rule name="Ryadom Signaling 9090" 2>$null | Out-Null
    netsh advfirewall firewall add rule name="Ryadom Backend 3010"   dir=in action=allow protocol=TCP localport=3010 | Out-Null
    netsh advfirewall firewall add rule name="Ryadom Signaling 9090" dir=in action=allow protocol=TCP localport=9090 | Out-Null
    Write-Ok "Порты 3010 и 9090 открыты в Firewall"
} catch {
    Write-Fail "Не удалось изменить Firewall — запусти скрипт от Администратора"
}

# ── 11. Финальный статус ──────────────────────────────────────────────
Write-Host ""
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Blue
Write-Host "  СЕРВЕРЫ ЗАПУЩЕНЫ" -ForegroundColor Green
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Blue
Write-Host ""
Write-Host "  Backend  REST API  :  http://176.99.158.181:3010" -ForegroundColor White
Write-Host "  Signaling WebSocket:  ws://176.99.158.181:9090" -ForegroundColor White
Write-Host ""
Write-Host "  Проверка backend:" -ForegroundColor Gray
Write-Host "  http://176.99.158.181:3010/health" -ForegroundColor Yellow
Write-Host ""
Write-Host "  Управление серверами:" -ForegroundColor Gray
Write-Host "  pm2 status       — статус процессов" -ForegroundColor Gray
Write-Host "  pm2 logs         — логи в реальном времени" -ForegroundColor Gray
Write-Host "  pm2 restart all  — перезапустить всё" -ForegroundColor Gray
Write-Host "  pm2 stop all     — остановить всё" -ForegroundColor Gray
Write-Host ""

pm2 status

Read-Host "`nНажми Enter для выхода"
