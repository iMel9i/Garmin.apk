# Build and Install Script for GHUD Lite
# Описание: Собирает APK и устанавливает на подключенное устройство

Write-Host "==================================" -ForegroundColor Cyan
Write-Host "  GHUD Lite - Build & Install" -ForegroundColor Cyan
Write-Host "==================================" -ForegroundColor Cyan
Write-Host ""

# Путь к проекту
$projectPath = "C:\Users\mts88\Documents\GHUD\Garmin.apk\ghud-lite"
$adbPath = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"

# Проверяем наличие проекта
if (-not (Test-Path $projectPath)) {
    Write-Host "Ошибка: Проект не найден по пути: $projectPath" -ForegroundColor Red
    exit 1
}

# Переходим в директорию проекта
Set-Location $projectPath

# Шаг 1: Очистка предыдущих сборок
Write-Host "[1/4] Очистка предыдущих сборок..." -ForegroundColor Yellow
.\gradlew clean

# Шаг 2: Сборка APK
Write-Host ""
Write-Host "[2/4] Сборка APK (Debug)..." -ForegroundColor Yellow
.\gradlew assembleDebug

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "Ошибка: Сборка завершилась с ошибкой!" -ForegroundColor Red
    exit 1
}

# Путь к собранному APK
$apkPath = "$projectPath\app\build\outputs\apk\debug\app-debug.apk"

# Проверяем наличие APK
if (-not (Test-Path $apkPath)) {
    Write-Host ""
    Write-Host "Ошибка: APK не найден по пути: $apkPath" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Сборка завершена успешно!" -ForegroundColor Green
Write-Host "APK: $apkPath" -ForegroundColor Gray

# Шаг 3: Проверка подключения устройства
Write-Host ""
Write-Host "[3/4] Проверка подключения устройства..." -ForegroundColor Yellow

if (-not (Test-Path $adbPath)) {
    Write-Host "Ошибка: ADB не найден по пути: $adbPath" -ForegroundColor Red
    exit 1
}

$devices = & $adbPath devices | Select-Object -Skip 1 | Where-Object { $_ -match '\tdevice$' }

if ($devices.Count -eq 0) {
    Write-Host "Предупреждение: Устройства не обнаружены" -ForegroundColor Yellow
    Write-Host "Подключите устройство через USB и включите отладку по USB" -ForegroundColor Yellow
    
    $response = Read-Host "Продолжить установку (y/n)?"
    if ($response -ne 'y') {
        exit 0
    }
} else {
    Write-Host "Устройство обнаружено" -ForegroundColor Green
}

# Шаг 4: Установка APK
Write-Host ""
Write-Host "[4/4] Установка APK на устройство..." -ForegroundColor Yellow

& $adbPath install -r $apkPath

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "=====================================" -ForegroundColor Green
    Write-Host "  Установка завершена успешно!" -ForegroundColor Green
    Write-Host "=====================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "Запустите приложение GHUD Lite на устройстве" -ForegroundColor Cyan
} else {
    Write-Host ""
    Write-Host "Ошибка при установке APK!" -ForegroundColor Red
    exit 1
}
