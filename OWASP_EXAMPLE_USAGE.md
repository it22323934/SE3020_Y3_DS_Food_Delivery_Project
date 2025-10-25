# OWASP Dependency Check - Example Usage

## Quick Start Guide

This document provides practical examples of how to use OWASP Dependency Check in the Food Delivery Project.

## Prerequisites

- Java 21
- Maven 3.6+
- Internet connection (for vulnerability database updates)

## Running OWASP Dependency Check

### 1. Basic Usage

#### For OrderService:
```bash
cd orderService
mvn org.owasp:dependency-check-maven:check
```

#### For PaymentService:
```bash
cd paymentService
mvn org.owasp:dependency-check-maven:check
```

### 2. Using PowerShell Scripts (Windows)

#### For OrderService:
```powershell
cd orderService
.\scripts\owasp-check.ps1
```

#### For PaymentService:
```powershell
cd paymentService
.\scripts\owasp-check.ps1
```

### 3. Using Bash Scripts (Linux/Mac)

#### For OrderService:
```bash
cd orderService
chmod +x scripts/owasp-check.sh
./scripts/owasp-check.sh
```

#### For PaymentService:
```bash
cd paymentService
chmod +x scripts/owasp-check.sh
./scripts/owasp-check.sh
```

## Example Output

### Successful Run
```
🔒 Starting OWASP Dependency Check for OrderService...
📊 Running OWASP dependency check...
[INFO] Checking for updates
[INFO] Download Started for NVD CVE - 2024
[INFO] Download Complete for NVD CVE - 2024  (0 ms)
[INFO] Analysis Started
[INFO] Finished File Name: C:\Users\HP\Desktop\excess folder\New folder (4)\SE3020_Y3_DS_Food_Delivery_Project\orderService\target\owasp-reports\dependency-check-report.html
[INFO] Finished File Name: C:\Users\HP\Desktop\excess folder\New folder (4)\SE3020_Y3_DS_Food_Delivery_Project\orderService\target\owasp-reports\dependency-check-report.json
[INFO] Finished File Name: C:\Users\HP\Desktop\excess folder\New folder (4)\SE3020_Y3_DS_Food_Delivery_Project\orderService\target\owasp-reports\dependency-check-report.xml
[INFO] Analysis Complete  (12345 ms)
✅ OWASP dependency check completed successfully!
📁 Reports generated in: target\owasp-reports\
🌐 Open HTML report: target\owasp-reports\dependency-check-report.html
```

### Failed Run (Vulnerabilities Found)
```
🔒 Starting OWASP Dependency Check for OrderService...
📊 Running OWASP dependency check...
[INFO] Checking for updates
[INFO] Analysis Started
[WARN] One or more dependencies were identified with known vulnerabilities:
[WARN] 
[WARN] spring-boot-starter-web-3.4.4.jar (org.springframework.boot:spring-boot-starter-web:3.4.4, cpe:2.3:a:springsource:spring_framework:3.4.4:*:*:*:*:*:*:*) has 1 CVE
[WARN] 
[ERROR] 
[ERROR] One or more dependencies were identified with vulnerabilities that have a CVSS score greater than or equal to '7.0':
[ERROR] 
[ERROR] spring-boot-starter-web-3.4.4.jar (org.springframework.boot:spring-boot-starter-web:3.4.4, cpe:2.3:a:springsource:spring_framework:3.4.4:*:*:*:*:*:*:*) has 1 CVE with a CVSS score greater than or equal to '7.0'
[ERROR] 
[ERROR] See the dependency-check report for more details.
❌ OWASP dependency check found vulnerabilities!
📁 Check reports in: target\owasp-reports\
🔍 Review the HTML report for details
```

## Understanding the Reports

### 1. HTML Report

The HTML report provides a comprehensive view of vulnerabilities:

- **Summary**: Overview of vulnerabilities found
- **Dependencies**: List of all dependencies analyzed
- **Vulnerabilities**: Detailed information about each vulnerability
- **CVSS Scores**: Risk assessment for each vulnerability

### 2. JSON Report

The JSON report is machine-readable and can be integrated with other tools:

```json
{
  "reportSchema": "1.3.0",
  "scanInfo": {
    "engineVersion": "10.0.4",
    "dataSource": {
      "name": "NVD",
      "timestamp": "2024-01-15T10:30:00.000Z"
    }
  },
  "projectInfo": {
    "name": "orderService",
    "reportDate": "2024-01-15T10:30:00.000Z",
    "credits": {
      "tool": "OWASP Dependency Check",
      "url": "https://owasp.org/www-project-dependency-check/"
    }
  },
  "dependencies": [
    {
      "fileName": "spring-boot-starter-web-3.4.4.jar",
      "filePath": "org.springframework.boot:spring-boot-starter-web:3.4.4",
      "md5sum": "abc123...",
      "sha1sum": "def456...",
      "vulnerabilities": [
        {
          "name": "CVE-2023-20863",
          "severity": "HIGH",
          "cvssv3": {
            "baseScore": 7.5,
            "baseSeverity": "HIGH"
          },
          "description": "Spring Framework vulnerability..."
        }
      ]
    }
  ]
}
```

