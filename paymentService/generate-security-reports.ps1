# Generate Security Reports - OWASP Alternative
Write-Host "Generating Security Reports for PaymentService" -ForegroundColor Green
Write-Host "===============================================" -ForegroundColor Green

# Create reports directory
if (!(Test-Path "target\security-reports")) {
    New-Item -ItemType Directory -Path "target\security-reports" -Force
}

Write-Host "Creating comprehensive security reports..." -ForegroundColor Yellow

# Generate dependency tree report
Write-Host ""
Write-Host "1. Generating dependency tree report..." -ForegroundColor Cyan
mvn dependency:tree > target\security-reports\dependency-tree.txt
Write-Host "   Saved: target\security-reports\dependency-tree.txt" -ForegroundColor Green

# Generate dependency updates report
Write-Host ""
Write-Host "2. Generating dependency updates report..." -ForegroundColor Cyan
mvn versions:display-dependency-updates > target\security-reports\dependency-updates.txt
Write-Host "   Saved: target\security-reports\dependency-updates.txt" -ForegroundColor Green

# Generate plugin updates report
Write-Host ""
Write-Host "3. Generating plugin updates report..." -ForegroundColor Cyan
mvn versions:display-plugin-updates > target\security-reports\plugin-updates.txt
Write-Host "   Saved: target\security-reports\plugin-updates.txt" -ForegroundColor Green

# Generate effective POM
Write-Host ""
Write-Host "4. Generating effective POM..." -ForegroundColor Cyan
mvn help:effective-pom > target\security-reports\effective-pom.xml
Write-Host "   Saved: target\security-reports\effective-pom.xml" -ForegroundColor Green

# Generate project info
Write-Host ""
Write-Host "5. Generating project information..." -ForegroundColor Cyan
mvn help:describe -Dplugin=org.owasp:dependency-check-maven > target\security-reports\owasp-plugin-info.txt
Write-Host "   Saved: target\security-reports\owasp-plugin-info.txt" -ForegroundColor Green

# Create HTML summary report
Write-Host ""
Write-Host "6. Creating HTML security report..." -ForegroundColor Cyan

$htmlReport = @"
<!DOCTYPE html>
<html>
<head>
    <title>Security Report - PaymentService</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .header { background-color: #f0f0f0; padding: 20px; border-radius: 5px; }
        .section { margin: 20px 0; }
        .file-link { color: #0066cc; text-decoration: none; }
        .file-link:hover { text-decoration: underline; }
        .status { padding: 5px 10px; border-radius: 3px; }
        .success { background-color: #d4edda; color: #155724; }
        .warning { background-color: #fff3cd; color: #856404; }
        .info { background-color: #d1ecf1; color: #0c5460; }
    </style>
</head>
<body>
    <div class="header">
        <h1>🔒 Security Report - PaymentService</h1>
        <p>Generated on: $(Get-Date)</p>
    </div>
    
    <div class="section">
        <h2>📊 Generated Reports</h2>
        <ul>
            <li><a href="dependency-tree.txt" class="file-link">Dependency Tree</a> - Complete dependency hierarchy</li>
            <li><a href="dependency-updates.txt" class="file-link">Dependency Updates</a> - Available updates for dependencies</li>
            <li><a href="plugin-updates.txt" class="file-link">Plugin Updates</a> - Available updates for Maven plugins</li>
            <li><a href="effective-pom.xml" class="file-link">Effective POM</a> - Resolved Maven configuration</li>
            <li><a href="owasp-plugin-info.txt" class="file-link">OWASP Plugin Info</a> - OWASP dependency check plugin details</li>
        </ul>
    </div>
    
    <div class="section">
        <h2>⚠️ Security Status</h2>
        <p class="status info">OWASP NVD API currently has known issues affecting vulnerability scanning</p>
        <p class="status warning">Alternative security methods used to generate reports</p>
    </div>
    
    <div class="section">
        <h2>🔧 Recommended Actions</h2>
        <ol>
            <li>Review dependency updates in <a href="dependency-updates.txt" class="file-link">dependency-updates.txt</a></li>
            <li>Update outdated dependencies to latest secure versions</li>
            <li>Use Snyk for comprehensive vulnerability scanning: <code>npm install -g snyk && snyk test</code></li>
            <li>Enable GitHub Dependabot for automatic security updates</li>
        </ol>
    </div>
    
    <div class="section">
        <h2>📋 Next Steps</h2>
        <ul>
            <li>Check for high-severity vulnerabilities in dependency updates</li>
            <li>Update Spring Boot, Jackson, Stripe, and other critical dependencies</li>
            <li>Monitor security advisories for your dependencies</li>
            <li>Set up automated security scanning in CI/CD pipeline</li>
        </ul>
    </div>
</body>
</html>
"@

$htmlReport | Out-File -FilePath "target\security-reports\security-report.html" -Encoding UTF8
Write-Host "   Saved: target\security-reports\security-report.html" -ForegroundColor Green

Write-Host ""
Write-Host "📋 Security Reports Generated Successfully!" -ForegroundColor Green
Write-Host ""
Write-Host "📁 Reports Location: target\security-reports\" -ForegroundColor Cyan
Write-Host "🌐 Open HTML Report: target\security-reports\security-report.html" -ForegroundColor Cyan
Write-Host ""
Write-Host "📊 Generated Files:" -ForegroundColor Yellow
Write-Host "- dependency-tree.txt" -ForegroundColor White
Write-Host "- dependency-updates.txt" -ForegroundColor White
Write-Host "- plugin-updates.txt" -ForegroundColor White
Write-Host "- effective-pom.xml" -ForegroundColor White
Write-Host "- owasp-plugin-info.txt" -ForegroundColor White
Write-Host "- security-report.html" -ForegroundColor White
