package com.openterface.AOS.utils;

/**
 * Validates text input before sending via HID.
 * Based on KeyCMD's ImeComposeSendGate validation logic.
 */
public class TextValidator {

    /**
     * Warning threshold for long text (advisory only, not a hard block).
     * Same as KeyCMD's LONG_TEXT_WARNING_THRESHOLD.
     */
    public static final int LONG_TEXT_WARNING_THRESHOLD = 300;

    public static class ValidationResult {
        public final boolean isConnected;
        public final boolean isEmpty;
        public final boolean hasNonAscii;
        public final boolean hasLengthRisk;
        public final int charCount;

        public ValidationResult(boolean isConnected, boolean isEmpty,
                               boolean hasNonAscii, boolean hasLengthRisk, int charCount) {
            this.isConnected = isConnected;
            this.isEmpty = isEmpty;
            this.hasNonAscii = hasNonAscii;
            this.hasLengthRisk = hasLengthRisk;
            this.charCount = charCount;
        }

        /**
         * Check if send is allowed (no hard blocks).
         */
        public boolean canSend() {
            return isConnected && !isEmpty;
        }

        /**
         * Get error message if send is blocked, or null if allowed.
         */
        public String getErrorMessage() {
            if (!isConnected) {
                return "No device connected. Please connect a device first.";
            }
            if (isEmpty) {
                return "Text is empty. Please enter some text to send.";
            }
            return null;
        }

        /**
         * Check if there are any warnings (non-ASCII or long text).
         */
        public boolean hasWarnings() {
            return hasNonAscii || hasLengthRisk;
        }

        /**
         * Get warning message if there are warnings, or null if no warnings.
         */
        public String getWarningMessage() {
            if (!hasWarnings()) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            if (hasNonAscii) {
                sb.append("Text contains non-ASCII characters that may not be received correctly by the target device.\n");
            }
            if (hasLengthRisk) {
                sb.append("Long text (").append(charCount).append(" chars) may take time to send or lose characters.\n");
            }
            return sb.toString().trim();
        }
    }

    /**
     * Validate text before sending.
     * Based on KeyCMD's ImeComposeSendGate.assess() logic.
     *
     * @param text the text to validate
     * @param isConnected whether device is connected
     * @return ValidationResult with validation results
     */
    public static ValidationResult validate(String text, boolean isConnected) {
        boolean isEmpty = text == null || text.trim().isEmpty();
        boolean hasNonAscii = textContainsNonAscii(text);
        boolean hasLengthRisk = textLengthHasRisk(text);
        int charCount = text != null ? text.length() : 0;

        return new ValidationResult(isConnected, isEmpty, hasNonAscii, hasLengthRisk, charCount);
    }

    /**
     * Check if text contains non-ASCII characters (code points > 127).
     * Same as KeyCMD's textContainsNonAscii().
     */
    public static boolean textContainsNonAscii(String text) {
        if (text == null) {
            return false;
        }
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            if (cp > 127) {
                return true;
            }
            i += Character.charCount(cp);
        }
        return false;
    }

    /**
     * Check if text length exceeds warning threshold.
     * Same as KeyCMD's textLengthHasRisk().
     */
    public static boolean textLengthHasRisk(String text) {
        return text != null && text.length() >= LONG_TEXT_WARNING_THRESHOLD;
    }

    /**
     * Convenience method: check if text contains non-ASCII characters.
     * Same as textContainsNonAscii().
     */
    public static boolean hasNonAscii(String text) {
        return textContainsNonAscii(text);
    }

    /**
     * Convenience method: check if text is too long (>= LONG_TEXT_WARNING_THRESHOLD).
     */
    public static boolean isTooLong(String text) {
        return textLengthHasRisk(text);
    }
}
