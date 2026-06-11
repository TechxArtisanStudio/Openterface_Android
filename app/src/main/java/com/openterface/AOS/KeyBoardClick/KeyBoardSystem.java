/**
* @Title: KeyBoardSystem
* @Package com.openterface.AOS.KeyBoardClick
* @Description:
 * ========================================================================== *
 *                                                                            *
 *    This file is part of the Openterface Mini KVM App Android version       *
 *                                                                            *
 *    Copyright (C) 2024   <info@openterface.com>                             *
 *                                                                            *
 *    This program is free software: you can redistribute it and/or modify    *
 *    it under the terms of the GNU General Public License as published by    *
 *    the Free Software Foundation version 3.                                 *
 *                                                                            *
 *    This program is distributed in the hope that it will be useful, but     *
 *    WITHOUT ANY WARRANTY; without even the implied warranty of              *
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU        *
 *    General Public License for more details.                                *
 *                                                                            *
 *    You should have received a copy of the GNU General Public License       *
 *    along with this program. If not, see <http://www.gnu.org/licenses/>.    *
 *                                                                            *
 * ========================================================================== *
*/
package com.openterface.AOS.KeyBoardClick;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.openterface.AOS.R;
import com.openterface.AOS.activity.MainActivity;
import com.openterface.AOS.target.HidManager;
// import com.openterface.AOS.target.KeyBoardManager;
import com.openterface.AOS.target.KeyBoardMapping;
import com.openterface.AOS.view.KeyPreviewPopup;
import com.openterface.AOS.manager.TargetOsManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KeyBoardSystem {

    private final Button KeyBoard_ShortCut;
    private final Button KeyBoard_Function;
    private final ImageButton KeyBoard_System;
    private final LinearLayout Fragment_KeyBoard_ShortCut;
    private final LinearLayout Fragment_KeyBoard_Function;
    private final LinearLayout Fragment_KeyBoard_System;
    private final Context context;
    private final Button winButton;  // Reference to Win key for dynamic label updates

    private static boolean KeyBoard_Ctrl_Press_state;
    private static boolean KeyBoard_ShIft_Press_state;
    private static boolean KeyBoard_Alt_Press_state;
    private static boolean KeyBoard_Win_Press_state;
    private static boolean KeyBoard_FN_Press_state;  // FN key toggle state
    private final View[] SystemButtons;

    // Letter buttons that need case updates when Shift is toggled
    private static final List<Button> letterButtons = new ArrayList<>();
    // The 10 letter buttons in Row 1 (Q-P) for FN layer switching
    private static final List<Button> row1LetterButtons = new ArrayList<>();
    // The 9 letter buttons in Row 2 (A-L) for FN layer hints
    private static final List<Button> row2LetterButtons = new ArrayList<>();
    // The 8 buttons in Row 3 (Z-/) for FN layer hints
    private static final List<Button> row3LetterButtons = new ArrayList<>();
    private static KeyBoardSystem instance;

    private static KeyboardSettingsManager settingsManager;
    private static KeyPreviewPopup keyPreviewPopup;

    private static final Map<String, KeyBoardMapping> languageMappings = new HashMap<>();
    private static KeyBoardMapping currentMapping;
    private static String currentLanguage = "us";

    public KeyBoardSystem(View rootView) {
        instance = this;
        Fragment_KeyBoard_ShortCut = rootView.findViewById(R.id.Fragment_KeyBoard_ShortCut);
        Fragment_KeyBoard_Function = rootView.findViewById(R.id.Fragment_KeyBoard_Function);
        Fragment_KeyBoard_System = rootView.findViewById(R.id.Fragment_KeyBoard_System);

        KeyBoard_ShortCut = rootView.findViewById(R.id.KeyBoard_ShortCut);
        KeyBoard_Function = rootView.findViewById(R.id.KeyBoard_Function);
        KeyBoard_System = rootView.findViewById(R.id.KeyBoard_System);
        this.context = rootView.getContext();

        // Letter buttons for case switching
        Button[] letters = new Button[]{
                rootView.findViewById(R.id.Q_Button), rootView.findViewById(R.id.W_Button),
                rootView.findViewById(R.id.E_Button), rootView.findViewById(R.id.R_Button),
                rootView.findViewById(R.id.T_Button), rootView.findViewById(R.id.Y_Button),
                rootView.findViewById(R.id.U_Button), rootView.findViewById(R.id.I_Button),
                rootView.findViewById(R.id.O_Button), rootView.findViewById(R.id.P_Button),
                rootView.findViewById(R.id.A_Button), rootView.findViewById(R.id.S_Button),
                rootView.findViewById(R.id.D_Button), rootView.findViewById(R.id.F_Button),
                rootView.findViewById(R.id.G_Button), rootView.findViewById(R.id.H_Button),
                rootView.findViewById(R.id.J_Button), rootView.findViewById(R.id.K_Button),
                rootView.findViewById(R.id.L_Button),
                rootView.findViewById(R.id.Z_Button), rootView.findViewById(R.id.X_Button),
                rootView.findViewById(R.id.C_Button), rootView.findViewById(R.id.V_Button),
                rootView.findViewById(R.id.B_Button), rootView.findViewById(R.id.N_Button),
                rootView.findViewById(R.id.M_Button),
        };
        for (Button b : letters) { if (b != null) letterButtons.add(b); }

        // Row 1 letter buttons (Q-P) for FN layer switching
        Button[] row1Letters = new Button[]{
                rootView.findViewById(R.id.Q_Button), rootView.findViewById(R.id.W_Button),
                rootView.findViewById(R.id.E_Button), rootView.findViewById(R.id.R_Button),
                rootView.findViewById(R.id.T_Button), rootView.findViewById(R.id.Y_Button),
                rootView.findViewById(R.id.U_Button), rootView.findViewById(R.id.I_Button),
                rootView.findViewById(R.id.O_Button), rootView.findViewById(R.id.P_Button),
        };
        for (Button b : row1Letters) { if (b != null) row1LetterButtons.add(b); }

        // Row 2 letter buttons (A-L) for FN layer hints
        Button[] row2Letters = new Button[]{
                rootView.findViewById(R.id.A_Button), rootView.findViewById(R.id.S_Button),
                rootView.findViewById(R.id.D_Button), rootView.findViewById(R.id.F_Button),
                rootView.findViewById(R.id.G_Button), rootView.findViewById(R.id.H_Button),
                rootView.findViewById(R.id.J_Button), rootView.findViewById(R.id.K_Button),
                rootView.findViewById(R.id.L_Button),
        };
        for (Button b : row2Letters) { if (b != null) row2LetterButtons.add(b); }

        // Row 3 letter buttons (Z-/) for FN layer hints
        Button[] row3Letters = new Button[]{
                rootView.findViewById(R.id.Z_Button), rootView.findViewById(R.id.X_Button),
                rootView.findViewById(R.id.C_Button), rootView.findViewById(R.id.V_Button),
                rootView.findViewById(R.id.B_Button), rootView.findViewById(R.id.N_Button),
                rootView.findViewById(R.id.M_Button), rootView.findViewById(R.id.Key_Slash),
        };
        for (Button b : row3Letters) { if (b != null) row3LetterButtons.add(b); }

        SystemButtons = new View[]{
            // Row 1: 10 letters Q-P (or numbers when FN active)
            rootView.findViewById(R.id.Q_Button),
            rootView.findViewById(R.id.W_Button),
            rootView.findViewById(R.id.E_Button),
            rootView.findViewById(R.id.R_Button),
            rootView.findViewById(R.id.T_Button),
            rootView.findViewById(R.id.Y_Button),
            rootView.findViewById(R.id.U_Button),
            rootView.findViewById(R.id.I_Button),
            rootView.findViewById(R.id.O_Button),
            rootView.findViewById(R.id.P_Button),

            // Row 2: Tab + A-L + Backspace
            rootView.findViewById(R.id.Key_Tab),
            rootView.findViewById(R.id.A_Button),
            rootView.findViewById(R.id.S_Button),
            rootView.findViewById(R.id.D_Button),
            rootView.findViewById(R.id.F_Button),
            rootView.findViewById(R.id.G_Button),
            rootView.findViewById(R.id.H_Button),
            rootView.findViewById(R.id.J_Button),
            rootView.findViewById(R.id.K_Button),
            rootView.findViewById(R.id.L_Button),
            rootView.findViewById(R.id.Key_Backspace),

            // Row 3: Shift + Z-M / + Enter
            rootView.findViewById(R.id.Key_LeftShift),
            rootView.findViewById(R.id.Z_Button),
            rootView.findViewById(R.id.X_Button),
            rootView.findViewById(R.id.C_Button),
            rootView.findViewById(R.id.V_Button),
            rootView.findViewById(R.id.B_Button),
            rootView.findViewById(R.id.N_Button),
            rootView.findViewById(R.id.M_Button),
            rootView.findViewById(R.id.Key_Slash),
            rootView.findViewById(R.id.Enter_Button),

            // Row 4: FN + Ctrl + Space + Alt + Win
            rootView.findViewById(R.id.Key_FN),
            rootView.findViewById(R.id.Key_Ctrl),
            rootView.findViewById(R.id.Space_Button),
            rootView.findViewById(R.id.Key_Alt),
            rootView.findViewById(R.id.Key_Win),
        };

        languageMappings.put("us", new KeyMapConfig_Us());
        languageMappings.put("de", new KeyMapConfig_De());
        currentMapping = languageMappings.get("us");
        winButton = rootView.findViewById(R.id.Key_Win);
        applyTargetOsLabels();
        SystemButtonListeners();
    }

    /**
     * Get the shared instance for external modifier buttons to call.
     */
    public static KeyBoardSystem getInstance() { return instance; }

    /**
     * Get the Win button reference for external label updates.
     */
    public Button getWinButton() { return winButton; }

    /**
     * Update Win key label based on Target OS setting.
     * macOS: "Cmd", Windows: "Win", Linux: "Super"
     */
    public void applyTargetOsLabels() {
        if (winButton == null) return;
        TargetOsManager manager = TargetOsManager.getInstance();
        TargetOsManager.TargetOS os = manager.getCurrentOs();
        winButton.setText(os.getKeyLabel());
        Log.d("KeyBoardSystem", "Applied Target OS: " + os.getName() + ", Win label: " + os.getKeyLabel());
    }

    /**
     * Refresh Row 1 buttons based on FN state.
     * When FN is active, show F1-F10; otherwise show letters Q-P.
     */
    public static void refreshRow1Buttons() {
        if (instance == null) return;

        String[] fnLabels = {"F1", "F2", "F3", "F4", "F5", "F6", "F7", "F8", "F9", "F10"};
        String[] letterLabels = {"Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"};

        for (int i = 0; i < row1LetterButtons.size() && i < 10; i++) {
            Button btn = row1LetterButtons.get(i);
            if (btn != null) {
                if (KeyBoard_FN_Press_state) {
                    btn.setText(fnLabels[i]);
                } else {
                    // Apply shift state for letters
                    btn.setText(KeyBoard_ShIft_Press_state ? letterLabels[i] : letterLabels[i].toLowerCase());
                }
            }
        }
    }

    /**
     * Refresh all letter button labels based on current Shift and FN state.
     * Called when Shift or FN is pressed/released/locked.
     */
    public static void refreshLetterButtons() {
        if (instance == null) return;

        // FN layer mappings for display
        Map<Integer, String> fnDisplayMap = new HashMap<>();
        fnDisplayMap.putAll(fnRow1Map);
        fnDisplayMap.putAll(fnRow2Map);
        fnDisplayMap.putAll(fnRow3Map);

        for (Button btn : letterButtons) {
            int id = btn.getId();

            if (KeyBoard_FN_Press_state && fnDisplayMap.containsKey(id)) {
                // FN激活时显示功能键标签
                btn.setText(fnDisplayMap.get(id));
            } else {
                // 正常模式显示字母（受Shift影响）
                String[] mapping = currentMapping.getKeyMappings().get(id);
                if (mapping != null) {
                    btn.setText(KeyBoard_ShIft_Press_state ? mapping[1] : mapping[0]);
                }
            }
        }
    }

    /**
     * Apply pressed visual feedback to a view.
     */
    public static void setButtonPressed(View v, boolean pressed) {
        if (pressed) {
            v.setBackgroundResource(R.drawable.press_button_background);
        } else {
            v.setBackgroundResource(R.drawable.nopress_button_background);
        }
    }

    public static void KeyBoard_Ctrl_Press(Boolean KeyBoard_Ctrl_Press){
        KeyBoard_Ctrl_Press_state = KeyBoard_Ctrl_Press;
    }

    public static void KeyBoard_ShIft_Press(Boolean KeyBoard_ShIft_Press){
        KeyBoard_ShIft_Press_state = KeyBoard_ShIft_Press;
        refreshLetterButtons();
    }

    public static void KeyBoard_Alt_Press(Boolean KeyBoard_Alt_Press){
        KeyBoard_Alt_Press_state = KeyBoard_Alt_Press;
    }

    public static void KeyBoard_Win_Press(Boolean KeyBoard_Win_Press){
        KeyBoard_Win_Press_state = KeyBoard_Win_Press;
    }

    public static void KeyBoard_FN_Press(Boolean KeyBoard_FN_Press){
        KeyBoard_FN_Press_state = KeyBoard_FN_Press;
        refreshLetterButtons();
    }

    public static boolean isFnActive() {
        return KeyBoard_FN_Press_state;
    }

    public static void setKeyboardLanguage(String language) {
        currentLanguage = language;
        currentMapping = languageMappings.get(language);
        if (currentMapping == null) {
            currentLanguage = "us";
            currentMapping = languageMappings.get("us");
        }
    }

    /**
     * Set the KeyPreviewPopup for bubble hints on all key presses.
     */
    public static void setKeyPreviewPopup(KeyPreviewPopup popup) {
        keyPreviewPopup = popup;
    }

    /**
     * Set the KeyboardSettingsManager for sound/vibration feedback.
     */
    public static void setSettingsManager(KeyboardSettingsManager manager) {
        settingsManager = manager;
    }

    /**
     * Play key feedback sound and vibration if enabled.
     */
    private static void playKeyFeedback(View v) {
        if (settingsManager != null) {
            settingsManager.playKeySound();
            settingsManager.vibrate(v);
        }
    }

    /**
     * Show key preview bubble hint for the given key label.
     */
    private static void showKeyPreview(View anchor, String label) {
        if (keyPreviewPopup != null && anchor != null && label != null && !label.isEmpty()) {
            keyPreviewPopup.show(anchor, label);
        }
    }

    private void SystemButtonListeners() {
        for (View view : SystemButtons) {
            if (view == null) continue;
            final boolean[] isKeyPressed = {false};
            final String keyLabel = getViewLabel(view);

            // Check if this is the FN toggle key
            boolean isFn = isFnView(view);

            // Check if this is a modifier key in the keyboard layout
            boolean isModifier = isModifierView(view);

            if (isFn) {
                // FN key: toggle behavior with visual feedback
                setupFnButton(view);
            } else if (isModifier) {
                // Modifier key: use ModifierKeyHelper for press feedback + long-press toggle
                setupModifierButton(view);
            } else {
                // Regular key: press feedback on touch, send key events
                view.setOnTouchListener((v, event) -> {
                    String systemButtonId = getKey(view.getId());

                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            // Visual feedback: show pressed
                            v.setBackgroundResource(R.drawable.press_button_background);
                            if (!isKeyPressed[0]) {
                                handleKeyPress(systemButtonId);
                                isKeyPressed[0] = true;
                                // Sound and vibration feedback
                                playKeyFeedback(v);
                                // Bubble hint
                                showKeyPreview(v, keyLabel);
                            }
                            return true;

                        case MotionEvent.ACTION_MOVE:
                            // Finger moved away from button - release visual
                            if (event.getY() < 0 || event.getY() > v.getHeight() ||
                                event.getX() < 0 || event.getX() > v.getWidth()) {
                                if (isKeyPressed[0]) {
                                    v.setBackgroundResource(R.drawable.nopress_button_background);
                                }
                            }
                            return true;

                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            // Restore background
                            v.setBackgroundResource(R.drawable.nopress_button_background);
                            if (isKeyPressed[0]) {
                                handleKeyRelease();
                                isKeyPressed[0] = false;
                                // Dismiss the key preview bubble immediately
                                if (keyPreviewPopup != null) {
                                    keyPreviewPopup.dismiss();
                                }
                            }
                            return true;
                    }
                    return false;
                });
            }
        }
    }

    /**
     * Get the display label for a keyboard key view.
     */
    private String getViewLabel(View v) {
        if (v instanceof Button) {
            CharSequence text = ((Button) v).getText();
            return text != null ? text.toString() : "";
        }
        if (v instanceof ImageButton) {
            int id = v.getId();
            if (id == R.id.Key_Backspace) return "⌫";
            if (id == R.id.Enter_Button) return "↵";
            if (id == R.id.Key_Up) return "↑";
            if (id == R.id.Key_Down) return "↓";
            if (id == R.id.Key_Left) return "←";
            if (id == R.id.Key_Right) return "→";
            return "";
        }
        return "";
    }

    /**
     * Check if a view is a modifier key (Ctrl, Alt, Win, Shift) in the keyboard layout.
     */
    private boolean isModifierView(View v) {
        int id = v.getId();
        return id == R.id.Key_Ctrl || id == R.id.Key_CtrlGr || id == R.id.Key_Win ||
               id == R.id.Key_Alt || id == R.id.Key_AltGr ||
               id == R.id.Key_LeftShift || id == R.id.Key_RightShift;
    }

    /**
     * Check if a view is the FN toggle key.
     */
    private boolean isFnView(View v) {
        return v.getId() == R.id.Key_FN;
    }

    /**
     * Get display label for a modifier key by its view ID.
     */
    private String getModifierLabel(int id) {
        if (id == R.id.Key_Ctrl || id == R.id.Key_CtrlGr) return "Ctrl";
        if (id == R.id.Key_Win) return "Win";
        if (id == R.id.Key_Alt || id == R.id.Key_AltGr) return "Alt";
        if (id == R.id.Key_LeftShift || id == R.id.Key_RightShift) return "Shift";
        return "";
    }

    /**
     * Set up the FN toggle key button.
     * Tap to toggle FN mode on/off with visual highlight.
     */
    private void setupFnButton(View v) {
        final boolean[] isFnActive = {KeyBoard_FN_Press_state};
        final String fnLabel = "FN";

        // Initial visual state if FN is already active
        if (isFnActive[0]) {
            v.setBackgroundResource(R.drawable.press_button_background);
        }

        v.setOnTouchListener((view, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    playKeyFeedback(view);
                    showKeyPreview(view, fnLabel);
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (keyPreviewPopup != null) {
                        keyPreviewPopup.dismiss();
                    }
                    // Toggle FN state
                    isFnActive[0] = !isFnActive[0];
                    KeyBoard_FN_Press(isFnActive[0]);

                    // Update visual state
                    if (isFnActive[0]) {
                        view.setBackgroundResource(R.drawable.press_button_background);
                    } else {
                        view.setBackgroundResource(R.drawable.nopress_button_background);
                    }
                    return true;
            }
            return false;
        });
    }

    /**
     * Set up a modifier key button with press/hold and long-press swipe-up to lock.
     * Press = activate, Release = deactivate, Long-press+swipe-up = toggle lock.
     */
    private void setupModifierButton(View v) {
        final boolean[] isLocked = {false};
        final float[] startY = {0};
        final Handler lockHandler = new Handler();
        final boolean[] longPressFired = {false};
        final String modLabel = getModifierLabel(v.getId());

        v.setOnTouchListener((view, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startY[0] = event.getRawY();
                    longPressFired[0] = false;
                    // Always show pressed visual
                    view.setBackgroundResource(R.drawable.press_button_background);
                    // Activate if not already locked
                    if (!isLocked[0]) {
                        activateModifier(view, isLocked);
                    }
                    // Sound and vibration feedback
                    playKeyFeedback(view);
                    // Bubble hint
                    showKeyPreview(view, modLabel);
                    // Schedule long press
                    lockHandler.postDelayed(() -> longPressFired[0] = true, LONG_PRESS_MS);
                    return true;

                case MotionEvent.ACTION_MOVE:
                    // Check swipe up after long press
                    float dy = startY[0] - event.getRawY();
                    if (longPressFired[0] && dy > SWIPE_DISTANCE) {
                        if (isLocked[0]) {
                            isLocked[0] = false;
                            deactivateModifier(view, isLocked);
                            view.setBackgroundResource(R.drawable.nopress_button_background);
                        } else {
                            isLocked[0] = true;
                        }
                        longPressFired[0] = false;
                        lockHandler.removeCallbacksAndMessages(null);
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    lockHandler.removeCallbacksAndMessages(null);
                    // Dismiss the key preview bubble for modifier keys
                    if (keyPreviewPopup != null) {
                        keyPreviewPopup.dismiss();
                    }
                    // Only release if not locked
                    if (!isLocked[0]) {
                        view.setBackgroundResource(R.drawable.nopress_button_background);
                        deactivateModifier(view, isLocked);
                    }
                    longPressFired[0] = false;
                    return true;
            }
            return false;
        });
    }

    private static final long LONG_PRESS_MS = 500;
    private static final int SWIPE_DISTANCE = 40;

    /**
     * Activate a modifier key — send modifier byte to USB HID and update internal state.
     */
    private void activateModifier(View v, boolean[] isLocked) {
        int id = v.getId();

        if (id == R.id.Key_Ctrl) {
            KeyBoard_Ctrl_Press(true);
            KeyBoardFunction.KeyBoard_Ctrl_Press(true);
            HidManager.sendModifierPress("Ctrl");
        } else if (id == R.id.Key_CtrlGr) {
            KeyBoard_Ctrl_Press(true);
            KeyBoardFunction.KeyBoard_Ctrl_Press(true);
            HidManager.sendModifierPress("CtrlR");
        } else if (id == R.id.Key_Alt) {
            KeyBoard_Alt_Press(true);
            KeyBoardFunction.KeyBoard_Alt_Press(true);
            HidManager.sendModifierPress("Alt");
        } else if (id == R.id.Key_AltGr) {
            KeyBoard_Alt_Press(true);
            KeyBoardFunction.KeyBoard_Alt_Press(true);
            HidManager.sendModifierPress("AltR");
        } else if (id == R.id.Key_Win) {
            KeyBoard_Win_Press(true);
            KeyBoardFunction.KeyBoard_Win_Press(true);
            HidManager.sendModifierPress("Win");
        } else if (id == R.id.Key_LeftShift) {
            KeyBoard_ShIft_Press(true);
            KeyBoardFunction.KeyBoard_ShIft_Press(true);
            HidManager.sendModifierPress("Shift");
        } else if (id == R.id.Key_RightShift) {
            KeyBoard_ShIft_Press(true);
            KeyBoardFunction.KeyBoard_ShIft_Press(true);
            HidManager.sendModifierPress("ShiftR");
        }
    }

    /**
     * Deactivate a modifier key — release all modifiers on USB HID and update internal state.
     */
    private void deactivateModifier(View v, boolean[] isLocked) {
        int id = v.getId();

        if (id == R.id.Key_Ctrl || id == R.id.Key_CtrlGr) {
            KeyBoard_Ctrl_Press(false);
            KeyBoardFunction.KeyBoard_Ctrl_Press(false);
            HidManager.sendModifierRelease();
        } else if (id == R.id.Key_Alt || id == R.id.Key_AltGr) {
            KeyBoard_Alt_Press(false);
            KeyBoardFunction.KeyBoard_Alt_Press(false);
            HidManager.sendModifierRelease();
        } else if (id == R.id.Key_Win) {
            KeyBoard_Win_Press(false);
            KeyBoardFunction.KeyBoard_Win_Press(false);
            HidManager.sendModifierRelease();
        } else if (id == R.id.Key_LeftShift || id == R.id.Key_RightShift) {
            KeyBoard_ShIft_Press(false);
            KeyBoardFunction.KeyBoard_ShIft_Press(false);
            HidManager.sendModifierRelease();
        }
    }

    /**
     * Map Row 1 letter buttons (Q-P) to F1-F10 when FN is active.
     */
    private static final Map<Integer, String> fnRow1Map = new HashMap<>();
    static {
        fnRow1Map.put(R.id.Q_Button, "F1");
        fnRow1Map.put(R.id.W_Button, "F2");
        fnRow1Map.put(R.id.E_Button, "F3");
        fnRow1Map.put(R.id.R_Button, "F4");
        fnRow1Map.put(R.id.T_Button, "F5");
        fnRow1Map.put(R.id.Y_Button, "F6");
        fnRow1Map.put(R.id.U_Button, "F7");
        fnRow1Map.put(R.id.I_Button, "F8");
        fnRow1Map.put(R.id.O_Button, "F9");
        fnRow1Map.put(R.id.P_Button, "F10");
    }

    /**
     * Map Row 2 letter buttons (A-L) to special keys when FN is active.
     * A=F11, S=F12, then remaining map to other navigation keys.
     */
    private static final Map<Integer, String> fnRow2Map = new HashMap<>();
    static {
        fnRow2Map.put(R.id.A_Button, "F11");
        fnRow2Map.put(R.id.S_Button, "F12");
        fnRow2Map.put(R.id.D_Button, "PrtSc");
        fnRow2Map.put(R.id.F_Button, "ScrLk");
        fnRow2Map.put(R.id.G_Button, "Pause");
        fnRow2Map.put(R.id.H_Button, "Ins");
        fnRow2Map.put(R.id.J_Button, "Home");
        fnRow2Map.put(R.id.K_Button, "PgUp");
        fnRow2Map.put(R.id.L_Button, "Delete");
    }

    /**
     * Map Row 3 letter buttons (Z-M, /) to special keys when FN is active.
     */
    private static final Map<Integer, String> fnRow3Map = new HashMap<>();
    static {
        fnRow3Map.put(R.id.Z_Button, "End");
        fnRow3Map.put(R.id.X_Button, "PgDn");
        fnRow3Map.put(R.id.C_Button, "DPAD_UP");
        fnRow3Map.put(R.id.V_Button, "DPAD_DOWN");
        fnRow3Map.put(R.id.B_Button, "DPAD_LEFT");
        fnRow3Map.put(R.id.N_Button, "DPAD_RIGHT");
        fnRow3Map.put(R.id.M_Button, "TAB");
        fnRow3Map.put(R.id.Key_Slash, "Esc");
    }

    /**
     * Check if an id is an FN-layer mapped button.
     */
    private static boolean isFnLayerKey(int systemButtonId) {
        return fnRow1Map.containsKey(systemButtonId)
            || fnRow2Map.containsKey(systemButtonId)
            || fnRow3Map.containsKey(systemButtonId);
    }

    /**
     * Get FN layer mapping for a button.
     */
    private static String getFnLayerKey(int systemButtonId) {
        if (fnRow1Map.containsKey(systemButtonId)) return fnRow1Map.get(systemButtonId);
        if (fnRow2Map.containsKey(systemButtonId)) return fnRow2Map.get(systemButtonId);
        if (fnRow3Map.containsKey(systemButtonId)) return fnRow3Map.get(systemButtonId);
        return null;
    }

    private String getKey(int systemButtonId) {
        // If FN is active and this is an FN-layer key, send the FN mapping
        if (KeyBoard_FN_Press_state && isFnLayerKey(systemButtonId)) {
            return getFnLayerKey(systemButtonId);
        }

        String[] mapping = currentMapping.getKeyMappings().get(systemButtonId);
        if (mapping != null) {
            return KeyBoard_ShIft_Press_state ? mapping[1] : mapping[0];
        }
        return "";
    }

    private void handleShortcut(String System_buttonId) {
        String SystemKeyCtrlPress;
        String SystemKeyShiftPress;
        String SystemKeyAltPress;
        String SystemKeyWinPress;
        if (KeyBoard_Ctrl_Press_state){
            SystemKeyCtrlPress = "Ctrl";
        }else{
            SystemKeyCtrlPress = "ShortCutKeyCtrlNull";
        }

        if (KeyBoard_ShIft_Press_state){
            SystemKeyShiftPress = "Shift";
        }else{
            SystemKeyShiftPress = "ShortCutKeyShiftNull";
        }

        if (KeyBoard_Alt_Press_state){
            SystemKeyAltPress = "Alt";
        }else{
            SystemKeyAltPress = "ShortCutKeyAltNull";
        }

        if (KeyBoard_Win_Press_state){
            SystemKeyWinPress = "Win";
        }else{
            SystemKeyWinPress = "ShortCutKeyWinNull";
        }
        HidManager.sendKeyBoardFunction(SystemKeyCtrlPress, SystemKeyShiftPress, SystemKeyAltPress, SystemKeyWinPress, System_buttonId);
        System.out.println("Shift State: " + KeyBoard_ShIft_Press_state);

    }

    /**
     * Handle key press event - send key press without auto-release
     */
    private void handleKeyPress(String System_buttonId) {
        Log.e("KeyBoardSystem", "🟢 handleKeyPress called for: " + System_buttonId);
        
        String SystemKeyCtrlPress;
        String SystemKeyShiftPress;
        String SystemKeyAltPress;
        String SystemKeyWinPress;
        
        if (KeyBoard_Ctrl_Press_state){
            SystemKeyCtrlPress = "Ctrl";
        }else{
            SystemKeyCtrlPress = "ShortCutKeyCtrlNull";
        }

        if (KeyBoard_ShIft_Press_state){
            SystemKeyShiftPress = "Shift";
        }else{
            SystemKeyShiftPress = "ShortCutKeyShiftNull";
        }

        if (KeyBoard_Alt_Press_state){
            SystemKeyAltPress = "Alt";
        }else{
            SystemKeyAltPress = "ShortCutKeyAltNull";
        }

        if (KeyBoard_Win_Press_state){
            SystemKeyWinPress = "Win";
        }else{
            SystemKeyWinPress = "ShortCutKeyWinNull";
        }
        
        Log.e("KeyBoardSystem", "🟢 Modifiers - Ctrl:" + SystemKeyCtrlPress + 
              ", Shift:" + SystemKeyShiftPress + 
              ", Alt:" + SystemKeyAltPress + 
              ", Win:" + SystemKeyWinPress);
        
        // Send key press without automatic release
        HidManager.sendKeyBoardFunctionPress(SystemKeyCtrlPress, SystemKeyShiftPress, SystemKeyAltPress, SystemKeyWinPress, System_buttonId);
        Log.e("KeyBoardSystem", "🟢 Key press command sent - Shift State: " + KeyBoard_ShIft_Press_state);
    }

    /**
     * Handle key release event - send key release command
     */
    private void handleKeyRelease() {
        Log.e("KeyBoardSystem", "🔴 handleKeyRelease called");
        HidManager.sendKeyBoardRelease();
        Log.e("KeyBoardSystem", "🔴 Key release command sent");
    }

    public void setSystemButtonsClickColor(){
        if (KeyBoard_System == null) return;
        KeyBoard_System.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);//close keyboard

                if (Fragment_KeyBoard_System != null && Fragment_KeyBoard_System.getVisibility() == View.VISIBLE){
                    Fragment_KeyBoard_System.setVisibility(View.GONE);
                    KeyBoard_System.setBackgroundResource(R.drawable.nopress_button_background);
                }else if (Fragment_KeyBoard_System != null) {
                    Fragment_KeyBoard_System.setVisibility(View.VISIBLE);
                    KeyBoard_System.setBackgroundResource(R.drawable.press_button_background);
                }

                if (Fragment_KeyBoard_ShortCut != null && Fragment_KeyBoard_ShortCut.getVisibility() == View.VISIBLE) {
                    Fragment_KeyBoard_ShortCut.setVisibility(View.GONE);
                    if (KeyBoard_ShortCut != null) KeyBoard_ShortCut.setBackgroundResource(R.drawable.nopress_button_background);
                }

                if (Fragment_KeyBoard_Function != null && Fragment_KeyBoard_Function.getVisibility() == View.VISIBLE) {
                    Fragment_KeyBoard_Function.setVisibility(View.GONE);
                    if (KeyBoard_Function != null) KeyBoard_Function.setBackgroundResource(R.drawable.nopress_button_background);
                }
            }
        });
    }
}
