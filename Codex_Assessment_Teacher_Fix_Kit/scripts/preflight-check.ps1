$ErrorActionPreference = "Stop"

Write-Host "=== Git status ==="
git status --short

Write-Host "`n=== Java ==="
java -version

Write-Host "`n=== Maven wrapper ==="
if (Test-Path ".\mvnw.cmd") {
    .\mvnw.cmd -version
} else {
    Write-Warning "mvnw.cmd not found. Run this script from repository root."
}

Write-Host "`n=== Assessment-related files ==="
Get-ChildItem -Recurse -File |
    Where-Object {
        $_.FullName -notmatch '\\target\\|\\.git\\' -and
        $_.Name -match 'grading|assignment|submission|quiz|question|answer|testcase|attempt|result'
    } |
    Select-Object -ExpandProperty FullName

Write-Host "`nPreflight complete. This script does not modify project files."
