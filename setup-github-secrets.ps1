# Quick Setup Script for Your Keystore
# This script will help you test and prepare values for GitHub Secrets

$KeystorePath = "E:\work\secretKey\upload-keystore.jks"
$Alias = "upload"

Write-Host "================================" -ForegroundColor Cyan
Write-Host "Openterface Keystore Setup" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "📁 Keystore: $KeystorePath" -ForegroundColor Green
Write-Host "🔑 Alias: $Alias" -ForegroundColor Green
Write-Host ""

# Check if keystore exists
if (-not (Test-Path $KeystorePath)) {
    Write-Host "❌ Error: Keystore not found at $KeystorePath" -ForegroundColor Red
    exit 1
}

# Get passwords from user
Write-Host "Please enter your credentials:" -ForegroundColor Yellow
Write-Host ""

Write-Host "Store Password (to open keystore): " -ForegroundColor Cyan -NoNewline
$storePassword = Read-Host -AsSecureString
$storePwd = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto([System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($storePassword))

Write-Host "Key Password (for the '$Alias' key): " -ForegroundColor Cyan -NoNewline
$keyPassword = Read-Host -AsSecureString
$keyPwd = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto([System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($keyPassword))

Write-Host ""
Write-Host "================================" -ForegroundColor Cyan
Write-Host "Testing Credentials" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""

# Test 1: Store password
Write-Host "Test 1/3: Testing store password..." -ForegroundColor Yellow
$output1 = keytool -list -keystore $KeystorePath -storepass $storePwd 2>&1

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ FAILED: Store password is incorrect" -ForegroundColor Red
    Write-Host $output1
    exit 1
}
Write-Host "✅ PASSED: Store password is correct" -ForegroundColor Green
Write-Host ""

# Test 2: Alias exists
Write-Host "Test 2/3: Verifying alias '$Alias'..." -ForegroundColor Yellow
$output2 = keytool -list -keystore $KeystorePath -storepass $storePwd -alias $Alias 2>&1

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ FAILED: Alias '$Alias' not found" -ForegroundColor Red
    Write-Host $output2
    exit 1
}
Write-Host "✅ PASSED: Alias exists" -ForegroundColor Green
Write-Host ""

# Test 3: Key password
Write-Host "Test 3/3: Testing key password..." -ForegroundColor Yellow
$output3 = keytool -list -v -keystore $KeystorePath -storepass $storePwd -alias $Alias -keypass $keyPwd 2>&1

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ FAILED: Key password is incorrect" -ForegroundColor Red
    Write-Host ""
    Write-Host "Note: Key password might be the same as store password" -ForegroundColor Yellow
    Write-Host "Try running the script again with the same password for both" -ForegroundColor Yellow
    Write-Host $output3
    exit 1
}
Write-Host "✅ PASSED: Key password is correct" -ForegroundColor Green
Write-Host ""

# Extract SHA fingerprints
Write-Host "================================" -ForegroundColor Cyan
Write-Host "Certificate Information" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""

$sha256Line = $output3 | Select-String -Pattern "SHA256:" | Select-Object -First 1
if ($sha256Line) {
    $sha256 = $sha256Line.ToString().Trim() -replace ".*SHA256:\s*", ""
    Write-Host "SHA256: " -ForegroundColor White -NoNewline
    Write-Host $sha256 -ForegroundColor Yellow
}

$sha1Line = $output3 | Select-String -Pattern "SHA1:" | Select-Object -First 1
if ($sha1Line) {
    $sha1 = $sha1Line.ToString().Trim() -replace ".*SHA1:\s*", ""
    Write-Host "SHA1:   " -ForegroundColor White -NoNewline
    Write-Host $sha1 -ForegroundColor Yellow
}
Write-Host ""

# Generate base64
Write-Host "================================" -ForegroundColor Cyan
Write-Host "Generating Base64 for GitHub" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Encoding keystore to base64..." -ForegroundColor Yellow

$bytes = [IO.File]::ReadAllBytes($KeystorePath)
$base64 = [Convert]::ToBase64String($bytes)
$base64File = "keystore-base64.txt"

# Write without BOM and without extra newlines - critical for Linux base64 -d
[IO.File]::WriteAllText($base64File, $base64, [System.Text.Encoding]::ASCII)

Write-Host "✅ Base64 saved to: $base64File" -ForegroundColor Green
Write-Host "   File size: $($bytes.Length) bytes (original)" -ForegroundColor Gray
Write-Host "   Base64 length: $($base64.Length) characters" -ForegroundColor Gray
Write-Host ""

