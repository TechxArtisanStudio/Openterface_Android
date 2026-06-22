package com.openterface.AOS.utils;

import android.util.Log;

import com.openterface.AOS.serial.CH9329Function;
import com.openterface.AOS.serial.UsbDeviceManager;
import com.openterface.AOS.target.CH9329MSKBMap;
import com.openterface.AOS.target.KeyBoardManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sends plain text as keystrokes via serial port to CH9329 chip.
 *
 * <p>Uses {@link KeyBoardManager} to get the correct key code map (handles keyboard language).
 * Each character is sent as: key press → hold → key release, all on a single background thread.
 */
public final class TextHidSender {

    private static final String TAG = "TextHidSender";
    private static final String MOD_NONE = "00";
    private static final String MOD_SHIFT = "02";

    public enum Result {
        COMPLETED,
        CANCELLED,
    }

    /** Invoked from the send thread after each completed send unit. */
    @FunctionalInterface
    public interface SendProgressListener {
        void onProgress(int completed, int total);
    }

    private TextHidSender() {}

    /**
     * Map a character to its keyName for CH9329MSKBMap lookup.
     * Returns null if no mapping exists.
     */
    private static String charToKeyName(char c) {
        // Letters: use the character directly (A-Z, a-z are all in the map)
        if (Character.isLetter(c)) {
            return String.valueOf(c);
        }
        // Digits: use the character directly (0-9)
        if (Character.isDigit(c)) {
            return String.valueOf(c);
        }
        // Special characters: use the character itself
        // CH9329MSKBMap has entries for: ! @ # $ % ^ & * ( ) ` ~ - _ + = [ ] { } | \ ; : ' " , < . > / ?
        String keyName = String.valueOf(c);
        return keyName;
    }

    /** Returns true if the character needs Shift modifier. */
    private static boolean needsShift(char c) {
        if (Character.isUpperCase(c)) return true;
        return "~!@#$%^&*()_+{}|:\"<>?".indexOf(c) >= 0;
    }

    // --- Token parsing ---

    /** Special key tokens mapped to keyNames in CH9329MSKBMap. */
    private static String specialTokenToKeyName(String token) {
        if (!token.startsWith("<") || !token.endsWith(">") || token.startsWith("</")) {
            return null;
        }
        String content = token.substring(1, token.length() - 1).toUpperCase(Locale.ROOT);
        switch (content) {
            case "ENTER":       return "ENTER";
            case "ESC":         return "Esc";
            case "BACK":
            case "BACKSPACE":   return "BACK";
            case "TAB":         return "TAB";
            case "SPACE":       return "SPACE";
            case "RIGHT":       return "DPAD_RIGHT";
            case "LEFT":        return "DPAD_LEFT";
            case "DOWN":        return "DPAD_DOWN";
            case "UP":          return "DPAD_UP";
            case "HOME":        return "Home";
            case "END":         return "End";
            case "PAGEUP":
            case "PGUP":        return "PgUp";
            case "PAGEDOWN":
            case "PGDN":        return "PgDn";
            case "INSERT":      return "Ins";
            case "DELETE":
            case "DEL":         return "DEL";
            case "F1": case "F2": case "F3": case "F4": case "F5": case "F6":
            case "F7": case "F8": case "F9": case "F10": case "F11": case "F12":
                return content;
            default: return null;
        }
    }

    /**
     * Tokenize input into &lt;TAG&gt; tokens and individual characters.
     */
    private static List<String> tokenizeInput(String text) {
        Pattern p = Pattern.compile("</?[A-Z0-9]+>|[\\s\\S]");
        Matcher m = p.matcher(text);
        List<String> tokens = new ArrayList<>();
        while (m.find()) {
            tokens.add(m.group());
        }
        return tokens;
    }

    // --- Count send units ---

    public static int countSendUnits(String text) {
        if (text == null) return 0;
        List<String> tokens = tokenizeInput(text);
        int activeMods = 0;
        int units = 0;
        for (String token : tokens) {
            if (token.startsWith("</") && token.endsWith(">")) {
                activeMods = 0;
                continue;
            }
            switch (token) {
                case "<CTRL>":  activeMods |= 0x01; continue;
                case "<SHIFT>": activeMods |= 0x02; continue;
                case "<ALT>":   activeMods |= 0x04; continue;
                case "<WIN>":   activeMods |= 0x08; continue;
                default: break;
            }
            if (isDelayTag(token)) continue;
            String keyName = specialTokenToKeyName(token);
            if (keyName != null) { units++; continue; }
            for (int ci = 0; ci < token.length(); ) {
                int cp = token.codePointAt(ci);
                ci += Character.charCount(cp);
                if (cp > 0x7E) continue;
                char c = (char) cp;
                if (charToKeyName(c) != null) units++;
            }
        }
        return units;
    }

    private static boolean isDelayTag(String token) {
        return token.equals("<DELAY1S>") || token.equals("<DELAY2S>")
                || token.equals("<DELAY5S>") || token.equals("<DELAY10S>");
    }

    // --- Direct serial port sending ---

