# NVD API Key Setup Script
# This script helps you configure your NVD API key for OWASP dependency checking

Write-Host "🔑 NVD API Key Setup for OWASP Dependency Check" -ForegroundColor Green
Write-Host "=================================================" -ForegroundColor Green

# Check if API key is already configured
$orderServiceKey = $null
$paymentServiceKey = $null

if (Test-Path "orderService\owasp.properties") {
    $orderServiceKey = (Get-Content "orderService\owasp.properties" | Select-String "nvd.api.key" | ForEach-Object { $_.Line.Split('=')[1] }).Trim()
}

if (Test-Path "paymentService\owasp.properties") {
    $paymentServiceKey = (Get-Content "paymentService\owasp.properties" | Select-String "nvd.api.key" | ForEach-Object { $_.Line.Split('=')[1] }).Trim()
}

if ($orderServiceKey -and $orderServiceKey -ne "your-nvd-api-key-here" -and $paymentServiceKey -and $paymentServiceKey -ne "your-nvd-api-key-here") {
    Write-Host "✅ NVD API key is already configured!" -ForegroundColor Green
    Write-Host "OrderService: $orderServiceKey" -ForegroundColor Cyan
    Write-Host "PaymentService: $paymentServiceKey" -ForegroundColor Cyan
    exit 0
}

Write-Host ""
Write-Host "📋 Setup Steps:" -ForegroundColor Yellow
Write-Host "1. Get your free NVD API key from: https://nvd.nist.gov/developers/request-an-api-key" -ForegroundColor White
Write-Host "2. Enter your API key below" -ForegroundColor White
Write-Host "3. The script will configure both services" -ForegroundColor White
Write-Host ""

# Get API key from user
$apiKey = Read-Host "Enter your NVD API key"

if ([string]::IsNullOrWhiteSpace($apiKey)) {
    Write-Host "❌ No API key provided. Exiting..." -ForegroundColor Red
    exit 1
}

# Validate API key format (basic validation)
if ($apiKey.Length -lt 20) {
    Write-Host "⚠️  API key seems too short. Please verify it's correct." -ForegroundColor Yellow
    $confirm = Read-Host "Continue anyway? (y/n)"
    if ($confirm -ne "y" -and $confirm -ne "Y") {
        exit 1
    }
}

Write-Host ""
Write-Host "🔧 Configuring API key..." -ForegroundColor Yellow

# Configure OrderService
Write-Host "📝 Configuring OrderService..." -ForegroundColor Cyan
$orderServiceContent = @"
# OWASP Dependency Check Configuration
# NVD API Key for better performance and rate limiting
# Get your free API key from: https://nvd.nist.gov/developers/request-an-api-key

# Replace 'your-nvd-api-key-here' with your actual NVD API key
nvd.api.key=$apiKey
"@
$orderServiceContent | Out-File -FilePath "orderService\owasp.properties" -Encoding UTF8

# Configure PaymentService
Write-Host "📝 Configuring PaymentService..." -ForegroundColor Cyan
$paymentServiceContent = @"
# OWASP Dependency Check Configuration
# NVD API Key for better performance and rate limiting
# Get your free API key from: https://nvd.nist.gov/developers/request-an-api-key

# Replace 'your-nvd-api-key-here' with your actual NVD API key
nvd.api.key=$apiKey
"@
$paymentServiceContent | Out-File -FilePath "paymentService\owasp.properties" -Encoding UTF8

Write-Host ""
Write-Host "✅ Configuration complete!" -ForegroundColor Green
Write-Host ""
Write-Host "🧪 Testing configuration..." -ForegroundColor Yellow

# Test the configuration
Write-Host "Testing OrderService..." -ForegroundColor Cyan
cd orderService
mvn org.owasp:dependency-check-maven:check -Dnvd.api.key=$apiKey -q
if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ OrderService configuration test passed!" -ForegroundColor Green
} else {
    Write-Host "❌ OrderService configuration test failed!" -ForegroundColor Red
}
cd ..

Write-Host "Testing PaymentService..." -ForegroundColor Cyan
cd paymentService
mvn org.owasp:dependency-check-maven:check -Dnvd.api.key=$apiKey -q
if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ PaymentService configuration test passed!" -ForegroundColor Green
} else {
    Write-Host "❌ PaymentService configuration test failed!" -ForegroundColor Red
}
cd ..

Write-Host ""
Write-Host "🎉 Setup Complete!" -ForegroundColor Green
Write-Host ""
Write-Host "📋 Next Steps:" -ForegroundColor Yellow
Write-Host "1. Run: cd orderService && .\scripts\owasp-check.ps1" -ForegroundColor White
Write-Host "2. Run: cd paymentService && .\scripts\owasp-check.ps1" -ForegroundColor White
Write-Host "3. Check reports in: target\owasp-reports\" -ForegroundColor White
Write-Host ""
Write-Host "🔒 Your NVD API key is now configured for both services!" -ForegroundColor Green
