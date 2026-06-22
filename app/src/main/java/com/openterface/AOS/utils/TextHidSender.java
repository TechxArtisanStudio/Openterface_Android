package com.openterface.AOS.utils;

import android.util.Log;

import com.openterface.AOS.serial.CH9329Function;
import com.openterface.AOS.serial.UsbDeviceManager;
import com.openterface.AOS.target.CH9329MSKBMap;
import com.openterface.AOS.target.KeyBoardManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sends plain text (and optional &lt;TAG&gt; tokens) as keystrokes via serial port to CH9329 chip.
 * Uses direct serial port writes for synchronous sequential sending.
 *
 * <p>Key characters (like 'A', '1', '!') are looked up in CH9329MSKBMap.
 * Modifier keys (Shift for uppercase/symbols) are passed as functionKey parameter.
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
        /**
         * @param completed send units finished so far, in range 1..total
         * @param total from {@link #countSendUnits}
         */
        void onProgress(int completed, int total);
    }

    private TextHidSender() {}

    // --- Character to keyName mapping ---

    /**
     * Map a character to its keyName in CH9329MSKBMap.
     * Returns null if no mapping exists.
     * The keyName is the character itself for most cases.
     */
    private static String charToKeyName(char c) {
        // For letters, use the character directly (both upper and lower case are in the map)
        if (Character.isLetter(c)) {
            return String.valueOf(c);
        }
        // For digits
        if (Character.isDigit(c)) {
            return String.valueOf(c);
        }
        // For special characters, check if they exist in the map
        String keyName = String.valueOf(c);
        Map<Object, String> keyCodeMap = CH9329MSKBMap.getKeyCodeMap();
        if (keyCodeMap.containsKey(keyName)) {
            return keyName;
        }
        return null;
    }

    /** Returns true if the character needs Shift modifier. */
    private static boolean needsShift(char c) {
        // Uppercase letters need shift
        if (Character.isUpperCase(c)) return true;
        // These symbols need shift
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
            case "ENTER": return "ENTER";
            case "ESC": return "Esc";
            case "BACK":
            case "BACKSPACE": return "BACK";
            case "TAB": return "TAB";
            case "SPACE": return "SPACE";
            case "RIGHT": return "DPAD_RIGHT";
            case "LEFT": return "DPAD_LEFT";
            case "DOWN": return "DPAD_DOWN";
            case "UP": return "DPAD_UP";
            case "HOME": return "MOVE_HOME";
            case "END": return "MOVE_END";
            case "PAGEUP":
            case "PGUP": return "PAGE_UP";
            case "PAGEDOWN":
            case "PGDN": return "PAGE_DOWN";
            case "INSERT": return "INSERT";
            case "DELETE":
            case "DEL": return "Delete";
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

    /**
     * Count total send units for progress display.
     * Modifier open/close tags and delay tags contribute 0.
     */
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
                if (cp > 0x7E) {
                    continue;
                }
                char c = (char) cp;
                if (charToKeyName(c) != null) {
                    units++;
                }
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
     * Send a key press command directly to serial port (synchronous).
     * This builds the HID command and writes to port without spawning new threads.
     */
    private static void sendKeyPressToPort(String functionKey, String keyName) throws IOException {
        if (UsbDeviceManager.port == null || !UsbDeviceManager.port.isOpen()) {
            Log.w(TAG, "Serial port not available");
            return;
        }

        Map<Object, String> keyCodeMap = CH9329MSKBMap.getKeyCodeMap();
        String keyCode = keyCodeMap.get(keyName);
        if (keyCode == null) {
            Log.w(TAG, "No keyCode for: " + keyName);
            return;
        }

        // Build the HID command: prefix + address + command + length + modifier + null + keyCode + 5xnull
        String sendKBData = keyCodeMap.get("prefix1")
                + keyCodeMap.get("prefix2")
                + keyCodeMap.get("address")
                + CH9329MSKBMap.CmdData().get("CmdKB_HID")
                + CH9329MSKBMap.DataLen().get("DataLenKB")
                + functionKey
                + CH9329MSKBMap.DataNull().get("DataNull")
                + keyCode
                + CH9329MSKBMap.DataNull().get("DataNull")
                + CH9329MSKBMap.DataNull().get("DataNull")
                + CH9329MSKBMap.DataNull().get("DataNull")
                + CH9329MSKBMap.DataNull().get("DataNull")
                + CH9329MSKBMap.DataNull().get("DataNull");

        // Add checksum
        sendKBData = sendKBData + CH9329Function.makeChecksum(sendKBData);

        // Convert to bytes and send
        byte[] sendKBDataBytes = CH9329Function.hexStringToByteArray(sendKBData);
        UsbDeviceManager.port.write(sendKBDataBytes, 100);
    }

    /**
     * Send key release (empty keyboard) directly to serial port.
     */
    private static void sendKeyReleaseToPort() throws IOException {
        if (UsbDeviceManager.port == null || !UsbDeviceManager.port.isOpen()) {
            return;
        }

        // Release command is the pre-built release string
        String releaseData = CH9329MSKBMap.getKeyCodeMap().get("release");
        byte[] releaseBytes = CH9329Function.hexStringToByteArray(releaseData);
        UsbDeviceManager.port.write(releaseBytes, 100);
    }

    /**
     * Send a single key press+release synchronously.
     * Caller must be on a background thread.
     */
    private static void sendKey(String modifier, String keyName) throws InterruptedException {
        try {
            sendKeyPressToPort(modifier, keyName);
            Thread.sleep(30);  // Hold time
            sendKeyReleaseToPort();
            Thread.sleep(20);  // Gap between keys
        } catch (IOException e) {
            Log.e(TAG, "Error sending key: " + e.getMessage());
        }
    }

    /**
     * Send text as keystrokes on the caller thread (must be a background thread).
     *
     * @param text     text to send (may contain &lt;TAG&gt; tokens)
     * @param cancel   if non-null, polled between steps; true aborts
     * @param progress optional progress callback; invoked on caller thread
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

            // Close modifier tags
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
                // Determine modifier for special key
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

                // Determine modifier: use activeMods if set, otherwise check if shift is needed
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
