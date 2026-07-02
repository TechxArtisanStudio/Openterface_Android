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

    private static final String TAG = "OP-KB";
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
        // Special printable characters: use their map key names
        switch (c) {
            case ' ':  return "SPACE";
            case '\n': return "ENTER";
            case '\t': return "TAB";
            case '\r': return "ENTER";
        }
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
        if (c >= 32 && c <= 126) {
            return String.valueOf(c);
        }
        // Non-printable or non-ASCII: no mapping
        return null;
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
     * Both press and release packets are sent via UsbDeviceManager.writeKeyboardData()
     * which auto-routes to FE0C bulk transfer (1A86:FE0C, Mini-KVM v2) or
     * serial port.write() (7523, Mini-KVM v1).
     *
     * Matches the proven KeyCMD pattern (KeyboardHidTransport.sendKeyReport) exactly:
     *   57 AB 00 02 08 <mod> 00 <key0> 00 00 00 00 00 <checksum>
     */
    private static void sendKey(String modifier, String keyName) throws InterruptedException {
        try {
            // Resolve the scan code for this key (respects language-specific layouts)
            String keyCode = KeyBoardManager.getCurrentKeyCodeMap().get(keyName);
            if (keyCode == null) {
                Log.e(TAG, "❌ No keyCode found for keyName: '" + keyName + "'");
                return;
            }

            // Build the PRESS packet (matches KeyCMD's KeyboardHidTransport.sendKeyReport exactly):
            //   57 AB 00 02 08 <mod> 00 <key[0]> 00 00 00 00 00
            //   = header(5) + mod(1) + reserved(1) + key0(1) + key1..key5(5) = 13 bytes
            //   + checksum(1) = 14 bytes total
            String pressData = String.format(
                    "57AB000208%02X00%s0000000000",
                    Integer.parseInt(modifier, 16),
                    keyCode);
            pressData = pressData + CH9329Function.makeChecksum(pressData);
            byte[] pressBytes = CH9329Function.hexStringToByteArray(pressData);

            // Build the RELEASE packet (matches KeyCMD's sendAllKeysReleased exactly):
            //   57 AB 00 02 08 00 00 00 00 00 00 00 <0C = checksum>
            String releaseData = "57AB00020800000000000000000C";
            byte[] releaseBytes = CH9329Function.hexStringToByteArray(releaseData);

            // Use UsbDeviceManager.writeKeyboardData() for all writes — it auto-routes
            // to FE0C bulk transfer (1A86:FE0C) or CH340 with bulkTransfer fallback (1A86:7523).
            // Direct port.write() is NOT used because testConnection() fails on some CH340 chips.
            com.openterface.AOS.serial.UsbDeviceManager mgr = KeyBoardManager.getUsbDeviceManager();

            Log.d(TAG, "📤 Press key='" + keyName + "' mod=0x" + modifier +
                  " keyCode=" + keyCode + " packet=" + pressData +
                  " viaManager=" + (mgr != null));

            if (mgr == null) {
                Log.e(TAG, "❌ UsbDeviceManager is null — cannot send key press");
                return;
            }

            // Send PRESS via proper device-aware write path
            boolean pressOk = mgr.writeKeyboardData(pressBytes, 50);
            if (!pressOk) {
                Log.e(TAG, "❌ PRESS write returned false for key='" + keyName + "'");
                return;
            }

            // Brief hold so CH9329 registers the press
            Thread.sleep(15);

            Log.d(TAG, "🔼 Release key='" + keyName + "' packet=" + releaseData);

            // Send RELEASE via same path
            boolean releaseOk = mgr.writeKeyboardData(releaseBytes, 50);
            if (!releaseOk) {
                Log.e(TAG, "❌ RELEASE write returned false for key='" + keyName + "'");
            }

            // Brief gap before the next key
            Thread.sleep(20);

            Log.d(TAG, "✅ Done key='" + keyName + "'");

        } catch (Exception e) {
            Log.e(TAG, "❌ Error: " + e.getMessage(), e);
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
