package com.openterface.AOS.webrtc;

import com.openterface.AOS.target.KeyBoardManager;
import com.openterface.AOS.target.MouseManager;

/**
 * Android implementation of HidInputSender.
 * Routes input events through the existing MouseManager and KeyBoardManager
 * to the CH9329 USB serial HID path.
 */
public class AndroidHidInputSender implements HidInputSender {

    @Override
    public void setMouseDimensions(int width, int height) {
        MouseManager.width_height(width, height);
    }

    @Override
    public void sendAbsMove(int x, int y) {
        MouseManager.sendHexAbsData(x, y);
    }

    @Override
    public void sendAbsButtonClick(String clickType, int x, int y) {
        MouseManager.sendHexAbsButtonClickData(clickType, x, y);
    }

    @Override
    public void sendKeyboardPress(String functionKey, String keyName) {
        KeyBoardManager.sendKeyBoardPress(functionKey, keyName);
    }

    @Override
    public void sendKeyboardKey(String keyName) {
        KeyBoardManager.sendKeyBoardData("00", keyName);
    }

    @Override
    public void sendKeyboardRelease() {
        KeyBoardManager.sendKeyBoardRelease();
    }
}
