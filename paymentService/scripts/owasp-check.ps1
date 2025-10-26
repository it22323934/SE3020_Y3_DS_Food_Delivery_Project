# OWASP Dependency Check Script for PaymentService (PowerShell)
# This script runs OWASP dependency check and generates security reports

Write-Host "🔒 Starting OWASP Dependency Check for PaymentService..." -ForegroundColor Green

# Change to the paymentService directory
Set-Location $PSScriptRoot\..

# Create reports directory if it doesn't exist
if (!(Test-Path "target\owasp-reports")) {
    New-Item -ItemType Directory -Path "target\owasp-reports" -Force
}

# Run OWASP dependency check
Write-Host "📊 Running OWASP dependency check..." -ForegroundColor Yellow
Write-Host "🔑 Using NVD API key for better performance..." -ForegroundColor Green

# Run with the hardcoded API key to avoid PowerShell parsing issues
mvn org.owasp:dependency-check-maven:check

# Check if the check was successful
if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ OWASP dependency check completed successfully!" -ForegroundColor Green
    Write-Host "📁 Reports generated in: target\owasp-reports\" -ForegroundColor Cyan
    Write-Host "🌐 Open HTML report: target\owasp-reports\dependency-check-report.html" -ForegroundColor Cyan
} else {
    Write-Host "❌ OWASP dependency check found vulnerabilities!" -ForegroundColor Red
    Write-Host "📁 Check reports in: target\owasp-reports\" -ForegroundColor Yellow
    Write-Host "🔍 Review the HTML report for details" -ForegroundColor Yellow
    exit 1
}

# Display summary
Write-Host ""
Write-Host "📋 Summary:" -ForegroundColor Cyan
Write-Host "- HTML Report: target\owasp-reports\dependency-check-report.html" -ForegroundColor White
Write-Host "- JSON Report: target\owasp-reports\dependency-check-report.json" -ForegroundColor White
Write-Host "- XML Report: target\owasp-reports\dependency-check-report.xml" -ForegroundColor White
Write-Host ""
Write-Host "🔒 Security Check Complete!" -ForegroundColor Green
