package com.openterface.AOS.target;

import android.util.Log;

import com.openterface.AOS.jni.KeymodJNI;
import com.openterface.AOS.serial.UsbDeviceManager;

/**
 * Unified HID Manager that can switch between Java and Core implementations.
 */
public class HidManager {
    private static final String TAG = "HidManager";

    // Feature flag to enable/disable Core integration
    private static boolean useCoreImplementation = false;

    // Track if Core library loaded successfully
    private static boolean coreLibraryLoaded = false;

    static {
        // Try to load the Core library
        try {
            System.loadLibrary("keymod");
            coreLibraryLoaded = true;
            Log.i(TAG, "Openterface_Core library loaded successfully");
        } catch (UnsatisfiedLinkError e) {
            Log.w(TAG, "Failed to load Openterface_Core library, using Java fallback: " + e.getMessage());
            coreLibraryLoaded = false;
        }
    }

    /**
     * Enable or disable Core implementation.
     * @param enable true to use Core, false to use Java
     */
    public static void setUseCoreImplementation(boolean enable) {
        if (enable && !coreLibraryLoaded) {
            Log.e(TAG, "Cannot enable Core - library not loaded");
            return;
        }
        useCoreImplementation = enable;
        Log.i(TAG, "Using " + (enable ? "Core" : "Java") + " implementation");
    }

    public static boolean isCoreImplementationEnabled() {
        return useCoreImplementation && coreLibraryLoaded;
    }

    public static boolean isCoreLibraryLoaded() {
        return coreLibraryLoaded;
    }

    // ====== Keyboard Delegation ======

    public static void sendKeyBoardData(String functionKey, String keyName) {
        if (useCoreImplementation && coreLibraryLoaded) {
            KeyBoardManagerCore.sendKeyBoardData(functionKey, keyName);
        } else {
            KeyBoardManager.sendKeyBoardData(functionKey, keyName);
        }
    }

    public static void sendKeyBoardPress(String functionKey, String keyName) {
        if (useCoreImplementation && coreLibraryLoaded) {
            KeyBoardManagerCore.sendKeyBoardPress(functionKey, keyName);
        } else {
            KeyBoardManager.sendKeyBoardPress(functionKey, keyName);
        }
    }

    public static void sendKeyBoardRelease() {
        if (useCoreImplementation && coreLibraryLoaded) {
            KeyBoardManagerCore.sendKeyBoardRelease();
        } else {
            KeyBoardManager.sendKeyBoardRelease();
        }
    }

    public static void sendKeyBoardPressQueued(String functionKey, String keyName) {
        if (useCoreImplementation && coreLibraryLoaded) {
            KeyBoardManagerCore.sendKeyBoardPressQueued(functionKey, keyName);
        } else {
            KeyBoardManager.sendKeyBoardPressQueued(functionKey, keyName);
        }
    }

    public static void sendKeyBoardReleaseQueued() {
        if (useCoreImplementation && coreLibraryLoaded) {
            KeyBoardManagerCore.sendKeyBoardReleaseQueued();
        } else {
            KeyBoardManager.sendKeyBoardReleaseQueued();
        }
    }

    public static void sendKeyBoardPressAndRelease(String functionKey, String keyName) {
        if (useCoreImplementation && coreLibraryLoaded) {
            KeyBoardManagerCore.sendKeyBoardPressAndRelease(functionKey, keyName);
        } else {
            KeyBoardManager.sendKeyBoardPressAndRelease(functionKey, keyName);
        }
    }

    public static void sendKeyBoardShortCut(String modifier, String key) {
        if (useCoreImplementation && coreLibraryLoaded) {
            KeyBoardManagerCore.sendKeyBoardShortCut(modifier, key);
        } else {
            KeyBoardManager.sendKeyBoardShortCut(modifier, key);
        }
    }

    public static void sendKeyBoardFunctionCtrlAltDel() {
        if (useCoreImplementation && coreLibraryLoaded) {
            KeyBoardManagerCore.sendKeyBoardFunctionCtrlAltDel();
        } else {
            KeyBoardManager.sendKeyBoardFunctionCtrlAltDel();
        }
    }

    public static void sendKeyBoardFunction(String ctrl, String shift, String alt, String win, String keyName) {
        if (useCoreImplementation && coreLibraryLoaded) {
            KeyBoardManagerCore.sendKeyBoardFunction(ctrl, shift, alt, win, keyName);
        } else {
            KeyBoardManager.sendKeyBoardFunction(ctrl, shift, alt, win, keyName);
        }
    }

    public static void sendKeyBoardFunctionPress(String ctrl, String shift, String alt, String win, String keyName) {
        if (useCoreImplementation && coreLibraryLoaded) {
            KeyBoardManagerCore.sendKeyBoardFunctionPress(ctrl, shift, alt, win, keyName);
        } else {
            KeyBoardManager.sendKeyBoardFunctionPress(ctrl, shift, alt, win, keyName);
        }
    }

    public static void sendKeyboardRequest(String functionKey, String keyName) {
        if (useCoreImplementation && coreLibraryLoaded) {
            KeyBoardManagerCore.sendKeyboardRequest(functionKey, keyName);
        } else {
            KeyBoardManager.sendKeyboardRequest(functionKey, keyName);
        }
    }

    public static void EmptyKeyboard() {
        if (useCoreImplementation && coreLibraryLoaded) {
            KeyBoardManagerCore.EmptyKeyboard();
        } else {
            KeyBoardManager.EmptyKeyboard();
        }
    }

