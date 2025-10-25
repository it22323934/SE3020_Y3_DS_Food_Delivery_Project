# Create Security Reports - BEFORE Dependency Updates
Write-Host "Creating Security Reports for PaymentService - BEFORE Updates" -ForegroundColor Green
Write-Host "=============================================================" -ForegroundColor Green

# Create reports directory
if (!(Test-Path "target\security-reports-before")) {
    New-Item -ItemType Directory -Path "target\security-reports-before" -Force
}

Write-Host "Generating BEFORE reports..." -ForegroundColor Yellow

# Generate dependency tree
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
Write-Host "BEFORE reports created successfully!" -ForegroundColor Green
Write-Host "Location: target\security-reports-before\" -ForegroundColor Cyan
Write-Host ""
Write-Host "Files created (BEFORE):" -ForegroundColor Yellow
Write-Host "- dependency-tree-before.txt" -ForegroundColor White
Write-Host "- dependency-updates-before.txt" -ForegroundColor White
Write-Host "- plugin-updates-before.txt" -ForegroundColor White
Write-Host "- effective-pom-before.xml" -ForegroundColor White
