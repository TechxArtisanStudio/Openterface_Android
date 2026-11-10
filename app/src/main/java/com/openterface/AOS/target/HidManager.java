package com.openterface.AOS.target;

import android.util.Log;

import com.openterface.AOS.jni.KeymodJNI;
import com.openterface.AOS.serial.CH9329Function;
import com.openterface.AOS.serial.UsbDeviceManager;

/**
 * Unified HID Manager that can switch between Java and Core implementations.
 */
public class HidManager {
    private static final String TAG = "OP-HID";

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

    /**
     * Set mouse movement speed multiplier for relative mode.
     * Does NOT change the number of packets sent — only scales the delta values.
     * Applied to BOTH Java and Core implementations so the setting is preserved
     * when switching between them.
     * @param multiplier 0.25f to 3.0f (1.0f = normal speed)
     */
    public static void setMouseSpeedMultiplier(float multiplier) {
        MouseManager.setMouseSpeedMultiplier(multiplier);
        MouseManagerCore.setMouseSpeedMultiplier(multiplier);
    }

    public static void width_height(int width, int height) {
        if (useCoreImplementation && coreLibraryLoaded) {
            MouseManagerCore.width_height(width, height);
        } else {
            MouseManager.width_height(width, height);
        }
    }

    public static void sendHexAbsData(float x, float y) {
        // Per-frame log with a stack trace: this runs on every absolute move (60~120
        // times per second), so it must stay behind the shared debug switch.
        if (CH9329Function.DEBUG_SEND_LOG) {
            Log.d("OP-HID", "sendHexAbsData CALLED: x=" + x + " y=" + y
                    + " btMouseConnected=" + com.openterface.AOS.serial.BtMouseInputHandler.isBtMouseConnected(),
                    new Exception("Stack trace"));
        }
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

    /**
     * Absolute move that explicitly releases any held button. Use it at the end of a
     * click, a drag or a remote client click — a plain move keeps the held mask alive.
     */
    public static void sendHexAbsReleaseData(float x, float y) {
        if (useCoreImplementation && coreLibraryLoaded) {
            MouseManagerCore.sendHexAbsReleaseData(x, y);
        } else {
            MouseManager.sendHexAbsReleaseData(x, y);
        }
    }

    /**
     * Set the gesture buttons (tap, double-tap drag, BT mouse drag, remote client).
     * Pass null or "SecNullData" to release them; locked keycaps stay pressed.
     * Both implementations are updated so the mask survives a switch between them,
     * in the same way as setMouseSpeedMultiplier().
     */
    public static void setHeldButton(String button) {
        MouseManager.setHeldButton(button);
        MouseManagerCore.setHeldButtons(coreButtonMask(button));
    }

    /** Add a button that stays pressed until it is unlocked (keycap long press). */
    public static void lockButton(String button) {
        MouseManager.lockButton(button);
        MouseManagerCore.lockButtons(coreButtonMask(button));
    }

    /** Drop one locked button (the keycap was tapped again to unlock). */
    public static void unlockButton(String button) {
        MouseManager.unlockButton(button);
        MouseManagerCore.unlockButtons(coreButtonMask(button));
    }

    /** Drop every gesture and locked button: disconnect, rotation, activity stop. */
    public static void unlockAll() {
        MouseManager.unlockAll();
        MouseManagerCore.unlockAll();
    }

    /** Buttons that are currently kept pressed by a locked keycap. */
    public static int getLockedMask() {
        return (useCoreImplementation && coreLibraryLoaded)
                ? MouseManagerCore.getLockedButtons()
                : MouseManager.getLockedMask();
    }

    private static int coreButtonMask(String button) {
        if (button == null) return KeymodJNI.MS_BTN_NONE;
        switch (button) {
            case "SecLeftData": return KeymodJNI.MS_BTN_LEFT;
            case "SecRightData": return KeymodJNI.MS_BTN_RIGHT;
            case "SecMiddleData": return KeymodJNI.MS_BTN_MIDDLE;
            case "SecBackData": return KeymodJNI.MS_BTN_BACK;
            case "SecForwardData": return KeymodJNI.MS_BTN_FORWARD;
            default: return KeymodJNI.MS_BTN_NONE;
        }
    }

    /**
     * Send BT mouse absolute move with pre-converted HID coordinates.
     * Bypasses Handler's view→HID scaling to avoid double-scaling.
     * @param hidX HID X coordinate [0, 4095]
     * @param hidY HID Y coordinate [0, 4095]
     */
    public static void sendBtMouseHidAbsData(float hidX, float hidY) {
        if (useCoreImplementation && coreLibraryLoaded) {
            MouseManagerCore.sendBtMouseHidAbsData(hidX, hidY);
        } else {
            MouseManager.sendBtMouseHidAbsData(hidX, hidY);
        }
    }

    /**
     * Send BT mouse button click with pre-converted HID coordinates.
     * Bypasses Handler's view→HID scaling to avoid double-scaling.
     * @param mouseClick Button key (e.g., "SecLeftData")
     * @param hidX HID X coordinate [0, 4095]
     * @param hidY HID Y coordinate [0, 4095]
     */
    public static void sendBtMouseHidAbsButtonClickData(String mouseClick, float hidX, float hidY) {
        if (useCoreImplementation && coreLibraryLoaded) {
            MouseManagerCore.sendBtMouseHidAbsButtonClickData(mouseClick, hidX, hidY);
        } else {
            MouseManager.sendBtMouseHidAbsButtonClickData(mouseClick, hidX, hidY);
        }
    }

    /**
     * BT mouse release with pre-converted HID coordinates [0, 4095]. Clears the held
     * button, so a finished drag really ends in the host.
     */
    public static void sendBtMouseHidAbsReleaseData(float hidX, float hidY) {
        if (useCoreImplementation && coreLibraryLoaded) {
            MouseManagerCore.sendBtMouseHidAbsReleaseData(hidX, hidY);
        } else {
            MouseManager.sendBtMouseHidAbsReleaseData(hidX, hidY);
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
            MouseManagerCore.releaseMSAbsData(0f, 0f);
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
        // Called on every relative move frame, so it stays behind the debug switch.
        if (CH9329Function.DEBUG_SEND_LOG) {
            Log.d("OP-HID", "sendHexRelData CALLED: click=" + mouseClick
                    + " cur=(" + sx + "," + sy + ") prev=(" + lx + "," + ly + ")");
        }
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
