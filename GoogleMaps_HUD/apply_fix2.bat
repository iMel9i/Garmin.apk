@echo off
echo Applying null safety fixes to NotificationMonitor.java...

powershell -Command "(Get-Content 'gmaps_hud\src\main\java\sky4s\garminhud\app\NotificationMonitor.java' -Raw) -replace 'String distance = title_str\[0\]\.trim\(\);\r\n                if \(Character\.isDigit\(distance\.charAt\(0\)\)\) \{', 'String distance = title_str[0].trim();`r`n                // CRITICAL FIX: Check if distance is not empty before charAt()`r`n                if (!distance.isEmpty() && Character.isDigit(distance.charAt(0))) {' | Set-Content 'gmaps_hud\src\main\java\sky4s\garminhud\app\NotificationMonitor.java' -NoNewline"

echo.
echo Fix applied! Building app...
call build_and_install.ps1
