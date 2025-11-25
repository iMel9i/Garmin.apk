# Setup ADB in System PATH
# This script adds Android SDK platform-tools to your system PATH permanently
# Run this script ONCE with Administrator privileges

Write-Host "=== ADB PATH Setup ===" -ForegroundColor Cyan
Write-Host ""

# Check if running as Administrator
$isAdmin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)

if (-not $isAdmin) {
    Write-Host "ERROR: This script requires Administrator privileges!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please run PowerShell as Administrator and try again:" -ForegroundColor Yellow
    Write-Host "  1. Right-click on PowerShell" -ForegroundColor White
    Write-Host "  2. Select 'Run as Administrator'" -ForegroundColor White
    Write-Host "  3. Run this script again" -ForegroundColor White
    Write-Host ""
    pause
    exit 1
}

# Common ADB locations
$possiblePaths = @(
    "$env:LOCALAPPDATA\Android\Sdk\platform-tools",
    "$env:USERPROFILE\AppData\Local\Android\Sdk\platform-tools",
    "C:\Android\Sdk\platform-tools",
    "C:\Program Files\Android\Sdk\platform-tools",
    "C:\Program Files (x86)\Android\Sdk\platform-tools"
)

# Find ADB
$adbPath = $null
foreach ($path in $possiblePaths) {
    if (Test-Path "$path\adb.exe") {
        $adbPath = $path
        Write-Host "Found ADB at: $adbPath" -ForegroundColor Green
        break
    }
}

if (-not $adbPath) {
    Write-Host "ADB not found in common locations!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please enter the path to platform-tools folder manually:" -ForegroundColor Yellow
    Write-Host "(Example: C:\Users\mts88\AppData\Local\Android\Sdk\platform-tools)" -ForegroundColor Gray
    $adbPath = Read-Host "Path"
    
    if (-not (Test-Path "$adbPath\adb.exe")) {
        Write-Host "ERROR: adb.exe not found at $adbPath" -ForegroundColor Red
        exit 1
    }
}

# Get current system PATH
$currentPath = [Environment]::GetEnvironmentVariable("Path", "Machine")

# Check if already in PATH
if ($currentPath -like "*$adbPath*") {
    Write-Host ""
    Write-Host "ADB is already in system PATH!" -ForegroundColor Green
    Write-Host "Path: $adbPath" -ForegroundColor White
    Write-Host ""
    Write-Host "Testing ADB..." -ForegroundColor Yellow
    
    # Refresh environment variables for current session
    $env:Path = [System.Environment]::GetEnvironmentVariable("Path", "Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path", "User")
    
    adb version
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "ADB is working correctly!" -ForegroundColor Green
    }
    else {
        Write-Host ""
        Write-Host "Please restart PowerShell to use ADB" -ForegroundColor Yellow
    }
    
    pause
    exit 0
}

# Add to system PATH
Write-Host ""
Write-Host "Adding ADB to system PATH..." -ForegroundColor Yellow

try {
    $newPath = $currentPath + ";" + $adbPath
    [Environment]::SetEnvironmentVariable("Path", $newPath, "Machine")
    
    Write-Host "Successfully added to system PATH!" -ForegroundColor Green
    Write-Host ""
    
    # Refresh environment variables for current session
    $env:Path = [System.Environment]::GetEnvironmentVariable("Path", "Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path", "User")
    
    Write-Host "Testing ADB..." -ForegroundColor Yellow
    adb version
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "=== Setup Complete! ===" -ForegroundColor Green
        Write-Host ""
        Write-Host "ADB is now available system-wide!" -ForegroundColor Green
        Write-Host "You can use 'adb' command in any PowerShell window." -ForegroundColor White
    }
    else {
        Write-Host ""
        Write-Host "Setup complete, but please restart PowerShell to use ADB" -ForegroundColor Yellow
    }
    
}
catch {
    Write-Host "ERROR: Failed to add to PATH" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    exit 1
}

Write-Host ""
pause
