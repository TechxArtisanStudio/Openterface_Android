# Tutorial 10 — Troubleshooting

**Level:** Beginner/Intermediate · **Prerequisites:** Tutorials 1–9 · **Estimated time:** varies by issue

When something doesn't work, this tutorial helps you find and fix the problem.

---

## 10.1 Quick Diagnostic Flow

Start at the top and work down. Each step checks one part of the chain:

```
1. Is the Openterface device detected by your phone?
       ↓ Yes
2. Is the video preview showing the target's screen?
       ↓ Yes
3. Can you move the mouse by tapping?
       ↓ Yes
4. Can you type using the on-screen keyboard?
       ↓ Yes
   Everything works!
```

If any step fails, follow the specific troubleshooting section below.

---

## 10.2 Device Not Detected

**Symptom:** Video preview shows a placeholder, not the target's screen.

1. **Check the USB OTG connection** — unplug and replug the cable
2. **Verify OTG support** — try connecting a USB flash drive to confirm your phone supports OTG
3. **Check the Openterface device** — is it powered on? Are any indicator lights on?
4. **Try a different cable** — some OTG adapters are faulty
5. **Restart the app** — close it fully (swipe from recent apps) and reopen
6. **Check USB permission** — if a system dialog asked for USB access, make sure you tapped **Allow**

---

## 10.3 No Video

**Symptom:** Device is detected but the screen is black or frozen.

1. **Check the HDMI cable** — is the target computer's HDMI firmly connected to the Openterface HDMI input?
2. **Check the target's output** — is the target computer actually displaying something? Try a different HDMI cable
3. **Try a lower resolution** — open settings → **Video Format** → pick a lower resolution
4. **Check camera permission** — go to Android Settings → Apps → Openterface → Permissions → ensure Camera is allowed
5. **Restart the app**

---

## 10.4 Mouse Not Responding

**Symptom:** Video works but tapping the screen does nothing on the target.

1. **Check USB connection for HID** — open settings → **Device** and confirm the device is active
2. **Try a different mouse mode** — switch from Absolute to Relative or vice versa
3. **Disconnect and reconnect** — use the red **Disconnect Device** button, then reconnect
4. **Check the target computer** — does it recognize a USB keyboard/mouse? Try unplugging and replugging the USB cable on the target side

---

## 10.5 Keyboard Not Sending Keys

**Symptom:** Mouse works but typing does nothing.

1. **Make sure the keyboard is open** — tap the keyboard button
2. **Check the serial connection** — open settings → **Device** and confirm it's active
3. **Check the baudrate** — open settings → **Baudrate** and make sure it matches your device (115200 is the default)
4. **Try the System key panel first** — if regular keys work but shortcuts don't, the issue may be with a specific combination
5. **Check the keyboard layout** — make sure the correct layout (US, JP, DE) is selected

---

## 10.6 App Crashes or Freezes

1. **Close and restart** the app
2. **Lower the video resolution and frame rate** — high settings can overwhelm devices with limited memory
3. **Check available storage** — low storage can cause instability
4. **Update the app** — check for a newer version in the releases

---

## 10.7 Recordings or Screenshots Not Saving

1. **Check Storage permission** — Android Settings → Apps → Openterface → Permissions → Storage
2. **Check available storage space** on your device
3. Recordings and screenshots are saved to your device's default media folder

---

## 10.8 Getting Logs (For Advanced Debugging)

If the standard troubleshooting doesn't help, you can collect logs to share with the maintainers:

```bash
adb logcat | grep -i openterface > openterface.log
```

This captures everything the app is doing. Include this file when you open a GitHub issue.

---

## Checkpoint

- [ ] You can follow the diagnostic flow to isolate a problem
- [ ] You know the common fixes for device, video, mouse, and keyboard issues
- [ ] You know how to collect logs for advanced debugging

When you're ready, move on to **[Tutorial 11 — Updates and Versions](./11_updates_versions.md)**.