    public static void sendKeyboardMultiple(String keyName) {
        if (useCoreImplementation && coreLibraryLoaded) {
            KeyBoardManagerCore.sendKeyboardMultiple(keyName);
        } else {
            KeyBoardManager.sendKeyboardMultiple(keyName);
        }
    }

    public static boolean sendKeyBoardDataEnhanced(UsbDeviceManager manager, String functionKey, String keyName) {
        if (useCoreImplementation && coreLibraryLoaded) {
            KeyBoardManagerCore.sendKeyBoardDataEnhanced(manager, functionKey, keyName);
            return true;
        } else {
            return KeyBoardManager.sendKeyBoardDataEnhanced(manager, functionKey, keyName);
        }
    }

    // ====== Mouse Delegation ======

    public static void initMouse() {
        if (useCoreImplementation && coreLibraryLoaded) {
            MouseManagerCore.init();
        } else {
            MouseManager.init();
        }
    }

    public static void releaseMouse() {
        if (useCoreImplementation && coreLibraryLoaded) {
            MouseManagerCore.release();
        } else {
            MouseManager.release();
        }
    }

    public static void setUsbDeviceManager(UsbDeviceManager manager) {
        MouseManagerCore.setUsbDeviceManager(manager);
        MouseManager.setUsbDeviceManager(manager);
        KeyBoardManager.setUsbDeviceManager(manager);
    }

    public static void width_height(int width, int height) {
        if (useCoreImplementation && coreLibraryLoaded) {
            MouseManagerCore.width_height(width, height);
        } else {
            MouseManager.width_height(width, height);
        }
    }

    public static void sendHexAbsData(float x, float y) {
        if (useCoreImplementation && coreLibraryLoaded) {
            MouseManagerCore.sendHexAbsData(x, y);
        } else {
            MouseManager.sendHexAbsData(x, y);
        }
    }

    public static void sendHexAbsButtonClickData(String mouseClick, float x, float y) {
        if (useCoreImplementation && coreLibraryLoaded) {
            MouseManagerCore.sendHexAbsButtonClickData(mouseClick, x, y);
        } else {
            MouseManager.sendHexAbsButtonClickData(mouseClick, x, y);
        }
    }

    public static void sendHexAbsDragData(float x, float y) {
        if (useCoreImplementation && coreLibraryLoaded) {
            MouseManagerCore.sendHexAbsDragData(x, y);
        } else {
            MouseManager.sendHexAbsDragData(x, y);
        }
    }

    public static void handleDoubleClickAbs(float x, float y) {
        if (useCoreImplementation && coreLibraryLoaded) {
            MouseManagerCore.handleDoubleClickAbs(x, y);
        } else {
            MouseManager.handleDoubleClickAbs(x, y);
        }
    }

    public static void releaseMSAbsData(String x0, String x1, String y0, String y1) {
        if (useCoreImplementation && coreLibraryLoaded) {
            MouseManagerCore.releaseMSAbsData();
        } else {
            MouseManager.releaseMSAbsData(x0, x1, y0, y1);
        }
    }

    public static void handleTwoPress() {
        if (useCoreImplementation && coreLibraryLoaded) {
            MouseManagerCore.handleTwoPress();
        } else {
            MouseManager.handleTwoPress();
        }
    }

    public static void handleDoubleFingerPan(float startY, float lastY) {
        if (useCoreImplementation && coreLibraryLoaded) {
            MouseManagerCore.handleDoubleFingerPan(startY, lastY);
        } else {
            MouseManager.handleDoubleFingerPan(startY, lastY);
        }
    }

    public static void handleTwoFingerPanSlideUpDown(float slideData) {
        // Not implemented in Core, delegate to Java
        MouseManager.handleTwoFingerPanSlideUpDown(slideData);
    }

    public static void sendHexRelData(String mouseClick, float sx, float sy, float lx, float ly) {
        if (useCoreImplementation && coreLibraryLoaded) {
            MouseManagerCore.sendHexRelData(mouseClick, sx, sy, lx, ly);
        } else {
            MouseManager.sendHexRelData(mouseClick, sx, sy, lx, ly);
        }
    }

    public static void releaseMSRelData() {
        if (useCoreImplementation && coreLibraryLoaded) {
            MouseManagerCore.releaseMSRelData();
        } else {
            MouseManager.releaseMSRelData();
        }
    }

    public static void handleDoubleClickRel() {
        if (useCoreImplementation && coreLibraryLoaded) {
            MouseManagerCore.releaseMSRelData();
        } else {
            MouseManager.handleDoubleClickRel();
        }
    }

    public static void sendModifierPress(String modifierKey) {
        KeyBoardManager.sendModifierPress(modifierKey);
    }

    public static void sendModifierRelease() {
        KeyBoardManager.sendModifierRelease();
    }

    // ====== Utility Methods ======

    public static void setKeyBoardLanguage() {
        KeyBoardManager.setKeyBoardLanguage();
    }

    public static String getFunctionKey(android.view.KeyEvent event, int keyCode) {
        return KeyBoardManager.getFunctionKey(event, keyCode);
    }

    public static String getKeyName(int keyCode) {
        return KeyBoardManager.getKeyName(keyCode);
    }

    // ====== Static Field Access ======

    public static int getScreenWidth() {
        return MouseManager.screenWidth;
    }

    public static int getScreenHeight() {
        return MouseManager.screenHeight;
    }
}
