package com.openterface.AOS.utils;

import android.util.Log;

import com.openterface.AOS.serial.CH9329Function;
import com.openterface.AOS.serial.UsbDeviceManager;
import com.openterface.AOS.target.CH9329MSKBMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sends plain text (and optional &lt;TAG&gt; tokens) as HID keystrokes via the CH9329 chip.
 * Replaces the broken charToScanCode() / sendTextViaHID() in MainActivity.
 *
 * <p>Mirror of KeyCMD's HidTextKeystrokeSender, adapted for the AOS KeyBoardManager / CH9329MSKBMap.
 */
public final class TextHidSender {

    private static final String TAG = "TextHidSender";

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

    // --- Character mapping (mirrors KeyCMD's HidTextKeystrokeSender.mapCharToHidCode) ---

    /** Map a printable ASCII char to its HID usage code. Returns -1 if no mapping. */
    public static int mapCharToHidCode(char c) {
        if (c >= 'a' && c <= 'z') return 4 + (c - 'a');
        if (c >= 'A' && c <= 'Z') return 4 + (c - 'A');
        if (c >= '1' && c <= '9') return 30 + (c - '1');
        if (c == '0') return 39;
        switch (c) {
            case ' ':  return 0x2C;  // space
            case '\n': return 0x28;  // enter
            case '\t': return 0x2B;  // tab
            case '-': case '_': return 0x2D;
            case '=': case '+': return 0x2E;
            case '[': case '{': return 0x2F;
            case ']': case '}': return 0x30;
            case '\\':case '|': return 0x31;
            case ';': case ':': return 0x33;
            case '\'':case '"': return 0x34;
            case ',': case '<': return 0x36;
            case '.': case '>': return 0x37;
            case '/': case '?': return 0x38;
            case '`': case '~': return 0x35;
            case '!': return 0x1E;
            case '@': return 0x1F;
            case '#': return 0x20;
            case '$': return 0x21;
            case '%': return 0x22;
            case '^': return 0x23;
            case '&': return 0x24;
            case '*': return 0x25;
            case '(': return 0x26;
            case ')': return 0x27;
            default: return -1;
        }
    }

    /** Returns true if the character needs Shift modifier (uppercase, symbols). */
    public static boolean needsShift(char c) {
        if (Character.isUpperCase(c)) return true;
        return "~!@#$%^&*()_+{}|:\"<>?".indexOf(c) >= 0;
    }

    // --- Token parsing ---

    /** Special key tokens mapped to HID usage codes. */
    public static int specialTokenToHidCode(String token) {
        if (!token.startsWith("<") || !token.endsWith(">") || token.startsWith("</")) {
            return -1;
        }
        String content = token.substring(1, token.length() - 1).toUpperCase(Locale.ROOT);
        switch (content) {
            case "ENTER":      return 0x28;
            case "ESC":        return 0x29;
            case "BACK":
            case "BACKSPACE":  return 0x2A;
            case "TAB":        return 0x2B;
            case "SPACE":      return 0x2C;
            case "RIGHT":      return 0x4F;
            case "LEFT":       return 0x50;
            case "DOWN":       return 0x51;
            case "UP":         return 0x52;
            case "HOME":       return 0x4A;
            case "END":        return 0x4D;
            case "PAGEUP":
            case "PGUP":       return 0x4B;
            case "PAGEDOWN":
            case "PGDN":       return 0x4E;
            case "INSERT":     return 0x49;
            case "DELETE":
            case "DEL":        return 0x4C;
            case "F1":  return 0x3A; case "F7":  return 0x40;
            case "F2":  return 0x3B; case "F8":  return 0x41;
            case "F3":  return 0x3C; case "F9":  return 0x42;
            case "F4":  return 0x3D; case "F10": return 0x43;
            case "F5":  return 0x3E; case "F11": return 0x44;
            case "F6":  return 0x3F; case "F12": return 0x45;
            default: return -1;
        }
    }

