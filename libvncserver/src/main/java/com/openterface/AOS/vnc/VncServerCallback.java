package com.openterface.AOS.vnc;

/**
 * Interface for VNC server client events.
 */
public interface VncServerCallback {
    void onClientConnected(String ip);
    void onClientDisconnected(String ip);
    void onPointerEvent(int buttonMask, int x, int y);
    void onKeyboardEvent(int keysym, boolean down);
    void onEncodingChanged(String ip, String encoding);
}
