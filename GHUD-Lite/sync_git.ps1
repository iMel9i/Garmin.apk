$commitMessage = Read-Host -Prompt "Enter commit message"
git add .
git commit -m "$commitMessage"
git push
