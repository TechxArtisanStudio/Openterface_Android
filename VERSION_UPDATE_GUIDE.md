# 📱 App Version Management Guide

## ✅ Version Updated!

Your app version has been updated:

| Property | Old Value | New Value |
|----------|-----------|-----------|
| **versionCode** | 5 | **6** ✅ |
| **versionName** | "1.1.2" | **"1.1.3"** ✅ |

## 📋 What Changed

**File**: `app/build.gradle`

```groovy
defaultConfig {
    versionCode 6        // ← Changed from 5
    versionName "1.1.3"  // ← Changed from "1.1.2"
}
```

## 🚀 Next Steps

### 1. Commit the Changes

```powershell
git add app/build.gradle
git commit -m "Bump version to 1.1.3 (versionCode 6) for 16 KB page size compliance"
git push
```

### 2. Wait for Build

The GitHub Actions workflow will automatically:
- ✅ Build the new APK/AAB
- ✅ Sign with your keystore
- ✅ Create: `Openterface-v1.1.3-release.apk`
- ✅ Create: `Openterface-v1.1.3-release.aab`

### 3. Download the Build

After the workflow completes:
1. Go to the **Actions** tab
2. Click on the latest workflow run
3. Scroll to **Artifacts**
4. Download `Openterface-v1.1.3-APK` or `Openterface-v1.1.3-AAB`

### 4. Upload to Google Play

1. Go to Google Play Console
2. Navigate to your app
3. Click **Production** → **Create new release**
4. Upload `Openterface-v1.1.3-release.aab`
5. Complete the release notes
6. **Verify**: 16 KB page size warning should be gone! ✅
7. Click **Review release** → **Start rollout to Production**

---

## 📚 Understanding Version Numbers

### versionCode (Integer)

- **What**: Internal version number (not visible to users)
- **Rule**: MUST increase with each release
- **Current**: 6
- **Next release**: 7, 8, 9, etc.
- **Important**: Google Play tracks this - can never reuse a number!

### versionName (String)

- **What**: User-facing version (shown in Google Play)
- **Format**: Usually "major.minor.patch" (e.g., "1.1.3")
- **Current**: "1.1.3"
- **Next release**: 
  - Patch: "1.1.4" (bug fixes)
  - Minor: "1.2.0" (new features)
  - Major: "2.0.0" (major changes)

---

## 🔄 How to Update Versions in the Future

### Method 1: Manual Edit

Open `app/build.gradle` and change:

```groovy
versionCode 7        // Increment by 1
versionName "1.1.4"  // Update as needed
```

### Method 2: Using Script

Create `bump-version.ps1`:

```powershell
# Read current version
$buildGradle = Get-Content "app/build.gradle"
$currentCode = ($buildGradle | Select-String "versionCode (\d+)").Matches.Groups[1].Value

# Increment
$newCode = [int]$currentCode + 1

# Update file
$buildGradle = $buildGradle -replace "versionCode $currentCode", "versionCode $newCode"
$buildGradle | Set-Content "app/build.gradle"

Write-Host "✅ Version code updated: $currentCode → $newCode"
```

Then run: `.\bump-version.ps1`

---

## 📝 Version History

| Version Code | Version Name | Date | Changes |
|--------------|--------------|------|---------|
| 1-4 | 1.0.x - 1.1.1 | Previous | Earlier releases |
| 5 | 1.1.2 | Oct 2025 | Previous release |
| **6** | **1.1.3** | **Oct 30, 2025** | **16 KB page size compliance** ✅ |

---

## ⚠️ Common Mistakes

### ❌ Mistake 1: Reusing versionCode
```groovy
versionCode 5  // Already used!
```
**Error**: "已有版本使用了版本代码"

**Fix**: Always increment!

### ❌ Mistake 2: Decreasing versionCode
```groovy
versionCode 4  // Lower than previous!
```
**Error**: Google Play won't accept it

**Fix**: Must be higher than all previous releases

### ❌ Mistake 3: Same versionCode in different branches
- Main branch: versionCode 6
- Feature branch: versionCode 6
- **Problem**: Conflict when merging!

**Fix**: Coordinate version numbers across branches

---

## 🎯 Quick Reference

**Current Version**:
- Code: **6**
- Name: **"1.1.3"**
- File: `app/build.gradle` lines 14-15

**To Update**:
1. Edit `app/build.gradle`
2. Increment `versionCode`
3. Update `versionName` (optional)
4. Commit and push
5. Build will create new APK/AAB automatically

---

## ✅ Checklist for This Release

- [x] Update versionCode to 6
- [x] Update versionName to "1.1.3"
- [ ] Commit changes
- [ ] Push to GitHub
- [ ] Wait for GitHub Actions build
- [ ] Download the AAB
- [ ] Upload to Google Play
- [ ] Verify 16 KB page size warning is gone
- [ ] Publish release

---

**Ready to commit?** Run these commands:

```powershell
git add app/build.gradle
git commit -m "Bump version to 1.1.3 (versionCode 6)"
git push
```

Your build will start automatically! 🚀
