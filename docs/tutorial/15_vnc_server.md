# Tutorial — VNC Server

**Level:** Intermediate · **Prerequisites:** Tutorial 3 (Connecting Hardware), Tutorial 8 (Settings Panel) · **Estimated time:** 15 minutes

The VNC server lets you remotely view and control the target device from any VNC client on the same network. The VNC server captures frames from the UVC camera (HDMI input) and streams them to remote clients. Keyboard and mouse input from VNC clients is routed through the USB HID (CH9329) path to the target device.

---

## 1. What You Need

- The Openterface device (USB camera/HID) connected to the phone
- A VNC client on another machine — examples:
  - **RealVNC Viewer** — available for Windows, macOS, Linux, iOS, Android
  - **TigerVNC** — open source, good performance
  - **Remmina** — Linux multi-protocol client
  - macOS built-in Screen Sharing — no extra software needed on Mac
- Network connectivity between the phone and the VNC client machine (same Wi-Fi or via USB tethering)

---

## 2. Starting the VNC Server

1. Open the Openterface app and connect the Openterface device
2. Tap the **VNC icon** (cast icon ⊔) in the drawer to open the settings dialog
3. Configure the settings (see section 3 below)
4. Tap **Start Server**
5. A notification will appear confirming the VNC server is running

> **Important:** The VNC server only works when the UVC camera is active (i.e., the target device is connected and streaming video). If the camera is not connected, the server cannot start.

---

## 3. VNC Server Settings

### 3.1 Basic Settings

| Setting | Default | Description |
|---------|---------|-------------|
| **Port** | 5900 | The TCP port the VNC server listens on |
| **Password** | (empty) | VNC authentication password, max 8 characters. Leave empty for no authentication (not recommended on untrusted networks) |
| **Encoding** | Tight | The video encoding method. **Tight** is recommended — it uses JPEG compression and adapts well to bandwidth constraints |
| **Auto-start** | Off | Automatically start the VNC server when the camera connects |

### 3.2 Quality and Compression (Tight Encoding)

When using **Tight** encoding, you can fine-tune the balance between image quality and bandwidth usage:

| Setting | Options | Description |
|---------|---------|-------------|
| **Quality Level** | Auto / Low / Medium / High / Max | JPEG quality for continuous-tone regions. Higher = better image quality but more bandwidth. **Medium** is a good default. |
| **Compression Level** | Auto / Fast / Medium / Max | zlib compression level. Higher = smaller data but more CPU usage. **Medium** is a good default. |

**Recommended combinations:**

| Use Case | Quality | Compression |
|----------|---------|-------------|
| High quality (local network) | High | Fast |
| Balanced (general use) | Medium | Medium |
| Low bandwidth (slow network) | Low | Max |

> **Important:** Quality and compression settings **only apply when the server starts**. If you change them while the server is running, the settings will be greyed out. You must stop the server and restart it for the new values to take effect.

### 3.3 Settings Are Fixed While Running

When the VNC server is active, the encoding, quality, and compression spinners are **disabled** (greyed out). This is because these settings are applied at connection time by the libvncserver engine and cannot be changed dynamically. To change them:

1. Stop the VNC server
2. Adjust the settings
3. Start the server again

---

## 4. Connecting from a VNC Client

### 4.1 Find the Phone's IP Address

```bash
# On the phone, check the Wi-Fi IP:
adb shell ip addr show wlan0 | grep "inet "
# or
adb shell settings get global wifi_ip_address
```

Alternatively, check the phone's Wi-Fi settings in the Android system settings.

### 4.2 Connect

From your VNC client, connect to:

```
<phone-ip>:5900
```

For example: `192.168.1.100:5900`

If you set a password, you'll be prompted to enter it after connecting.

### 4.3 Verify the Connection

- You should see the target device's screen in the VNC client window
- Move your mouse and type — the input should appear on the target device
- Check the app's logcat for activity:

```bash
adb logcat -s VncServerJNI:V VncServerService:V
```

---

## 5. Troubleshooting

| Problem | Solution |
|---------|----------|
| **Cannot connect** | Verify the server is running (check for the notification). Ensure the phone and client are on the same network. Check firewall settings on the phone. |
| **Connection refused** | The server may not be started, or the port is in use. Try a different port in settings. |
| **Blank screen** | The UVC camera may not be connected or streaming. Ensure the target device is properly connected. |
| **High latency** | Lower the quality level and/or increase compression. Try a different encoding. |
| **Crash when stopping server** | Disconnect VNC clients before stopping the server, or use the latest app version (this was fixed in a recent update) |
| **Settings greyed out** | The server is currently running. Stop it first to change encoding, quality, or compression settings |

---

## 6. Bandwidth Optimization Tips

If you're experiencing high bandwidth usage:

1. **Lower the quality level** — `Low` quality significantly reduces JPEG data size
2. **Increase compression** — `Max` compression reduces network traffic at the cost of CPU
3. **Check resolution** — Lower camera resolution = smaller frame buffer = less data
4. **Use a wired connection** — USB tethering provides more stable bandwidth than Wi-Fi

The Tight encoding automatically switches between JPEG (continuous-tone) and lossless (text/graphics) regions, so it adapts well to mixed content.

---

## Checkpoint

- [ ] You can open the VNC settings and configure the server
- [ ] You understand how quality and compression affect bandwidth
- [ ] You can start the server and connect from a VNC client
- [ ] You know that settings cannot be changed while the server is running
- [ ] You can troubleshoot basic connection issues

When you're ready, move on to **[Tutorial 9 — Settings That Persist](./09_persistence.md)**.
