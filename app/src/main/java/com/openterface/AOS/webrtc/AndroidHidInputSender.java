package com.openterface.AOS.webrtc;

import com.openterface.AOS.target.HidManager;

import android.util.Log;

/**
 * Android implementation of HidInputSender.
 * Routes input events through HidManager which can use either
 * Core JNI or Java implementation based on configuration.
 */
public class AndroidHidInputSender implements HidInputSender {
    private static final String TAG = "AndroidHidInputSender";

    @Override
    public void setMouseDimensions(int width, int height) {
        HidManager.width_height(width, height);
    }

    @Override
    public void sendAbsMove(int x, int y) {
        HidManager.sendHexAbsData(x, y);
    }

    @Override
    public void sendAbsButtonClick(String clickType, int x, int y) {
        HidManager.sendHexAbsButtonClickData(clickType, x, y);
    }

    @Override
    public void sendKeyboardPress(String functionKey, String keyName) {
        HidManager.sendKeyBoardPress(functionKey, keyName);
    }

    @Override
    public void sendKeyboardKey(String keyName) {
        Log.i(TAG, "sendKeyboardKey: " + keyName);
        HidManager.sendKeyBoardPressAndRelease("00", keyName);
    }

    @Override
    public void sendKeyboardRelease() {
        HidManager.sendKeyBoardRelease();
    }
}
