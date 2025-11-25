# PowerShell script to fix the race condition in NotificationMonitor.java
$filePath = "C:\Users\mts88\Documents\GHUD\Garmin.apk\GoogleMaps_HUD\gmaps_hud\src\main\java\sky4s\garminhud\app\NotificationMonitor.java"

Write-Host "Reading file..."
$content = Get-Content $filePath -Raw -Encoding UTF8

# Create backup
$backupPath = "$filePath.backup"
Copy-Item $filePath $backupPath -Force
Write-Host "Backup created: $backupPath"

# The problematic code block to remove (lines 146-150)
$oldCode = @"
        // sometime onCreate run before sHud, that should be make garmunino app failed when android booting.
        // check null on sHud maybe can resolve no notification capture problem!?
        if (null == sHud) {
            return;
        }
"@

# The line to replace
$oldQueueSize = "        final int maxQueueSize = sHud.getMaxUpdatesPerSecond();"

# New code
$newQueueSize = @"
        // CRITICAL FIX: Use default queue size if sHud is not yet initialized
        final int maxQueueSize = (null != sHud) ? sHud.getMaxUpdatesPerSecond() : 10;
"@

# Apply fixes
Write-Host "Applying fixes..."
$content = $content -replace [regex]::Escape($oldCode), ""
$content = $content -replace [regex]::Escape($oldQueueSize), $newQueueSize

# Write back
Set-Content $filePath $content -Encoding UTF8 -NoNewline
Write-Host "✓ Fix applied successfully!"
Write-Host "File: $filePath"
