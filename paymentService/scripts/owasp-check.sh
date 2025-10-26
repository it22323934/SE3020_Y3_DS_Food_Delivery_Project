#!/bin/bash

# OWASP Dependency Check Script for PaymentService
# This script runs OWASP dependency check and generates security reports

set -e

echo "🔒 Starting OWASP Dependency Check for PaymentService..."

# Change to the paymentService directory
cd "$(dirname "$0")/.."

# Create reports directory if it doesn't exist
mkdir -p target/owasp-reports

# Run OWASP dependency check
echo "📊 Running OWASP dependency check..."
mvn org.owasp:dependency-check-maven:check

# Check if the check was successful
if [ $? -eq 0 ]; then
    echo "✅ OWASP dependency check completed successfully!"
    echo "📁 Reports generated in: target/owasp-reports/"
    echo "🌐 Open HTML report: target/owasp-reports/dependency-check-report.html"
else
    echo "❌ OWASP dependency check found vulnerabilities!"
    echo "📁 Check reports in: target/owasp-reports/"
    echo "🔍 Review the HTML report for details"
    exit 1
fi

# Display summary
echo ""
echo "📋 Summary:"
echo "- HTML Report: target/owasp-reports/dependency-check-report.html"
echo "- JSON Report: target/owasp-reports/dependency-check-report.json"
echo "- XML Report: target/owasp-reports/dependency-check-report.xml"
echo ""
echo "🔒 Security Check Complete!"
