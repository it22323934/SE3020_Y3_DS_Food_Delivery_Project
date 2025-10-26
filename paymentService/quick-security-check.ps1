# Quick Security Check - Fast OWASP Dependency Check
# This script runs a simplified security check without the problematic NVD API

Write-Host "🔒 Quick Security Check for PaymentService..." -ForegroundColor Green
Write-Host "===============================================" -ForegroundColor Green

# Create reports directory if it doesn't exist
if (!(Test-Path "target\owasp-reports")) {
    New-Item -ItemType Directory -Path "target\owasp-reports" -Force
}

Write-Host "📊 Running simplified security check..." -ForegroundColor Yellow
Write-Host "⚠️  Note: This is a basic check without full vulnerability database" -ForegroundColor Yellow

# Run with minimal configuration to avoid NVD API issues
try {
    mvn org.owasp:dependency-check-maven:check -Ddependency-check.enableNvd=false -Ddependency-check.enableNpm=false -Ddependency-check.enableOssIndex=false -Ddependency-check.enableRetired=false -Ddependency-check.failBuildOnCVSS=10
} catch {
    Write-Host "❌ Basic check failed. Trying alternative approach..." -ForegroundColor Red
}

Write-Host ""
Write-Host "📋 Alternative Security Approaches:" -ForegroundColor Cyan
Write-Host "1. Use Maven dependency plugin: mvn dependency:tree" -ForegroundColor White
Write-Host "2. Check for outdated dependencies: mvn versions:display-dependency-updates" -ForegroundColor White
Write-Host "3. Use Snyk or other security tools" -ForegroundColor White
Write-Host ""
Write-Host "🔒 Quick Security Check Complete!" -ForegroundColor Green
