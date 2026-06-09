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
        // x,y are already normalized 0-4095 from WebRTC client
        // Convert back to pixel coordinates for HidManager (which re-normalizes to 0-4095)
        int width = HidManager.getScreenWidth();
        int height = HidManager.getScreenHeight();
        int pixelX = (int) (x / 4095.0f * width);
        int pixelY = (int) (y / 4095.0f * height);
        HidManager.sendHexAbsData(pixelX, pixelY);
    }

    @Override
    public void sendAbsButtonClick(String clickType, int x, int y) {
        // x,y are already normalized 0-4095 from WebRTC client
        // Convert back to pixel coordinates for HidManager (which re-normalizes to 0-4095)
        int width = HidManager.getScreenWidth();
        int height = HidManager.getScreenHeight();
        int pixelX = (int) (x / 4095.0f * width);
        int pixelY = (int) (y / 4095.0f * height);
        HidManager.sendHexAbsButtonClickData(clickType, pixelX, pixelY);
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
