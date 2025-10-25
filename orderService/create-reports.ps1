# Create Security Reports
Write-Host "Creating Security Reports for OrderService" -ForegroundColor Green

# Create reports directory
if (!(Test-Path "target\security-reports")) {
    New-Item -ItemType Directory -Path "target\security-reports" -Force
}

Write-Host "Generating reports..." -ForegroundColor Yellow

# Generate dependency tree
Write-Host "1. Dependency tree..." -ForegroundColor Cyan
mvn dependency:tree > target\security-reports\dependency-tree.txt

# Generate dependency updates
Write-Host "2. Dependency updates..." -ForegroundColor Cyan
mvn versions:display-dependency-updates > target\security-reports\dependency-updates.txt

# Generate plugin updates
Write-Host "3. Plugin updates..." -ForegroundColor Cyan
mvn versions:display-plugin-updates > target\security-reports\plugin-updates.txt

# Generate effective POM
Write-Host "4. Effective POM..." -ForegroundColor Cyan
mvn help:effective-pom > target\security-reports\effective-pom.xml

Write-Host ""
Write-Host "Reports created successfully!" -ForegroundColor Green
Write-Host "Location: target\security-reports\" -ForegroundColor Cyan
Write-Host ""
Write-Host "Files created:" -ForegroundColor Yellow
Write-Host "- dependency-tree.txt" -ForegroundColor White
Write-Host "- dependency-updates.txt" -ForegroundColor White
Write-Host "- plugin-updates.txt" -ForegroundColor White
Write-Host "- effective-pom.xml" -ForegroundColor White