    /**
     * Tokenize input into &lt;TAG&gt; tokens and individual characters.
     * Tag tokens are &lt;CTRL&gt;, &lt;SHIFT&gt;, &lt;ALT&gt;, &lt;WIN&gt;, &lt;/CTRL&gt; etc.
     */
    public static List<String> tokenizeInput(String text) {
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
     * Count total HID send units for progress display.
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
            int specialHid = specialTokenToHidCode(token);
            if (specialHid > 0) { units++; continue; }
            for (int ci = 0; ci < token.length(); ) {
                int cp = token.codePointAt(ci);
                ci += Character.charCount(cp);
                if (cp > 0x7E) {
                    // Non-ASCII: skip (no HID mapping on CH9329)
                    continue;
                }
                int hidCode = mapCharToHidCode((char) cp);
                if (hidCode >= 0) units++;
            }
        }
        return units;
    }

    private static boolean isDelayTag(String token) {
        return token.equals("<DELAY1S>") || token.equals("<DELAY2S>")
                || token.equals("<DELAY5S>") || token.equals("<DELAY10S>");
    }

    // --- HID sending via CH9329 ---

    private static final String RELEASE_DATA = CH9329MSKBMap.getKeyCodeMap().get("release");

    /**
     * Send a single key press+release with the given modifier byte and HID usage code.
     * This is the synchronous core — caller must be on a background thread.
     */
    private static void sendHidKey(int modifier, int hidCode) throws InterruptedException {
        try {
            String modHex = String.format("%02X", modifier & 0xFF);
            String keyCodeHex = String.format("%02X", hidCode & 0xFF);
            String dataNull = CH9329MSKBMap.DataNull().get("DataNull");  // "00"

            String sendKBData = CH9329MSKBMap.getKeyCodeMap().get("prefix1")
                    + CH9329MSKBMap.getKeyCodeMap().get("prefix2")
                    + CH9329MSKBMap.getKeyCodeMap().get("address")
                    + CH9329MSKBMap.CmdData().get("CmdKB_HID")
                    + CH9329MSKBMap.DataLen().get("DataLenKB")
                    + modHex
                    + dataNull
                    + keyCodeHex
                    + dataNull + dataNull + dataNull + dataNull + dataNull;

            sendKBData = sendKBData + CH9329Function.makeChecksum(sendKBData);
            byte[] bytes = CH9329Function.hexStringToByteArray(sendKBData);

            if (UsbDeviceManager.port != null && UsbDeviceManager.port.isOpen()) {
                UsbDeviceManager.port.write(bytes, 200);
            } else {
                Log.w(TAG, "USB port not available for HID send");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending HID key: " + e.getMessage());
        }
    }

    /** Send a release (all keys up). Synchronous, caller on background thread. */
    private static void sendHidRelease() {
        try {
            byte[] bytes = CH9329Function.hexStringToByteArray(RELEASE_DATA);
            if (UsbDeviceManager.port != null && UsbDeviceManager.port.isOpen()) {
                UsbDeviceManager.port.write(bytes, 200);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending HID release: " + e.getMessage());
        }
    }

    /**
     * Send text as HID keystrokes on the caller thread (must be a background thread).
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
            int specialHid = specialTokenToHidCode(token);
            if (specialHid > 0) {
                sendHidKey(activeMods, specialHid);
                Thread.sleep(30);
                sendHidRelease();
                Thread.sleep(10);
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
                int hidCode = mapCharToHidCode(c);
                if (hidCode < 0) {
                    Log.w(TAG, "No HID code for char: '" + c + "' (U+" + Integer.toHexString(cp) + ")");
                    continue;
                }

                int mods = (activeMods != 0) ? activeMods : (needsShift(c) ? 0x02 : 0x00);
                sendHidKey(mods, hidCode);
                Thread.sleep(30);
                sendHidRelease();
                Thread.sleep(10);
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
