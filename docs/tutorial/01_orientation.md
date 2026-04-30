# Tutorial 1 — Getting Started: What Is Openterface?

**Level:** Beginner · **Prerequisites:** None · **Estimated time:** 10 minutes

Welcome! This is your first stop. By the end you'll understand what the Openterface app does and what you need to use it.

---

## 1.1 What Is This App?

The Openterface Mini-KVM app turns your Android phone or tablet into a **KVM console** — a way to control another computer using Keyboard, Video, and Mouse.

Think of it like sitting at a physical keyboard and monitor plugged into a server, except everything runs through your phone.

You can:

- **See** the target computer's screen in real-time via USB video capture
- **Tap** anywhere on your phone screen to move the mouse cursor on the target
- **Type** on the target using a built-in on-screen keyboard with function keys, shortcuts, and modifier keys
- **Adjust** the video — brightness, contrast, rotation
- **Record** the screen or take screenshots

---

## 1.2 What You Need

| Item | Why |
|---|---|
| **Openterface Mini-KVM hardware** | The bridge between your phone and the target computer |
| **Android phone or tablet** | Running Android 8.0 or later |
| **USB OTG cable or adapter** | Connects the Openterface device to your phone |
| **HDMI cable** | Carries video from the target to the Openterface |
| **USB cable** | Carries mouse/keyboard signals to the target |
| **Target computer** | The machine you want to control |

---

## 1.3 How It All Connects

```
┌──────────────┐     HDMI      ┌──────────────────┐
│              │ ────────────▶ │   Openterface     │
│  Target PC   │               │   Mini-KVM        │
│  (screen)    │ ◀─────────── │   Device          │
│              │     USB       │                   │
└──────────────┘               └────────┬─────────┘
                                        │
                                   USB OTG
                                        │
                               ┌────────▼─────────┐
                               │  Android Phone    │
                               │  (this app)       │
                               └──────────────────┘
```

The Openterface device sits between your phone and the target computer:
- It **captures** the target's HDMI video and sends it to your phone
- It **translates** your taps and keystrokes into USB signals the target sees as a real keyboard and mouse

---

## 1.4 The App at a Glance

When you open the app, you'll see:

- A **large video area** — shows the target computer's screen
- A **keyboard button** (bottom-right) — opens the on-screen keyboard
- A **menu button** (bottom-right) — opens the settings panel

That's it. The app is designed to be simple — most of the screen is the video preview, because that's what you need to see.

---

## Checkpoint

You should be able to:

- [ ] Explain what the app does in one sentence
- [ ] List the hardware you need
- [ ] Draw how the devices connect to each other

When you're ready, move on to **[Tutorial 2 — Installing the App](./02_installing.md)**.
