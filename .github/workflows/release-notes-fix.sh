#!/bin/bash

# Generate automatic release notes
cat > release_notes.md << 'EOF'
## Openterface Android v1.2.2

### 📱 Application Information
- **Version**: 1.2.2
- **Version Code**: 9
- **Release Date**: RELEASE_DATE_PLACEHOLDER

### 📦 Downloads
This release includes:
- **APK**: For direct installation on Android devices
- **AAB**: For Google Play Store submission (if available)

### ✨ Features
- Support for 16 KB page size (Android 15+ compatible)
- Target SDK: Android 15 (API 35)
- Minimum SDK: Android 5.0 (API 21)

### 📥 Installation
**APK Installation:**
1. Download the APK file below
2. Enable 'Install from Unknown Sources' in Android settings
3. Open the APK file to install

**Google Play:**
Upload the AAB file to Google Play Console for distribution.

### 🔐 Security
Both APK and AAB files are signed with the official release keystore.

---
**Full Changelog**: https://github.com/TechxArtisanStudio/Openterface_Android/commits/v1.2.2
EOF

# Replace the date placeholder
sed -i "s/RELEASE_DATE_PLACEHOLDER/$(date '+%Y-%m-%d')/g" release_notes.md

echo "notes_file=release_notes.md" >> $GITHUB_OUTPUT