    /**
     * Send a single key press+release synchronously on background thread.
     * Uses KeyBoardManager to get the current key code map (handles language).
     */
    private static void sendKey(String modifier, String keyName) throws InterruptedException {
        try {
            // Get the current key code map from KeyBoardManager
            java.util.Map<Object, String> keyCodeMap = CH9329MSKBMap.getKeyCodeMap();

            // Get the scan code for this key
            String keyCode = keyCodeMap.get(keyName);
            if (keyCode == null) {
                Log.w(TAG, "No keyCode for keyName: " + keyName);
                return;
            }

            // Build the HID press command (same as sendKeyboardRequest)
            // Format: prefix1(2) + prefix2(2) + address(2) + cmd(2) + len(2) + modifier(2) + null(2) + keycode(2) + 5*null(2*5)
            String sendKBData = keyCodeMap.get("prefix1")
                    + keyCodeMap.get("prefix2")
                    + keyCodeMap.get("address")
                    + CH9329MSKBMap.CmdData().get("CmdKB_HID")
                    + CH9329MSKBMap.DataLen().get("DataLenKB")
                    + modifier
                    + CH9329MSKBMap.DataNull().get("DataNull")
                    + keyCode
                    + CH9329MSKBMap.DataNull().get("DataNull")
                    + CH9329MSKBMap.DataNull().get("DataNull")
                    + CH9329MSKBMap.DataNull().get("DataNull")
                    + CH9329MSKBMap.DataNull().get("DataNull")
                    + CH9329MSKBMap.DataNull().get("DataNull");

            // Add checksum
            sendKBData = sendKBData + CH9329Function.makeChecksum(sendKBData);
            byte[] pressBytes = CH9329Function.hexStringToByteArray(sendKBData);

            // Send press
            if (UsbDeviceManager.port != null && UsbDeviceManager.port.isOpen()) {
                UsbDeviceManager.port.write(pressBytes, 100);
            }

            // Hold the key briefly so target registers it
            Thread.sleep(50);

            // Send release (same as EmptyKeyboard)
            String releaseKBData = keyCodeMap.get("release");
            if (releaseKBData != null) {
                CH9329Function.ReleaseSendLogData(releaseKBData);
                byte[] releaseBytes = CH9329Function.hexStringToByteArray(releaseKBData);
                if (UsbDeviceManager.port != null && UsbDeviceManager.port.isOpen()) {
                    UsbDeviceManager.port.write(releaseBytes, 100);
                }
            }

            // Small gap between keys
            Thread.sleep(20);

        } catch (Exception e) {
            Log.e(TAG, "Error sending key '" + keyName + "': " + e.getMessage());
        }
    }

    /**
     * Send text as keystrokes. Must be called from a background thread.
     *
     * @param text     text to send (may contain &lt;TAG&gt; tokens like &lt;CTRL&gt;)
     * @param cancel   polled between steps; true aborts
     * @param progress optional progress callback (invoked on caller thread)
     * @return COMPLETED or CANCELLED
     */
    public static Result send(
            String text,
            AtomicBoolean cancel,
            SendProgressListener progress
    ) throws InterruptedException {
        if (text == null || text.isEmpty()) return Result.COMPLETED;

        List<String> tokens = tokenizeInput(text);
        int activeMods = 0;
        final int totalUnits = progress != null ? countSendUnits(text) : 0;
        final int[] completed = progress != null ? new int[]{0} : null;

        for (String token : tokens) {
            if (isCancelled(cancel)) return Result.CANCELLED;

            // Close modifier tags (e.g. </CTRL>)
            if (token.startsWith("</") && token.endsWith(">")) {
                activeMods = 0;
                continue;
            }

            // Open modifier tags
            switch (token) {
                case "<CTRL>":  activeMods |= 0x01; continue;
                case "<SHIFT>": activeMods |= 0x02; continue;
                case "<ALT>":   activeMods |= 0x04; continue;
                case "<WIN>":   activeMods |= 0x08; continue;
            }

            // Delay tags
            if (token.equals("<DELAY1S>"))  { Thread.sleep(1000); continue; }
            if (token.equals("<DELAY2S>"))  { Thread.sleep(2000); continue; }
            if (token.equals("<DELAY5S>"))  { Thread.sleep(5000); continue; }
            if (token.equals("<DELAY10S>")) { Thread.sleep(10000); continue; }

            // Special key tokens like <ENTER>
            String specialKeyName = specialTokenToKeyName(token);
            if (specialKeyName != null) {
                String mod = (activeMods != 0) ? String.format("%02X", activeMods) : MOD_NONE;
                sendKey(mod, specialKeyName);
                reportProgress(progress, totalUnits, completed);
                continue;
            }

            // Character-by-character sending
            for (int ci = 0; ci < token.length(); ) {
                if (isCancelled(cancel)) return Result.CANCELLED;
                int cp = token.codePointAt(ci);
                ci += Character.charCount(cp);

                if (cp > 0x7E) {
                    Log.w(TAG, "Skipping non-ASCII code point U+" + Integer.toHexString(cp));
                    continue;
                }

                char c = (char) cp;
                String keyName = charToKeyName(c);
                if (keyName == null) {
                    Log.w(TAG, "No keyName for char: '" + c + "' (U+" + Integer.toHexString(cp) + ")");
                    continue;
                }

                // Determine modifier
                String mod;
                if (activeMods != 0) {
                    mod = String.format("%02X", activeMods);
                } else if (needsShift(c)) {
                    mod = MOD_SHIFT;
                } else {
                    mod = MOD_NONE;
                }

                sendKey(mod, keyName);
                reportProgress(progress, totalUnits, completed);
            }
        }
        return Result.COMPLETED;
    }

    private static boolean isCancelled(AtomicBoolean cancel) {
        return cancel != null && cancel.get();
    }

    private static void reportProgress(SendProgressListener progress, int totalUnits, int[] completed) {
        if (progress == null || totalUnits <= 0) return;
        completed[0] = Math.min(completed[0] + 1, totalUnits);
        progress.onProgress(completed[0], totalUnits);
    }
}