### 3. XML Report

The XML report is useful for integration with enterprise tools:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<analysis xmlns="https://jeremylong.github.io/DependencyCheck/dependency-check.2.0.xsd">
  <scanInfo>
    <engineVersion>10.0.4</engineVersion>
    <dataSource>
      <name>NVD</name>
      <timestamp>2024-01-15T10:30:00.000Z</timestamp>
    </dataSource>
  </scanInfo>
  <projectInfo>
    <name>orderService</name>
    <reportDate>2024-01-15T10:30:00.000Z</reportDate>
  </projectInfo>
  <dependencies>
    <dependency>
      <fileName>spring-boot-starter-web-3.4.4.jar</fileName>
      <filePath>org.springframework.boot:spring-boot-starter-web:3.4.4</filePath>
      <vulnerabilities>
        <vulnerability>
          <name>CVE-2023-20863</name>
          <severity>HIGH</severity>
          <cvssv3>
            <baseScore>7.5</baseScore>
            <baseSeverity>HIGH</baseSeverity>
          </cvssv3>
          <description>Spring Framework vulnerability...</description>
        </vulnerability>
      </vulnerabilities>
    </dependency>
  </dependencies>
</analysis>
```

## Advanced Usage

### 1. Custom Configuration

You can override the default configuration:

```bash
mvn org.owasp:dependency-check-maven:check \
  -Ddependency-check.failBuildOnCVSS=8 \
  -Ddependency-check.suppressionFile=custom-suppressions.xml \
  -Ddependency-check.formats=HTML,JSON
```

### 2. Offline Mode

For air-gapped environments:

```bash
mvn org.owasp:dependency-check-maven:check \
  -Ddependency-check.nvdApiKey= \
  -Ddependency-check.enableNvd=false
```

### 3. Specific Scope

Check only specific dependencies:

```bash
mvn org.owasp:dependency-check-maven:check \
  -Ddependency-check.scope=compile
```

## CI/CD Integration

### 1. GitHub Actions

```yaml
name: Security Check
on: [push, pull_request]
jobs:
  security:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'
      - name: Run OWASP Dependency Check
        run: |
          cd orderService
          mvn org.owasp:dependency-check-maven:check
      - name: Upload Security Report
        uses: actions/upload-artifact@v3
        with:
          name: security-report
          path: orderService/target/owasp-reports/
```

### 2. Jenkins Pipeline

```groovy
pipeline {
    agent any
    stages {
        stage('Security Check') {
            steps {
                sh '''
                    cd orderService
                    mvn org.owasp:dependency-check-maven:check
                '''
            }
        }
    }
    post {
        always {
            publishHTML([
                allowMissing: false,
                alwaysLinkToLastBuild: true,
                keepAll: true,
                reportDir: 'orderService/target/owasp-reports',
                reportFiles: 'dependency-check-report.html',
                reportName: 'OWASP Dependency Check Report'
            ])
        }
    }
}
```

## Troubleshooting

### 1. Common Issues

**Issue**: Build fails with false positives
```bash
# Add suppressions to owasp-suppressions.xml
# Then re-run the check
mvn clean org.owasp:dependency-check-maven:check
```

**Issue**: Slow execution
```bash
# Use NVD API key for better performance
mvn org.owasp:dependency-check-maven:check \
  -Ddependency-check.nvdApiKey=your-api-key
```

**Issue**: Outdated vulnerability data
```bash
# Clear cache and re-run
mvn org.owasp:dependency-check-maven:purge
mvn org.owasp:dependency-check-maven:check
```

### 2. Performance Optimization

- Use NVD API key for better performance
- Run during off-peak hours
- Configure appropriate cache settings
- Use offline mode for air-gapped environments

### 3. Network Issues

- Configure proxy settings if needed
- Set appropriate timeout values
- Use offline mode for air-gapped environments

## Best Practices

### 1. Regular Scanning

- Run dependency checks weekly
- Integrate into CI/CD pipeline
- Monitor for new vulnerabilities

### 2. Vulnerability Management

- Prioritize high and critical severity vulnerabilities
- Update dependencies regularly
- Keep suppression files minimal

### 3. Documentation

- Document all suppressions with clear reasons
- Review suppressions regularly
- Keep track of vulnerability trends

## Security Benefits

### 1. Vulnerability Detection

- Identifies known vulnerabilities in dependencies
- Provides CVSS scores for risk assessment
- Suggests remediation actions

### 2. Compliance

- Helps meet security compliance requirements
- Provides audit trails
- Supports security certifications

### 3. Risk Management

- Prioritizes security fixes
- Reduces attack surface
- Improves overall security posture

## Conclusion

OWASP Dependency Check is an essential tool for maintaining security in the Food Delivery Project. By implementing regular dependency scanning, the project can:

- Identify and remediate vulnerabilities
- Maintain security compliance
- Reduce security risks
- Improve overall security posture

Regular use of this tool, combined with proper vulnerability management practices, will help ensure the security and integrity of the food delivery system.
