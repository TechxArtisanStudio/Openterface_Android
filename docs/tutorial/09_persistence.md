# Tutorial 9 — Settings That Persist

**Level:** Beginner · **Prerequisites:** Tutorial 8 · **Estimated time:** 5 minutes

This tutorial explains which settings are saved between sessions and how the app remembers your preferences.

---

## 9.1 What Gets Saved

The app remembers your choices so you don't have to reconfigure every time you open it. These settings are saved:

| Setting | Is It Saved? |
|---|---|
| Mouse control mode (Absolute / Relative / Drag) | Yes |
| Video format (resolution, FPS) | Yes |
| Camera parameters (brightness, contrast, hue) | Yes |
| Keyboard layout (US, JP, DE) | Yes |
| Baudrate | Yes |
| Device connection history | Yes |

---

## 9.2 When Settings Are Loaded

When you open the app:

1. The app checks for saved preferences
2. It applies your last choices — mouse mode, video format, camera settings
3. The screen looks the way you left it

This means you only need to configure things **once**. The next time you launch, everything is as you set it.

---

## 9.3 Resetting to Defaults

If something feels off and you want to start fresh:

1. Change any setting back to what you want
2. For a full reset, you can clear the app's data in Android Settings → Apps → Openterface → Storage → Clear Data

> **Warning:** Clearing app data removes all saved preferences and returns the app to its first-launch state.

---

## Checkpoint

- [ ] You know which settings are saved automatically
- [ ] You understand that the app loads your last settings on startup
- [ ] You know how to reset settings if needed

When you're ready, move on to **[Tutorial 10 — Troubleshooting](./10_troubleshooting.md)**.
