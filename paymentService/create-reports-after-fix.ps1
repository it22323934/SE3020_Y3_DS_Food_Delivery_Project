# Create Security Reports for PaymentService - AFTER Dependency Updates
# This script generates security reports after dependency updates

Write-Host "Creating Security Reports for PaymentService - AFTER Updates"
Write-Host "============================================================"
Write-Host ""

# Create target directory for after-fix reports
$targetDir = "target\security-reports-after-fix"
if (!(Test-Path $targetDir)) {
    New-Item -ItemType Directory -Path $targetDir -Force
    Write-Host "Created directory: $targetDir"
}

Write-Host "Generating AFTER reports..."
Write-Host "1. Dependency tree (AFTER)..."
mvn dependency:tree | Out-File -FilePath "$targetDir\dependency-tree-after-fix.txt"

Write-Host "2. Dependency updates (AFTER)..."
mvn versions:display-dependency-updates | Out-File -FilePath "$targetDir\dependency-updates-after-fix.txt"

Write-Host "3. Plugin updates (AFTER)..."
mvn versions:display-plugin-updates | Out-File -FilePath "$targetDir\plugin-updates-after-fix.txt"

Write-Host "4. Effective POM (AFTER)..."
mvn help:effective-pom | Out-File -FilePath "$targetDir\effective-pom-after-fix.xml"

Write-Host ""
Write-Host "AFTER reports created successfully!"
Write-Host "Location: $targetDir\"
Write-Host ""
Write-Host "Files created (AFTER):"
Write-Host "- dependency-tree-after-fix.txt"
Write-Host "- dependency-updates-after-fix.txt"
Write-Host "- plugin-updates-after-fix.txt"
Write-Host "- effective-pom-after-fix.xml"