# Verify the encoding
Write-Host "Verifying base64 encoding..." -ForegroundColor Yellow
try {
    $testDecode = [Convert]::FromBase64String($base64)
    if ($testDecode.Length -eq $bytes.Length) {
        Write-Host "✅ Base64 encoding verified (can be decoded successfully)" -ForegroundColor Green
        Write-Host "   Decoded size: $($testDecode.Length) bytes" -ForegroundColor Gray
    } else {
        Write-Host "⚠️  Warning: Base64 verification size mismatch" -ForegroundColor Red
        Write-Host "   Original: $($bytes.Length) bytes" -ForegroundColor Gray
        Write-Host "   Decoded: $($testDecode.Length) bytes" -ForegroundColor Gray
    }
} catch {
    Write-Host "⚠️  Warning: Could not verify base64 encoding" -ForegroundColor Red
}
Write-Host ""

# Success!
Write-Host "================================" -ForegroundColor Green
Write-Host "🎉 ALL TESTS PASSED!" -ForegroundColor Green
Write-Host "================================" -ForegroundColor Green
Write-Host ""

Write-Host "Your GitHub Secrets values:" -ForegroundColor Cyan
Write-Host ""
Write-Host "┌─────────────────────────────────────────┐" -ForegroundColor Gray
Write-Host "│ ALIAS                                   │" -ForegroundColor Gray
Write-Host "└─────────────────────────────────────────┘" -ForegroundColor Gray
Write-Host $Alias -ForegroundColor Yellow
Write-Host ""

Write-Host "┌─────────────────────────────────────────┐" -ForegroundColor Gray
Write-Host "│ KEY_STORE_PASSWORD                      │" -ForegroundColor Gray
Write-Host "└─────────────────────────────────────────┘" -ForegroundColor Gray
Write-Host $storePwd -ForegroundColor Yellow
Write-Host ""

Write-Host "┌─────────────────────────────────────────┐" -ForegroundColor Gray
Write-Host "│ KEY_PASSWORD                            │" -ForegroundColor Gray
Write-Host "└─────────────────────────────────────────┘" -ForegroundColor Gray
Write-Host $keyPwd -ForegroundColor Yellow
Write-Host ""

Write-Host "┌─────────────────────────────────────────┐" -ForegroundColor Gray
Write-Host "│ SIGNING_KEY                             │" -ForegroundColor Gray
Write-Host "└─────────────────────────────────────────┘" -ForegroundColor Gray
Write-Host "(See content in $base64File)" -ForegroundColor Yellow
Write-Host "First 80 chars: $($base64.Substring(0, [Math]::Min(80, $base64.Length)))..." -ForegroundColor Gray
Write-Host ""

Write-Host "================================" -ForegroundColor Cyan
Write-Host "Next Steps" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "1. Go to GitHub Secrets:" -ForegroundColor White
Write-Host "   https://github.com/TechxArtisanStudio/Openterface_Android/settings/secrets/actions" -ForegroundColor Cyan
Write-Host ""
Write-Host "2. Update these 4 secrets:" -ForegroundColor White
Write-Host "   • ALIAS = upload" -ForegroundColor Green
Write-Host "   • KEY_STORE_PASSWORD = (copy from above)" -ForegroundColor Green
Write-Host "   • KEY_PASSWORD = (copy from above)" -ForegroundColor Green
Write-Host "   • SIGNING_KEY = (copy entire content from $base64File)" -ForegroundColor Green
Write-Host ""
Write-Host "3. IMPORTANT - Copy the ENTIRE base64 string:" -ForegroundColor Red
Write-Host "   - Open $base64File in Notepad" -ForegroundColor Yellow
Write-Host "   - Select All (Ctrl+A)" -ForegroundColor Yellow
Write-Host "   - Copy (Ctrl+C)" -ForegroundColor Yellow
Write-Host "   - Paste into SIGNING_KEY secret" -ForegroundColor Yellow
Write-Host "   - Make sure NO extra spaces or newlines!" -ForegroundColor Yellow
Write-Host ""
Write-Host "4. Delete the base64 file after updating:" -ForegroundColor White
Write-Host "   Remove-Item $base64File" -ForegroundColor Yellow
Write-Host ""
Write-Host "5. Run GitHub Actions workflow to test!" -ForegroundColor White
Write-Host ""
Write-Host "⚠️  IMPORTANT: Keep your passwords secure!" -ForegroundColor Red
Write-Host "    Close this window after copying the values" -ForegroundColor Red
Write-Host ""
