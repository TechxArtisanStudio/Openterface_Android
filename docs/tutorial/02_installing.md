# Tutorial 2 — Installing the App

**Level:** Beginner · **Prerequisites:** Tutorial 1 · **Estimated time:** 10 minutes

Get the app installed on your Android device. By the end you'll have the app running and ready to connect.

---

## 2.1 Install from the Play Store (Recommended)

If the app is published on Google Play, simply search for "Openterface" and install. This is the easiest way and ensures you always get updates automatically.

---

## 2.2 Install a Release APK

If the app is not on the Play Store, download the latest release APK:

1. Go to the project's [GitHub Releases page](https://github.com/TechxArtisanStudio/Openterface_Android/releases)
2. Find the latest release
3. Download the `.apk` file to your Android device
4. Tap the downloaded file to install

You may need to allow installation from "Unknown Sources" in your device settings.

---

## 2.3 First Launch Permissions

When you open the app for the first time, it will ask for permissions:

| Permission | Why | What Happens If Denied |
|---|---|---|
| **USB Host** | Talk to the Openterface hardware | App can't detect your device |
| **Camera** | Receive video from the target | No video preview |
| **Storage** | Save screenshots and recordings | Can't save captures |

Grant all permissions for full functionality.

---

## 2.4 Check Your Android Version

The app requires **Android 8.0 (API 26)** or later. To check:

1. Open **Settings** on your Android device
2. Go to **About Phone**
3. Look for **Android Version**

If your device runs an older version, the app will not install.

---

## 2.5 Verify USB OTG Support

Not all Android devices support USB OTG. To check:

- Most modern phones support it (Samsung, Google Pixel, OnePlus, etc.)
- Search "[your device model] USB OTG" online
- If unsure, try connecting any USB flash drive with an OTG adapter — if your phone detects it, OTG works

---

## Checkpoint

- [ ] The app is installed on your device
- [ ] You've granted all requested permissions
- [ ] Your device supports USB OTG

When you're ready, move on to **[Tutorial 3 — Connecting Your Hardware](./03_connecting_hardware.md)**.
