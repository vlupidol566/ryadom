# Запуск на Windows — дважды клик или: powershell -File start-windows.ps1
Set-Location $PSScriptRoot
if (-not (Get-Command node -ErrorAction SilentlyContinue)) {
    Write-Host "Установите Node.js с https://nodejs.org/"
    exit 1
}
if (-not (Test-Path "node_modules")) { npm install }
node server.js
