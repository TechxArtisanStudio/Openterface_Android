# Tutorial 3 — Connecting Your Hardware

**Level:** Beginner · **Prerequisites:** Tutorial 2 · **Estimated time:** 10 minutes

This tutorial walks you through connecting the Openterface device to your phone and the target computer.

---

## 3.1 Cable Everything Up

Follow this order:

1. **HDMI:** Connect the target computer's HDMI output to the Openterface device's HDMI **input** port
2. **USB (target):** Connect the target computer's USB port to the Openterface device's USB port — this carries your mouse and keyboard signals
3. **USB OTG (phone):** Connect the Openterface device to your Android phone using a USB OTG cable or adapter
4. **Power:** Power on the Openterface device (if it has a separate power input) and the target computer

---

## 3.2 What Success Looks Like

When everything is connected properly:

- The video preview switches from a placeholder icon to the **live screen** of your target computer
- You may see a system notification confirming a USB device was attached
- Tapping the screen moves the cursor on the target

If you don't see video yet, move to Section 3.3.

---

## 3.3 If It Doesn't Work

### No Video

1. **Check HDMI** — Is the target computer's HDMI cable firmly connected? Is the target actually outputting video?
2. **Check the Openterface device** — Is it powered on? Any indicator lights?
3. **Check USB OTG** — Try disconnecting and reconnecting the cable to your phone
4. **Try a different video format** — Open settings → **Video Format** and pick a lower resolution

### Phone Doesn't Detect the Device

1. **Test OTG** — Try connecting a USB flash drive to your phone. If it's not detected, your phone may not support OTG
2. **Try a different cable** — Some OTG adapters are faulty
3. **Restart the app** — Close it fully (swipe away from recent apps) and reopen

### Device Detected But Permission Denied

A system dialog should appear asking for USB permission. If it doesn't:

1. Open settings → **Device** in the app
2. Look for a **Request Permission** button
3. When the dialog appears, tap **Allow**

---

## 3.4 Safe Disconnect

When you're done:

1. Open the settings panel (tap the menu button)
2. Tap **Disconnect Device** (shown in red)
3. Wait a moment for the app to release the USB connection
4. Unplug the cables

This prevents data corruption and ensures the target computer properly releases the keyboard/mouse.

---

## Checkpoint

- [ ] Your Openterface device is connected to both the target and your phone
- [ ] You see the target computer's screen in the video preview
- [ ] You know how to safely disconnect

When you're ready, move on to **[Tutorial 4 — Understanding the Main Screen](./04_main_screen.md)**.
