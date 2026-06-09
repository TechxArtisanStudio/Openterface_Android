# Version Management Guide

This project uses automated version management through GitHub Actions workflows.

## 📋 Overview

The project uses **Semantic Versioning** (SemVer):
- **Format**: `MAJOR.MINOR.PATCH` (e.g., `1.2.0`)
- **Version Code**: Auto-increments with each version bump
- **Location**: `app/build.gradle`

### Version Bump Types

| Type | When to Use | Example | Version Code |
|------|-------------|---------|--------------|
| **Patch** | Bug fixes, small changes | 1.2.0 → 1.2.1 | Auto +1 |
| **Minor** | New features, backwards compatible | 1.2.0 → 1.3.0 | Auto +1 |
| **Major** | Breaking changes, major updates | 1.2.0 → 2.0.0 | Auto +1 |

## 🚀 How to Bump Version

### Method 1: Dedicated Version Bump Workflow (Recommended)

1. Go to **Actions** → **Bump Version** → **Run workflow**
2. Select options:
   - **Bump type**: Choose `patch`, `minor`, or `major`
   - **Create git tag**: Check to create a version tag (e.g., `v1.2.1`)
   - **Trigger build**: Check to automatically build after version bump
3. Click **Run workflow**

**What it does:**
- ✅ Updates `versionName` in build.gradle (e.g., 1.2.0 → 1.2.1)
- ✅ Auto-increments `versionCode` (e.g., 7 → 8)
- ✅ Commits changes to main branch
- ✅ (Optional) Creates a git tag
- ✅ (Optional) Triggers build workflow

### Method 2: Build with Version Bump

1. Go to **Actions** → **Openterface Build & Package** → **Run workflow**
2. Select **Version bump type**: Choose `patch`, `minor`, `major`, or `none`
3. Click **Run workflow**

**What it does:**
- ✅ Bumps version (if not `none`)
- ✅ Commits version changes
- ✅ Builds APK/AAB with new version

### Method 3: Manual Version Update

Edit `app/build.gradle`:
```gradle
defaultConfig {
    versionCode 8        // Increment this
    versionName "1.2.1"  // Update this
}
```

**Important:**
- `versionCode` must always increment (never reuse)
- `versionName` should follow SemVer format

## 📦 Version Release Workflow

### Complete Release Process:

```
1. Bump Version → 2. Build → 3. Create Release
```

#### Step 1: Bump Version
```
Actions → Bump Version → Run workflow
- Bump type: patch/minor/major
- Create git tag: ✓ YES
- Trigger build: ✓ YES
```

#### Step 2: Build (Automatic)
The build workflow runs automatically and creates:
- Signed APK
- Signed AAB (for Google Play)

#### Step 3: Create Release
```
Actions → Create GitHub Release → Run workflow
- Release type: release (or pre-release)
```
**Note**: The workflow automatically uses the version from `build.gradle` for the tag and title.

## 🔄 Automated Workflows

### `bump-version.yaml`
**Purpose**: Dedicated version bumping
- Manual trigger only
- Updates version in build.gradle
- Commits changes
- Optional: Create git tag
- Optional: Trigger build

### `build-package.yaml`
**Purpose**: Build APK/AAB
- Triggers on: push to main, pull request, manual
- Optional version bump (manual trigger only)
- Creates signed APK/AAB artifacts

### `create-release.yaml`
**Purpose**: Create GitHub releases
- Downloads latest build artifacts
- Auto-generates tag from build.gradle version
- Auto-generates title from version
- Creates GitHub release with APK/AAB
- Supports pre-release and release types

## 📝 Examples

### Example 1: Bug Fix Release
```
1. Actions → Bump Version
   - Bump type: patch
   - Create git tag: YES
   - Trigger build: YES
   
Result: 1.2.0 → 1.2.1 (versionCode: 7 → 8)
```

### Example 2: New Feature Release
```
1. Actions → Bump Version
   - Bump type: minor
   - Create git tag: YES
   - Trigger build: YES
   
Result: 1.2.1 → 1.3.0 (versionCode: 8 → 9)
```

### Example 3: Major Version Release
```
1. Actions → Bump Version
   - Bump type: major
   - Create git tag: YES
   - Trigger build: YES

Result: 1.3.0 → 2.0.0 (versionCode: 9 → 10)
```

## ⚠️ Important Notes

### Version Code Rules
- **Must always increment** (never decrease or reuse)
- **Google Play** requires each upload to have a unique, higher version code
- **Cannot be reversed** once uploaded to Play Store

### Version Name Best Practices
- Follow semantic versioning (MAJOR.MINOR.PATCH)
- Keep it simple and meaningful
- Example: `1.2.0`, `2.0.0`, `1.3.1`

### Git Tags
- Format: `v1.2.0` (lowercase 'v' + version)
- Created automatically when "Create git tag" is enabled
- Used by release workflow to identify versions

## 🔍 Checking Current Version

### From Code:
```bash
grep -A 2 "versionCode" app/build.gradle
```

### From GitHub:
- Go to latest successful build in Actions
- Check the build summary for version info

### From Releases:
- Check the latest release tag (e.g., `v1.2.0`)

## 🎯 Quick Reference

| Task | Workflow | Settings |
|------|----------|----------|
| Bug fix | Bump Version | patch, tag: yes, build: yes |
| New feature | Bump Version | minor, tag: yes, build: yes |
| Major update | Bump Version | major, tag: yes, build: yes |
| Just build | Build & Package | version_bump: none |
| Create release | Create Release | Use existing tag |

## 🆘 Troubleshooting

### "Version code X already used"
- Google Play remembers all version codes
- Bump to a higher version code
- Check Play Console for used version codes

### "Tag already exists"
- Delete the tag or use a different version
- Or skip "Create git tag" option

### Build fails after version bump
- Check build.gradle syntax
- Ensure version bump committed successfully
- Re-run the build workflow manually

## 📚 Related Files

- `app/build.gradle` - Version configuration
- `.github/workflows/bump-version.yaml` - Version bump workflow
- `.github/workflows/build-package.yaml` - Build workflow
- `.github/workflows/create-release.yaml` - Release workflow
