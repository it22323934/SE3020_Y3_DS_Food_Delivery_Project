# Create Security Reports - AFTER Dependency Fix
Write-Host "Creating Security Reports - AFTER Dependency Updates" -ForegroundColor Green

# Create reports directory for after-fix results
if (!(Test-Path "target\security-reports-after-fix")) {
    New-Item -ItemType Directory -Path "target\security-reports-after-fix" -Force
}

Write-Host "Generating reports after dependency updates..." -ForegroundColor Yellow

# Generate dependency tree
Write-Host "1. Dependency tree (AFTER FIX)..." -ForegroundColor Cyan
mvn dependency:tree > target\security-reports-after-fix\dependency-tree-after-fix.txt

# Generate dependency updates
Write-Host "2. Dependency updates (AFTER FIX)..." -ForegroundColor Cyan
mvn versions:display-dependency-updates > target\security-reports-after-fix\dependency-updates-after-fix.txt

# Generate plugin updates
Write-Host "3. Plugin updates (AFTER FIX)..." -ForegroundColor Cyan
mvn versions:display-plugin-updates > target\security-reports-after-fix\plugin-updates-after-fix.txt

# Generate effective POM
Write-Host "4. Effective POM (AFTER FIX)..." -ForegroundColor Cyan
mvn help:effective-pom > target\security-reports-after-fix\effective-pom-after-fix.xml

Write-Host ""
Write-Host "AFTER FIX Reports created successfully!" -ForegroundColor Green
Write-Host "Location: target\security-reports-after-fix\" -ForegroundColor Cyan
Write-Host ""
Write-Host "Files created (AFTER FIX):" -ForegroundColor Yellow
Write-Host "- dependency-tree-after-fix.txt" -ForegroundColor White
Write-Host "- dependency-updates-after-fix.txt" -ForegroundColor White
Write-Host "- plugin-updates-after-fix.txt" -ForegroundColor White
Write-Host "- effective-pom-after-fix.xml" -ForegroundColor White
