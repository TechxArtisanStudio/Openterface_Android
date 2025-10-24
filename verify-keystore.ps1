# 验证密钥库指纹脚本
# 用于确认哪个密钥库是正确的

param(
    [Parameter(Mandatory=$false)]
    [string]$KeystorePath = "keystore.jks",
    
    [Parameter(Mandatory=$false)]
    [string]$Alias = ""
)

$ExpectedSHA1 = "EF:20:92:F4:19:1E:2A:E7:AE:8F:0A:D6:E7:1D:8C:05:2C:F0:C0:82"
$WrongSHA1 = "98:92:E0:E8:19:52:0A:FD:F8:4E:8A:BA:2D:F6:4A:F2:0A:B3:C1:84"

Write-Host "================================" -ForegroundColor Cyan
Write-Host "密钥库验证工具" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""

# 检查文件是否存在
if (-not (Test-Path $KeystorePath)) {
    Write-Host "❌ 错误: 找不到密钥库文件: $KeystorePath" -ForegroundColor Red
    Write-Host ""
    Write-Host "请指定密钥库文件路径:" -ForegroundColor Yellow
    Write-Host "  .\verify-keystore.ps1 -KeystorePath '路径\到\keystore.jks'" -ForegroundColor Gray
    exit 1
}

Write-Host "📁 密钥库文件: $KeystorePath" -ForegroundColor Green
Write-Host ""

# 如果没有提供 alias，先列出所有 alias
if ([string]::IsNullOrEmpty($Alias)) {
    Write-Host "🔍 正在列出密钥库中的所有别名..." -ForegroundColor Yellow
    Write-Host ""
    keytool -list -keystore $KeystorePath
    Write-Host ""
    Write-Host "请输入要验证的别名 (alias): " -ForegroundColor Yellow -NoNewline
    $Alias = Read-Host
}

Write-Host ""
Write-Host "🔐 正在验证别名: $Alias" -ForegroundColor Yellow
Write-Host ""

# 获取详细信息
$output = keytool -list -v -keystore $KeystorePath -alias $Alias 2>&1

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ 错误: 无法读取密钥库信息" -ForegroundColor Red
    Write-Host $output
    exit 1
}

# 提取 SHA1 指纹
$sha1Line = $output | Select-String -Pattern "SHA1:" | Select-Object -First 1
if ($sha1Line) {
    $currentSHA1 = $sha1Line.ToString().Trim() -replace ".*SHA1:\s*", ""
    
    Write-Host "================================" -ForegroundColor Cyan
    Write-Host "验证结果" -ForegroundColor Cyan
    Write-Host "================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "当前密钥库的 SHA1: " -ForegroundColor White -NoNewline
    Write-Host $currentSHA1 -ForegroundColor Yellow
    Write-Host ""
    Write-Host "需要的 SHA1:       " -ForegroundColor White -NoNewline
    Write-Host $ExpectedSHA1 -ForegroundColor Green
    Write-Host ""
    Write-Host "错误的 SHA1:       " -ForegroundColor White -NoNewline
    Write-Host $WrongSHA1 -ForegroundColor Red
    Write-Host ""
    
    if ($currentSHA1 -eq $ExpectedSHA1) {
        Write-Host "✅ 正确! 这是需要的密钥库!" -ForegroundColor Green
        Write-Host ""
        Write-Host "下一步: 将此密钥库更新到 GitHub Secrets" -ForegroundColor Cyan
        Write-Host ""
        
        # 生成 Base64
        Write-Host "正在生成 Base64 编码..." -ForegroundColor Yellow
        $base64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes($KeystorePath))
        $outputFile = "keystore-base64.txt"
        $base64 | Out-File -FilePath $outputFile -Encoding ASCII -NoNewline
        
        Write-Host "✅ Base64 已保存到: $outputFile" -ForegroundColor Green
        Write-Host ""
        Write-Host "请按以下步骤操作:" -ForegroundColor Cyan
        Write-Host "1. 打开 GitHub 仓库: https://github.com/TechxArtisanStudio/Openterface_Android" -ForegroundColor White
        Write-Host "2. 进入 Settings → Secrets and variables → Actions" -ForegroundColor White
        Write-Host "3. 点击 SIGNING_KEY → Update secret" -ForegroundColor White
        Write-Host "4. 复制 $outputFile 的内容并粘贴" -ForegroundColor White
        Write-Host "5. 点击 Update secret" -ForegroundColor White
        Write-Host ""
        Write-Host "⚠️  完成后请删除 $outputFile 文件!" -ForegroundColor Red
        
    } elseif ($currentSHA1 -eq $WrongSHA1) {
        Write-Host "❌ 这是错误的密钥库!" -ForegroundColor Red
        Write-Host ""
        Write-Host "请找到正确的密钥库文件 (SHA1: $ExpectedSHA1)" -ForegroundColor Yellow
        
    } else {
        Write-Host "⚠️  这是另一个密钥库 (既不是正确的也不是当前错误的)" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "请继续查找 SHA1 为 $ExpectedSHA1 的密钥库" -ForegroundColor Yellow
    }
} else {
    Write-Host "❌ 无法提取 SHA1 指纹" -ForegroundColor Red
}

Write-Host ""
Write-Host "================================" -ForegroundColor Cyan
Write-Host "完整的证书信息:" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host $output
