@echo off
echo Collecting crash logs...
C:\Users\mts88\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat -d -s AndroidRuntime:E NotificationMonitor:E NotificationMonitor:W > crash_log.txt
echo.
echo Logs saved to crash_log.txt
echo.
type crash_log.txt
