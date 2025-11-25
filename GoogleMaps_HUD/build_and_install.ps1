# Build and Install Script for GoogleMaps HUD
# This script builds the APK and installs it on a connected Android device

# Auto-detect ADB if not in PATH
function Find-ADB {
    # Try to run adb directly first
    try {
        $null = adb version 2>&1
        return "adb"
    }
    catch {}
    
    # Search in common locations
    $possiblePaths = @(
        "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe",
        "$env:USERPROFILE\AppData\Local\Android\Sdk\platform-tools\adb.exe",
        "C:\Android\Sdk\platform-tools\adb.exe",
        "C:\Program Files\Android\Sdk\platform-tools\adb.exe",
        "C:\Program Files (x86)\Android\Sdk\platform-tools\adb.exe"
    )
    
    foreach ($path in $possiblePaths) {
        if (Test-Path $path) {
            return $path
        }
    }
    
    return $null
}

$adb = Find-ADB

if (-not $adb) {
    Write-Host "ERROR: ADB not found!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please run setup_adb.ps1 first to configure ADB" -ForegroundColor Yellow
    Write-Host "Or install Android SDK Platform Tools" -ForegroundColor Yellow
    exit 1
}

Write-Host "=== GoogleMaps HUD - Build and Install ===" -ForegroundColor Cyan
Write-Host ""

# Check if device is connected
Write-Host "Checking for connected devices..." -ForegroundColor Yellow
& $adb devices

$devices = & $adb devices | Select-String -Pattern "device$"
if ($devices.Count -eq 0) {
    Write-Host "ERROR: No Android device connected!" -ForegroundColor Red
    Write-Host "Please connect your device via USB and enable USB debugging" -ForegroundColor Yellow
    exit 1
}

Write-Host "Device found!" -ForegroundColor Green
Write-Host ""

# Build the APK
Write-Host "Building APK..." -ForegroundColor Yellow
.\gradlew assembleDebug

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Build failed!" -ForegroundColor Red
    exit 1
}

Write-Host "Build successful!" -ForegroundColor Green
Write-Host ""

# Find the APK
$apkPath = "gmaps_hud\build\outputs\apk\debug\gmaps_hud-debug.apk"

if (!(Test-Path $apkPath)) {
    Write-Host "ERROR: APK not found at $apkPath" -ForegroundColor Red
    exit 1
}

Write-Host "APK found: $apkPath" -ForegroundColor Green
Write-Host ""

# Install APK
Write-Host "Installing APK on device..." -ForegroundColor Yellow
& $adb install -r $apkPath

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Installation failed!" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "=== Installation Complete! ===" -ForegroundColor Green
Write-Host ""
Write-Host "To view logs, run:" -ForegroundColor Cyan
Write-Host "  .\view_logs.ps1" -ForegroundColor White
Write-Host ""
Write-Host "To start the app, run:" -ForegroundColor Cyan
Write-Host "  & `$adb shell am start -n sky4s.garminhud.app/.MainActivity" -ForegroundColor White
