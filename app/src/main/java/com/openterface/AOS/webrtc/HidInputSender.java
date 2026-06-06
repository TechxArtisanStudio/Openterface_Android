package com.openterface.AOS.webrtc;

/**
 * Interface for sending HID input events to the target machine.
 * Decouples the input router from the concrete Android HID implementations,
 * enabling unit testing with mock implementations.
 */
public interface HidInputSender {

    /**
     * Set mouse coordinate normalization dimensions.
     */
    void setMouseDimensions(int width, int height);

    /**
     * Send absolute mouse movement.
     */
    void sendAbsMove(int x, int y);

    /**
     * Send absolute mouse button click.
     * @param clickType "SecLeftData", "SecMiddleData", "SecRightData", or "SecNullData"
     */
    void sendAbsButtonClick(String clickType, int x, int y);

    /**
     * Send keyboard key press.
     * @param functionKey Modifier key ("Shift", "Ctrl", "Alt", "Win", or "00")
     * @param keyName     Key name from VncKeyMap
     */
    void sendKeyboardPress(String functionKey, String keyName);

    /**
     * Send regular keyboard key (no modifier).
     * @param keyName Key name from VncKeyMap
     */
    void sendKeyboardKey(String keyName);

    /**
     * Send keyboard release (clear all modifiers).
     */
    void sendKeyboardRelease();
}
