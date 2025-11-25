# Script to install OpenJDK 17 and set JAVA_HOME
# Run this in PowerShell as Administrator

Write-Host "Downloading OpenJDK 17..."
$url = "https://download.java.net/java/GA/jdk17.0.2/dfd4a8d0985749f896bed50d7138ee7f/8/GPL/openjdk-17.0.2_windows-x64_bin.zip"
$output = "$env:TEMP\openjdk-17.zip"
Invoke-WebRequest -Uri $url -OutFile $output

Write-Host "Extracting to C:\Java..."
$dest = "C:\Java"
if (!(Test-Path $dest)) {
    New-Item -ItemType Directory -Force -Path $dest | Out-Null
}
Expand-Archive -Path $output -DestinationPath $dest -Force

$jdkPath = "$dest\jdk-17.0.2"

Write-Host "Setting JAVA_HOME to $jdkPath..."
[System.Environment]::SetEnvironmentVariable("JAVA_HOME", $jdkPath, [System.EnvironmentVariableTarget]::User)

Write-Host "Adding to PATH..."
$currentPath = [System.Environment]::GetEnvironmentVariable("Path", [System.EnvironmentVariableTarget]::User)
if ($currentPath -notlike "*$jdkPath\bin*") {
    $newPath = "$currentPath;$jdkPath\bin"
    [System.Environment]::SetEnvironmentVariable("Path", $newPath, [System.EnvironmentVariableTarget]::User)
}

Write-Host "Done! Please restart your terminal/IDE for changes to take effect."
Write-Host "You can verify by running: java -version"
