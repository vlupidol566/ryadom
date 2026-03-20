# Ryadom - Server Setup Script for Windows 11
# Run as Administrator: powershell -ExecutionPolicy Bypass -File .\setup-server.ps1

$ErrorActionPreference = "Stop"

function Step  { Write-Host "`n==> $args" -ForegroundColor Cyan }
function Ok    { Write-Host "    [OK] $args" -ForegroundColor Green }
function Fail  { Write-Host "    [!!] $args" -ForegroundColor Red }
function Info  { Write-Host "    $args" -ForegroundColor Gray }

Write-Host ""
Write-Host "  RYADOM - Server Setup" -ForegroundColor Blue
Write-Host "  Backend :3010  |  Signaling :9090" -ForegroundColor Blue
Write-Host ""

# 1. Check Node.js
Step "Checking Node.js..."
try {
    $nodeVer = node --version 2>&1
    Ok "Node.js: $nodeVer"
} catch {
    Fail "Node.js not found!"
    Info "Download from https://nodejs.org (LTS)"
    Read-Host "Press Enter to exit"
    exit 1
}

$npmVer = npm --version 2>&1
Ok "npm: $npmVer"

# 2. Paths
$root          = Split-Path -Parent $MyInvocation.MyCommand.Path
$backendDir    = Join-Path $root "backend"
$signalingDir  = Join-Path $root "signaling-server"

Step "Checking folders..."
Info "Backend:   $backendDir"
Info "Signaling: $signalingDir"

if (-not (Test-Path $backendDir))   { Fail "backend folder not found";          Read-Host "Press Enter"; exit 1 }
if (-not (Test-Path $signalingDir)) { Fail "signaling-server folder not found"; Read-Host "Press Enter"; exit 1 }

# 3. npm install backend
Step "Installing backend dependencies (port 3010)..."
Push-Location $backendDir
npm install --silent 2>&1 | Out-Null
Ok "Backend deps installed"

# 4. Create .env
$envFile = Join-Path $backendDir ".env"
if (-not (Test-Path $envFile)) {
    Set-Content -Path $envFile -Value "PORT=3010" -Encoding UTF8
    Add-Content -Path $envFile -Value "API_TOKEN=roman_alex_8f3a2b1c9d4e5f6a7b8c9d0e1f2a3b4c5d"
    Ok ".env created (PORT=3010)"
} else {
    Info ".env already exists - skipping"
}
Pop-Location

# 5. npm install signaling
Step "Installing signaling-server dependencies (port 9090)..."
Push-Location $signalingDir
npm install --silent 2>&1 | Out-Null
Ok "Signaling deps installed"
Pop-Location

# 6. Install PM2
Step "Checking PM2..."
$pm2Ok = $false
try {
    $pm2Ver = pm2 --version 2>&1
    Ok "PM2 already installed: $pm2Ver"
    $pm2Ok = $true
} catch {
    Info "Installing PM2..."
    npm install -g pm2 2>&1 | Out-Null
    npm install -g pm2-windows-startup 2>&1 | Out-Null
    Ok "PM2 installed"
    $pm2Ok = $true
}

# 7. Stop old processes (ignore errors if none exist)
Step "Stopping old PM2 processes..."
$ErrorActionPreference = "Continue"
pm2 delete all 2>&1 | Out-Null
$ErrorActionPreference = "Stop"
Ok "Old processes cleared"

# 8. Start servers
Step "Starting Backend on port 3010..."
$backendJs   = Join-Path $backendDir "server.js"
$signalingJs = Join-Path $signalingDir "server.js"

$ErrorActionPreference = "Continue"
pm2 start $backendJs --name "ryadom-backend" 2>&1 | Out-Null
$ErrorActionPreference = "Stop"
Ok "Backend started"

Step "Starting Signaling Server on port 9090..."
$ErrorActionPreference = "Continue"
pm2 start $signalingJs --name "ryadom-signaling" 2>&1 | Out-Null
$ErrorActionPreference = "Stop"
Ok "Signaling started"

# 9. Save + autostart
Step "Saving PM2 process list..."
$ErrorActionPreference = "Continue"
pm2 save 2>&1 | Out-Null
$ErrorActionPreference = "Stop"
Ok "Saved"

Step "Setting up autostart on Windows boot..."
try {
    pm2-startup install 2>$null | Out-Null
    Ok "Autostart configured via pm2-windows-startup"
} catch {
    Info "Falling back to Task Scheduler..."
    try {
        $pm2Path = (Get-Command pm2 -ErrorAction Stop).Source
        $action   = New-ScheduledTaskAction -Execute "powershell.exe" -Argument ("-WindowStyle Hidden -Command `"& '" + $pm2Path + "' resurrect`"")
        $trigger  = New-ScheduledTaskTrigger -AtLogOn
        $settings = New-ScheduledTaskSettingsSet -ExecutionTimeLimit (New-TimeSpan -Minutes 5)
        Register-ScheduledTask -TaskName "Ryadom PM2 Autostart" -Action $action -Trigger $trigger -Settings $settings -RunLevel Highest -Force | Out-Null
        Ok "Autostart via Task Scheduler configured"
    } catch {
        Info "Could not configure autostart - run 'pm2 resurrect' manually after reboot"
    }
}

# 10. Firewall rules
Step "Opening ports in Windows Firewall..."
try {
    netsh advfirewall firewall delete rule name="Ryadom Backend 3010"   2>$null | Out-Null
    netsh advfirewall firewall delete rule name="Ryadom Signaling 9090" 2>$null | Out-Null
    netsh advfirewall firewall add rule name="Ryadom Backend 3010"   dir=in action=allow protocol=TCP localport=3010 | Out-Null
    netsh advfirewall firewall add rule name="Ryadom Signaling 9090" dir=in action=allow protocol=TCP localport=9090 | Out-Null
    Ok "Ports 3010 and 9090 opened in Firewall"
} catch {
    Fail "Firewall update failed - run this script as Administrator"
}

# 11. Status
Write-Host ""
Write-Host "---------------------------------------------------" -ForegroundColor Blue
Write-Host "  SERVERS ARE RUNNING" -ForegroundColor Green
Write-Host "---------------------------------------------------" -ForegroundColor Blue
Write-Host ""
Write-Host "  Backend  REST API : http://176.99.158.181:3010" -ForegroundColor White
Write-Host "  Signaling WebSocket: ws://176.99.158.181:9090"  -ForegroundColor White
Write-Host ""
Write-Host "  Health check: http://176.99.158.181:3010/health" -ForegroundColor Yellow
Write-Host ""
Write-Host "  pm2 status       - show process list" -ForegroundColor Gray
Write-Host "  pm2 logs         - live logs" -ForegroundColor Gray
Write-Host "  pm2 restart all  - restart everything" -ForegroundColor Gray
Write-Host "  pm2 stop all     - stop everything" -ForegroundColor Gray
Write-Host ""

pm2 status

Write-Host ""
Read-Host "Press Enter to exit"
