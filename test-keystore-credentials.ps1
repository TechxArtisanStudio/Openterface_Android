# 测试密钥库凭据脚本
# 用于验证密钥库文件、别名和密码是否匹配

param(
    [Parameter(Mandatory=$false)]
    [string]$KeystorePath = "app\keystore.jks"
)

Write-Host "================================" -ForegroundColor Cyan
Write-Host "密钥库凭据测试工具" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""

# 检查文件是否存在
if (-not (Test-Path $KeystorePath)) {
    Write-Host "❌ 错误: 找不到密钥库文件: $KeystorePath" -ForegroundColor Red
    exit 1
}

Write-Host "📁 密钥库文件: $KeystorePath" -ForegroundColor Green
Write-Host ""

# 步骤 1: 列出所有别名
Write-Host "步骤 1: 列出密钥库中的所有别名" -ForegroundColor Yellow
Write-Host "请输入密钥库密码 (store password): " -ForegroundColor Cyan -NoNewline
$storePassword = Read-Host -AsSecureString
$storePwd = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto([System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($storePassword))

Write-Host ""
Write-Host "正在列出别名..." -ForegroundColor Gray

$listOutput = keytool -list -keystore $KeystorePath -storepass $storePwd 2>&1

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "❌ 错误: 无法访问密钥库。密钥库密码可能不正确。" -ForegroundColor Red
    Write-Host ""
    Write-Host "错误信息:" -ForegroundColor Yellow
    Write-Host $listOutput
    exit 1
}

Write-Host ""
Write-Host "✅ 密钥库密码正确！" -ForegroundColor Green
Write-Host ""
Write-Host "密钥库中的别名:" -ForegroundColor Cyan
Write-Host $listOutput
Write-Host ""

# 步骤 2: 测试特定别名和密钥密码
Write-Host "================================" -ForegroundColor Cyan
Write-Host "步骤 2: 测试别名和密钥密码" -ForegroundColor Yellow
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "请输入要测试的别名 (alias): " -ForegroundColor Cyan -NoNewline
$alias = Read-Host

Write-Host "请输入密钥密码 (key password): " -ForegroundColor Cyan -NoNewline
$keyPassword = Read-Host -AsSecureString
$keyPwd = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto([System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($keyPassword))

Write-Host ""
Write-Host "正在验证别名和密钥密码..." -ForegroundColor Gray

$detailOutput = keytool -list -v -keystore $KeystorePath -alias $alias -storepass $storePwd -keypass $keyPwd 2>&1

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "❌ 错误: 无法访问密钥。可能的原因:" -ForegroundColor Red
    Write-Host "  1. 别名 (alias) 不正确" -ForegroundColor Yellow
    Write-Host "  2. 密钥密码 (key password) 不正确" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "错误信息:" -ForegroundColor Yellow
    Write-Host $detailOutput
    exit 1
}

Write-Host ""
Write-Host "✅ 所有凭据都正确！" -ForegroundColor Green
Write-Host ""

# 提取 SHA1 指纹
$sha1Line = $detailOutput | Select-String -Pattern "SHA1:" | Select-Object -First 1
if ($sha1Line) {
    $currentSHA1 = $sha1Line.ToString().Trim() -replace ".*SHA1:\s*", ""
    Write-Host "================================" -ForegroundColor Cyan
    Write-Host "密钥信息" -ForegroundColor Cyan
    Write-Host "================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "SHA1 指纹: " -ForegroundColor White -NoNewline
    Write-Host $currentSHA1 -ForegroundColor Yellow
    Write-Host ""
}

# 显示完整信息
Write-Host "完整的证书信息:" -ForegroundColor Cyan
Write-Host $detailOutput
Write-Host ""

# 步骤 3: 生成 GitHub Secrets 的值
Write-Host "================================" -ForegroundColor Cyan
Write-Host "GitHub Secrets 配置" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "请确认 GitHub Secrets 中的配置:" -ForegroundColor Yellow
Write-Host ""
Write-Host "ALIAS = " -ForegroundColor White -NoNewline
Write-Host $alias -ForegroundColor Green
Write-Host "KEY_STORE_PASSWORD = " -ForegroundColor White -NoNewline
Write-Host $storePwd -ForegroundColor Green
Write-Host "KEY_PASSWORD = " -ForegroundColor White -NoNewline
Write-Host $keyPwd -ForegroundColor Green
Write-Host ""

# 生成 Base64
Write-Host "是否要生成 SIGNING_KEY 的 Base64 编码? (Y/N): " -ForegroundColor Cyan -NoNewline
$generateBase64 = Read-Host

if ($generateBase64 -eq "Y" -or $generateBase64 -eq "y") {
    Write-Host ""
    Write-Host "正在生成 Base64..." -ForegroundColor Yellow
    $base64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes($KeystorePath))
    $outputFile = "keystore-base64.txt"
    $base64 | Out-File -FilePath $outputFile -Encoding ASCII -NoNewline
    
    Write-Host "✅ Base64 已保存到: $outputFile" -ForegroundColor Green
    Write-Host ""
    Write-Host "⚠️  请记得在上传到 GitHub 后删除此文件！" -ForegroundColor Red
}

Write-Host ""
Write-Host "================================" -ForegroundColor Cyan
Write-Host "下一步操作" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "1. 打开 GitHub: https://github.com/TechxArtisanStudio/Openterface_Android/settings/secrets/actions" -ForegroundColor White
Write-Host ""
Write-Host "2. 更新以下 Secrets:" -ForegroundColor White
Write-Host "   - SIGNING_KEY: 上传 keystore-base64.txt 的内容" -ForegroundColor Gray
Write-Host "   - KEY_STORE_PASSWORD: $storePwd" -ForegroundColor Gray
Write-Host "   - ALIAS: $alias" -ForegroundColor Gray
Write-Host "   - KEY_PASSWORD: $keyPwd" -ForegroundColor Gray
Write-Host ""
Write-Host "3. 提交并推送代码触发新的构建" -ForegroundColor White
Write-Host ""
Write-Host "✅ 完成！" -ForegroundColor Green
