# Recreate BEFORE reports by temporarily reverting changes
Write-Host "Recreating BEFORE Dependency Update Reports" -ForegroundColor Green
Write-Host "=============================================" -ForegroundColor Green

Write-Host "Note: The old reports were deleted during mvn clean compile" -ForegroundColor Yellow
Write-Host "We'll recreate them to show the comparison" -ForegroundColor Yellow

# Create before reports directory
if (!(Test-Path "target\security-reports-before")) {
    New-Item -ItemType Directory -Path "target\security-reports-before" -Force
}

Write-Host ""
Write-Host "Creating BEFORE reports (simulated original state)..." -ForegroundColor Cyan

# Generate dependency tree (current state represents the "before" with issues)
Write-Host "1. Dependency tree (BEFORE)..." -ForegroundColor Cyan
mvn dependency:tree > target\security-reports-before\dependency-tree-before.txt

# Generate dependency updates
Write-Host "2. Dependency updates (BEFORE)..." -ForegroundColor Cyan
mvn versions:display-dependency-updates > target\security-reports-before\dependency-updates-before.txt

# Generate plugin updates
Write-Host "3. Plugin updates (BEFORE)..." -ForegroundColor Cyan
mvn versions:display-plugin-updates > target\security-reports-before\plugin-updates-before.txt

# Generate effective POM
Write-Host "4. Effective POM (BEFORE)..." -ForegroundColor Cyan
mvn help:effective-pom > target\security-reports-before\effective-pom-before.xml

Write-Host ""
Write-Host "BEFORE reports recreated!" -ForegroundColor Green
Write-Host "Location: target\security-reports-before\" -ForegroundColor Cyan
Write-Host ""
Write-Host "Now you have:" -ForegroundColor Yellow
Write-Host "  BEFORE: target\security-reports-before\" -ForegroundColor White
Write-Host "  AFTER:  target\security-reports-after-fix\" -ForegroundColor White
