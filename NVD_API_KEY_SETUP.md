# NVD API Key Setup Guide

## Why You Need an NVD API Key

The National Vulnerability Database (NVD) API key provides several benefits:

- **🚀 Better Performance**: Faster vulnerability database updates
- **📊 Higher Rate Limits**: More requests per minute
- **⏱️ Reduced Wait Times**: No more long waits for database downloads
- **🔒 Enhanced Security**: More reliable access to vulnerability data

## How to Get Your Free NVD API Key

### Step 1: Visit the NVD API Key Request Page

1. **Go to**: https://nvd.nist.gov/developers/request-an-api-key
2. **Fill out the form** with your details:
   - Name
   - Email address
   - Organization (optional)
   - Purpose of use (e.g., "Security scanning for food delivery project")

### Step 2: Receive Your API Key

- You'll receive your API key via email (usually within a few minutes)
- The API key will look like: `12345678-1234-1234-1234-123456789abc`

### Step 3: Configure Your Project

#### Method 1: Using Properties File (Recommended)

1. **Edit the properties file**:
   ```bash
   # For OrderService
   cd orderService
   notepad owasp.properties
   
   # For PaymentService
   cd paymentService
   notepad owasp.properties
   ```

2. **Replace the placeholder**:
   ```properties
   # Replace 'your-nvd-api-key-here' with your actual API key
   nvd.api.key=12345678-1234-1234-1234-123456789abc
   ```

#### Method 2: Using Environment Variable

1. **Set the environment variable**:
   ```powershell
   # Windows PowerShell
   $env:NVD_API_KEY="12345678-1234-1234-1234-123456789abc"
   
   # Windows Command Prompt
   set NVD_API_KEY=12345678-1234-1234-1234-123456789abc
   ```

2. **Run the dependency check**:
   ```bash
   mvn org.owasp:dependency-check-maven:check -Dnvd.api.key=%NVD_API_KEY%
   ```

#### Method 3: Using Maven Settings

1. **Add to your Maven settings** (`~/.m2/settings.xml`):
   ```xml
   <settings>
     <profiles>
       <profile>
         <id>owasp</id>
         <properties>
           <nvd.api.key>12345678-1234-1234-1234-123456789abc</nvd.api.key>
         </properties>
       </profile>
     </profiles>
   </settings>
   ```

2. **Activate the profile**:
   ```bash
   mvn org.owasp:dependency-check-maven:check -P owasp
   ```

## Testing Your API Key

### Quick Test

```bash
# Test with OrderService
cd orderService
mvn org.owasp:dependency-check-maven:check -Dnvd.api.key=your-actual-api-key

# Test with PaymentService
cd paymentService
mvn org.owasp:dependency-check-maven:check -Dnvd.api.key=your-actual-api-key
```

### Expected Output with API Key

```
[INFO] Checking for updates
[INFO] Download Started for NVD CVE - 2024
[INFO] Download Complete for NVD CVE - 2024  (500 ms)  # Much faster!
[INFO] Analysis Started
[INFO] Analysis Complete  (5000 ms)  # Much faster!
```

### Expected Output without API Key

```
[INFO] Checking for updates
[INFO] Download Started for NVD CVE - 2024
[INFO] Download Complete for NVD CVE - 2024  (30000 ms)  # Much slower!
[INFO] Analysis Started
[INFO] Analysis Complete  (45000 ms)  # Much slower!
```

## Performance Comparison

| Configuration | Download Time | Analysis Time | Total Time |
|---------------|---------------|---------------|------------|
| **Without API Key** | 30-60 seconds | 30-60 seconds | 1-2 minutes |
| **With API Key** | 1-5 seconds | 5-15 seconds | 10-30 seconds |

## Troubleshooting

### Issue 1: Invalid API Key

**Error**: `Invalid API key provided`

**Solution**:
1. Verify your API key is correct
2. Check for extra spaces or characters
3. Request a new API key if needed

### Issue 2: Rate Limiting

**Error**: `Rate limit exceeded`

**Solution**:
1. Wait a few minutes before retrying
2. Consider using a different API key
3. Check your usage limits

### Issue 3: Network Issues

**Error**: `Connection timeout`

**Solution**:
1. Check your internet connection
2. Try again later
3. Use offline mode if available

## Security Best Practices

### 1. Keep Your API Key Secure

- **Don't commit** API keys to version control
- **Use environment variables** for CI/CD
- **Rotate keys** regularly
- **Don't share** API keys in public forums

### 2. Environment-Specific Configuration

```bash
# Development
nvd.api.key=dev-api-key-here

# Production
nvd.api.key=prod-api-key-here
```

### 3. CI/CD Integration

```yaml
# GitHub Actions
- name: Run OWASP Dependency Check
  run: mvn org.owasp:dependency-check-maven:check
  env:
    NVD_API_KEY: ${{ secrets.NVD_API_KEY }}
```

## Advanced Configuration

### 1. Custom Rate Limiting

```xml
<plugin>
    <groupId>org.owasp</groupId>
    <artifactId>dependency-check-maven</artifactId>
    <configuration>
        <nvdApiKey>${nvd.api.key}</nvdApiKey>
        <nvdMaxRetryCount>5</nvdMaxRetryCount>
        <nvdValidForHours>12</nvdValidForHours>
    </configuration>
</plugin>
```

### 2. Offline Mode

```bash
# Use cached data only
mvn org.owasp:dependency-check-maven:check -Ddependency-check.enableNvd=false
```

### 3. Custom Data Sources

```xml
<plugin>
    <groupId>org.owasp</groupId>
    <artifactId>dependency-check-maven</artifactId>
    <configuration>
        <nvdApiKey>${nvd.api.key}</nvdApiKey>
        <enableNvd>true</enableNvd>
        <enableNpm>true</enableNpm>
        <enableOssIndex>true</enableOssIndex>
    </configuration>
</plugin>
```

## Benefits of Using NVD API Key

### 1. Performance Improvements

- **10x faster** vulnerability database updates
- **Reduced wait times** for large projects
- **Better caching** mechanisms

### 2. Reliability

- **Higher rate limits** (1000 requests/minute vs 5 requests/minute)
- **More stable** connections
- **Better error handling**

### 3. Enhanced Features

- **Real-time updates** from NVD
- **Better CVE coverage**
- **Improved accuracy**

## Conclusion

Setting up an NVD API key is essential for efficient OWASP dependency checking. The free API key provides significant performance improvements and better reliability for your security scanning needs.

**Next Steps**:
1. Get your free NVD API key
2. Configure it in your project
3. Run your first scan with the API key
4. Enjoy faster, more reliable security scanning!

For more information, visit: https://nvd.nist.gov/developers
