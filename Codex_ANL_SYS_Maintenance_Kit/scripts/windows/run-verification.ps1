$ErrorActionPreference = "Continue"

Write-Host "=== Git status ==="
git status
git branch --show-current
git log -5 --oneline

Write-Host "`n=== Searching ANL/SYS files ==="
$patterns = @(
  "analytics", "dashboard", "revenue", "earning", "income",
  "payment", "order", "transaction", "active student",
  "system setting", "configuration"
)

$sourceRoots = @(
  "src/main/java",
  "src/main/resources/templates",
  "src/main/resources/static",
  "src/test"
)

foreach ($root in $sourceRoots) {
  if (Test-Path $root) {
    foreach ($pattern in $patterns) {
      Get-ChildItem -Path $root -Recurse -File -ErrorAction SilentlyContinue |
        Select-String -Pattern $pattern -SimpleMatch -ErrorAction SilentlyContinue |
        Select-Object Path, LineNumber, Line
    }
  }
}

Write-Host "`n=== Compile ==="
if (Test-Path ".\mvnw.cmd") {
  & .\mvnw.cmd -q -DskipTests compile
  Write-Host "`n=== Tests ==="
  & .\mvnw.cmd test
} elseif (Get-Command mvn -ErrorAction SilentlyContinue) {
  mvn -q -DskipTests compile
  mvn test
} else {
  Write-Warning "Không tìm thấy Maven Wrapper hoặc mvn."
}

Write-Host "`n=== Final git status ==="
git status
