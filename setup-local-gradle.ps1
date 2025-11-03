# Setup Local Gradle Properties for Java 17
# This script creates a user-level gradle.properties file
# This won't affect GitHub Actions (which uses its own environment)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  SETUP LOCAL GRADLE PROPERTIES" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$gradleUserHome = "$env:USERPROFILE\.gradle"
$gradlePropsFile = "$gradleUserHome\gradle.properties"

# Create .gradle directory if it doesn't exist
if (-not (Test-Path $gradleUserHome)) {
    New-Item -ItemType Directory -Path $gradleUserHome | Out-Null
    Write-Host "✅ Created .gradle directory" -ForegroundColor Green
}

# Check if Java 17 exists
$javaPath = "C:\Program Files\Java\jdk-17"
if (-not (Test-Path $javaPath)) {
    Write-Host "❌ Java 17 not found at: $javaPath" -ForegroundColor Red
    Write-Host "   Please install Java 17 or update the path in this script" -ForegroundColor Yellow
    exit 1
}

Write-Host "✅ Found Java 17 at: $javaPath" -ForegroundColor Green

# Create/update gradle.properties
$gradleContent = @"
# Local Gradle Properties for Openterface Android
# This file is user-specific and won't affect GitHub Actions

# Java 17 required for AGP 8.1+
org.gradle.java.home=C:\\Program Files\\Java\\jdk-17

# Gradle optimization
org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configureondemand=true

# Android build optimization
android.useAndroidX=true
android.enableJetifier=true
"@

Set-Content -Path $gradlePropsFile -Value $gradleContent

Write-Host "✅ Created user-level gradle.properties at:" -ForegroundColor Green
Write-Host "   $gradlePropsFile" -ForegroundColor Cyan
Write-Host ""
Write-Host "📝 Contents:" -ForegroundColor Yellow
Get-Content $gradlePropsFile | ForEach-Object { Write-Host "   $_" -ForegroundColor Gray }
Write-Host ""
Write-Host "✅ Setup complete! You can now build the project." -ForegroundColor Green
Write-Host "   GitHub Actions will use its own Java setup (not affected)" -ForegroundColor Cyan
