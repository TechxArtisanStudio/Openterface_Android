# Tutorial 13 — Advanced Features

**Level:** Advanced · **Prerequisites:** Tutorials 1–12 · **Estimated time:** varies

These are the deeper capabilities and future directions of the app.

---

## 13.1 Relative Mouse Mode (In Development)

Currently, the app primarily uses **absolute mouse positioning** — you tap and the cursor jumps there. A **relative mouse mode** is being developed that would work like a laptop trackpad:

- Drag your finger and the cursor moves relative to your drag
- Lift your finger and the cursor stays where it is
- Useful for tasks that require fine cursor control without tapping exact positions

This requires changes to how the HID (Human Interface Device) reports are sent to the target computer.

---

## 13.2 Multiple Device Support

Future versions may support managing **multiple Openterface devices** simultaneously:

- Switch between target computers without unplugging
- Each device maintains its own video feed and HID connection
- A device-switcher UI lets you pick which target to control

---

## 13.3 Performance Optimization

If you're experiencing frame delay or lag:

- **Lower the resolution** — 640×480 is much lighter than 1920×1080
- **Reduce the frame rate** — 30fps uses less bandwidth than 60fps
- **Close other apps** — free up your phone's memory
- **Use a good quality USB OTG adapter** — cheap adapters can bottleneck the connection

The development team is also working on reducing latency in the camera pipeline itself.

---

## 13.4 Accessibility

The app is being improved to support:

- **TalkBack** — screen reader support for visually impaired users
- **Content descriptions** — meaningful labels for all interactive elements
- **Keyboard navigation** — using a physical keyboard connected to your phone to navigate the app

---

## 13.5 Plugin System for Keyboard Layouts

A future goal is to allow **downloadable keyboard layouts** so users can add support for any regional keyboard without waiting for an app update. This would work through:

- A standard layout definition format
- A way to import and activate new layouts
- Community-contributed layouts for less common keyboards

---

## 13.6 Network KVM (Future Vision)

One day, the app may extend beyond USB to support **network-based KVM**:

- Control a target computer over a local network or the internet
- Stream video over TCP or WebSocket
- Send keyboard and mouse commands remotely

This would turn the app into a full remote management tool, not just a local USB KVM.

---

## Checkpoint

- [ ] You understand the direction the app is heading
- [ ] You know what advanced features are in development
- [ ] You can suggest features or contribute to development (see Tutorial 14)

When you're ready to give back, move on to **[Tutorial 14 — Getting Help and Contributing](./14_help_contributing.md)**.
