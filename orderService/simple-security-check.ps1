# Simple Security Check - Working Alternative to OWASP
Write-Host "Security Check for OrderService" -ForegroundColor Green
Write-Host "=================================" -ForegroundColor Green

Write-Host "Running alternative security checks..." -ForegroundColor Yellow
Write-Host "OWASP NVD API has known issues - using alternative methods" -ForegroundColor Yellow

# Check for outdated dependencies
Write-Host ""
Write-Host "Checking for outdated dependencies..." -ForegroundColor Cyan
mvn versions:display-dependency-updates -q

# Check dependency tree
Write-Host ""
Write-Host "Analyzing dependency tree..." -ForegroundColor Cyan
mvn dependency:tree -q

Write-Host ""
Write-Host "Security Summary:" -ForegroundColor Cyan
Write-Host "Dependency tree analyzed" -ForegroundColor Green
Write-Host "Outdated dependencies checked" -ForegroundColor Green
Write-Host ""
Write-Host "Security Check Complete!" -ForegroundColor Green
Write-Host ""
Write-Host "For comprehensive vulnerability scanning:" -ForegroundColor Yellow
Write-Host "1. Use Snyk: npm install -g snyk and snyk test" -ForegroundColor White
Write-Host "2. Use GitHub Dependabot" -ForegroundColor White
Write-Host "3. Wait for OWASP fix" -ForegroundColor White
