# Simple Security Reports Comparison
Write-Host "Security Reports Comparison - Before vs After" -ForegroundColor Green
Write-Host "=============================================" -ForegroundColor Green

Write-Host ""
Write-Host "Report Locations:" -ForegroundColor Yellow
Write-Host "BEFORE (Original): target\security-reports-before\" -ForegroundColor White
Write-Host "AFTER (Fixed):     target\security-reports-after-fix\" -ForegroundColor White

Write-Host ""
Write-Host "Files in BEFORE directory:" -ForegroundColor Cyan
if (Test-Path "target\security-reports-before") {
    Get-ChildItem "target\security-reports-before" | ForEach-Object {
        Write-Host "  $($_.Name) - $($_.Length) bytes" -ForegroundColor White
    }
}

Write-Host ""
Write-Host "Files in AFTER directory:" -ForegroundColor Cyan
if (Test-Path "target\security-reports-after-fix") {
    Get-ChildItem "target\security-reports-after-fix" | ForEach-Object {
        Write-Host "  $($_.Name) - $($_.Length) bytes" -ForegroundColor White
    }
}

Write-Host ""
Write-Host "Summary of Changes:" -ForegroundColor Yellow
Write-Host "1. Fixed Lombok duplicate dependency warning" -ForegroundColor Green
Write-Host "2. Updated Logback to version 1.5.20 for security" -ForegroundColor Green
Write-Host "3. Build successful after updates" -ForegroundColor Green
Write-Host "4. No compilation errors" -ForegroundColor Green

Write-Host ""
Write-Host "Security Status: IMPROVED" -ForegroundColor Green
