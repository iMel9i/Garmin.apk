[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
$commitMessage = Read-Host -Prompt "Enter commit message"
git add .
git commit -m "$commitMessage"
git push
