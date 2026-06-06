package com.openterface.AOS.webrtc;

/**
 * Interface for sending keyboard commands to the target device.
 * Abstracts the static KeyBoardManager methods for testability.
 */
public interface KeyboardSender {

    /**
     * Send a modifier key press (queued for thread safety).
     * @param functionKey Modifier type ("Shift", "Ctrl", "Alt", "Win")
     * @param keyName Key name from VncKeyMap
     */
    void sendKeyBoardPressQueued(String functionKey, String keyName);

    /**
     * Send a regular key press and release atomically.
     * @param functionKey Modifier byte ("00" for none)
     * @param keyName Key name from VncKeyMap
     */
    void sendKeyBoardPressAndRelease(String functionKey, String keyName);

    /**
     * Send keyboard release (queued).
     */
    void sendKeyBoardReleaseQueued();
}
