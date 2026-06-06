package com.openterface.AOS.webrtc;

import com.openterface.AOS.target.HidManager;

/**
 * Android implementation of KeyboardSender.
 * Delegates to HidManager which can use Core or Java.
 */
public class AndroidKeyboardSender implements KeyboardSender {

    @Override
    public void sendKeyBoardPressQueued(String functionKey, String keyName) {
        HidManager.sendKeyBoardPressQueued(functionKey, keyName);
    }

    @Override
    public void sendKeyBoardPressAndRelease(String functionKey, String keyName) {
        HidManager.sendKeyBoardPressAndRelease(functionKey, keyName);
    }

    @Override
    public void sendKeyBoardReleaseQueued() {
        HidManager.sendKeyBoardReleaseQueued();
    }
}
