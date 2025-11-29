[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
# Build and Install Script for GHUD Lite
# Description: Builds APK and installs on connected device

Write-Host "==================================" -ForegroundColor Cyan
Write-Host "  GHUD Lite - Build & Install" -ForegroundColor Cyan
Write-Host "==================================" -ForegroundColor Cyan
Write-Host ""

# Путь к проекту
$projectPath = "C:\Users\mts88\Documents\GHUD\Garmin.apk\ghud-lite"
$adbPath = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"

# Проверяем наличие проекта
if (-not (Test-Path $projectPath)) {
    Write-Host "Error: Project not found at path: $projectPath" -ForegroundColor Red
    exit 1
}

# Переходим в директорию проекта
Set-Location $projectPath

# Шаг 1: Очистка предыдущих сборок
Write-Host "[1/4] Cleaning previous builds..." -ForegroundColor Yellow
.\gradlew clean

# Шаг 2: Сборка APK
Write-Host ""
Write-Host "[2/4] Building APK (Debug)..." -ForegroundColor Yellow
.\gradlew assembleDebug

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "Error: Build failed!" -ForegroundColor Red
    exit 1
}

# Путь к собранному APK
$apkPath = "$projectPath\app\build\outputs\apk\debug\app-debug.apk"

# Проверяем наличие APK
if (-not (Test-Path $apkPath)) {
    Write-Host ""
    Write-Host "Error: APK not found at path: $apkPath" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Build successful!" -ForegroundColor Green
Write-Host "APK: $apkPath" -ForegroundColor Gray

# Шаг 3: Проверка подключения устройства
Write-Host ""
Write-Host "[3/4] Checking device connection..." -ForegroundColor Yellow

if (-not (Test-Path $adbPath)) {
    Write-Host "Error: ADB not found at path: $adbPath" -ForegroundColor Red
    exit 1
}

# Проверяем, есть ли уже подключенные устройства
$devices = & $adbPath devices
$deviceLines = $devices | Select-Object -Skip 1 | Where-Object { $_ -match '\tdevice$' }

$deviceFound = $false

if ($deviceLines) {
    $deviceFound = $true
    Write-Host "Device found!" -ForegroundColor Green
}
else {
    # Пытаемся подключиться по WiFi
    Write-Host "No USB devices found. Trying WiFi connection..." -ForegroundColor Yellow
    
    # Запрашиваем IP адрес устройства
    $deviceIP = Read-Host "Enter device IP address (or press Enter to skip)"
    
    if ($deviceIP) {
        Write-Host "Connecting to $deviceIP`:5555..." -ForegroundColor Yellow
        & $adbPath connect "$deviceIP`:5555"
        
        Start-Sleep -Seconds 2
        
        # Проверяем подключение
        $devices = & $adbPath devices
        $deviceLines = $devices | Select-Object -Skip 1 | Where-Object { $_ -match '\tdevice$' }
        
        if ($deviceLines) {
            $deviceFound = $true
            Write-Host "WiFi device connected!" -ForegroundColor Green
        }
        else {
            Write-Host "Failed to connect via WiFi" -ForegroundColor Red
        }
    }
}

if (-not $deviceFound) {
    Write-Host ""
    Write-Host "Warning: No devices found." -ForegroundColor Yellow
    Write-Host "To enable WiFi debugging:" -ForegroundColor Yellow
    Write-Host "1. Connect device via USB first" -ForegroundColor Yellow
    Write-Host "2. Run: adb tcpip 5555" -ForegroundColor Yellow
    Write-Host "3. Find device IP in Settings > About > Status" -ForegroundColor Yellow
    Write-Host "4. Run this script again" -ForegroundColor Yellow
    
    $response = Read-Host "Continue installation anyway (y/n)?"
    if ($response -ne 'y') {
        exit 0
    }
}

# Шаг 4: Установка APK
Write-Host ""
Write-Host "[4/4] Installing APK on device..." -ForegroundColor Yellow

& $adbPath install -r $apkPath

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "=====================================" -ForegroundColor Green
    Write-Host "  Installation successful!" -ForegroundColor Green
    Write-Host "=====================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "Launch GHUD Lite on device" -ForegroundColor Cyan
}
else {
    Write-Host ""
    Write-Host "Error installing APK!" -ForegroundColor Red
    exit 1
}
