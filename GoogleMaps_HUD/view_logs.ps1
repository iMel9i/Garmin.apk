# View Logs Script for GoogleMaps HUD
# This script shows real-time logs from the app with filtering

param(
    [switch]$All,  # Show all logs without filtering
    [switch]$Permissions,  # Show only permission-related logs
    [switch]$Clear  # Clear logcat before starting
)

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
    exit 1
}

Write-Host "=== GoogleMaps HUD - Log Viewer ===" -ForegroundColor Cyan
Write-Host ""

# Check if device is connected
$devices = & $adb devices | Select-String -Pattern "device$"
if ($devices.Count -eq 0) {
    Write-Host "ERROR: No Android device connected!" -ForegroundColor Red
    exit 1
}

if ($Clear) {
    Write-Host "Clearing logcat..." -ForegroundColor Yellow
    & $adb logcat -c
}

Write-Host "Starting log viewer..." -ForegroundColor Green
Write-Host "Press Ctrl+C to stop" -ForegroundColor Yellow
Write-Host ""
Write-Host "----------------------------------------" -ForegroundColor Gray

if ($Permissions) {
    # Show only permission-related logs
    Write-Host "Filtering: Permission logs only" -ForegroundColor Cyan
    & $adb logcat -v time | Select-String -Pattern "Permission|permission|PERMISSION"
}
elseif ($All) {
    # Show all logs
    Write-Host "Showing: All logs" -ForegroundColor Cyan
    & $adb logcat -v time
}
else {
    # Show only app logs
    Write-Host "Filtering: GoogleMaps HUD app logs" -ForegroundColor Cyan
    & $adb logcat -v time | Select-String -Pattern "MainActivity|PermissionManager|garminhud|GoogleMaps"
}
