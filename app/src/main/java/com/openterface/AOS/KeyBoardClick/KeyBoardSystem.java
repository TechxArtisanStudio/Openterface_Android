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
import com.openterface.AOS.target.KeyBoardManager;
import com.openterface.AOS.target.KeyBoardMapping;

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

    private static boolean KeyBoard_Ctrl_Press_state;
    private static boolean KeyBoard_ShIft_Press_state;
    private static boolean KeyBoard_Alt_Press_state;
    private static boolean KeyBoard_Win_Press_state;
    private final View[] SystemButtons;

    // Letter buttons that need case updates when Shift is toggled
    private static final List<Button> letterButtons = new ArrayList<>();
    private static KeyBoardSystem instance;

    private static final Map<String, KeyBoardMapping> languageMappings = new HashMap<>();
    private static KeyBoardMapping currentMapping;
    private static String currentLanguage = "us";

    public KeyBoardSystem(MainActivity activity) {
        instance = this;
        Fragment_KeyBoard_ShortCut = activity.findViewById(R.id.Fragment_KeyBoard_ShortCut);
        Fragment_KeyBoard_Function = activity.findViewById(R.id.Fragment_KeyBoard_Function);
        Fragment_KeyBoard_System = activity.findViewById(R.id.Fragment_KeyBoard_System);

        KeyBoard_ShortCut = activity.findViewById(R.id.KeyBoard_ShortCut);
        KeyBoard_Function = activity.findViewById(R.id.KeyBoard_Function);
        KeyBoard_System = activity.findViewById(R.id.KeyBoard_System);
        this.context = activity;

        // Letter buttons for case switching
        Button[] letters = new Button[]{
                activity.findViewById(R.id.Q_Button), activity.findViewById(R.id.W_Button),
                activity.findViewById(R.id.E_Button), activity.findViewById(R.id.R_Button),
                activity.findViewById(R.id.T_Button), activity.findViewById(R.id.Y_Button),
                activity.findViewById(R.id.U_Button), activity.findViewById(R.id.I_Button),
                activity.findViewById(R.id.O_Button), activity.findViewById(R.id.P_Button),
                activity.findViewById(R.id.A_Button), activity.findViewById(R.id.S_Button),
                activity.findViewById(R.id.D_Button), activity.findViewById(R.id.F_Button),
                activity.findViewById(R.id.G_Button), activity.findViewById(R.id.H_Button),
                activity.findViewById(R.id.J_Button), activity.findViewById(R.id.K_Button),
                activity.findViewById(R.id.L_Button),
                activity.findViewById(R.id.Z_Button), activity.findViewById(R.id.X_Button),
                activity.findViewById(R.id.C_Button), activity.findViewById(R.id.V_Button),
                activity.findViewById(R.id.B_Button), activity.findViewById(R.id.N_Button),
                activity.findViewById(R.id.M_Button),
        };
        for (Button b : letters) letterButtons.add(b);

        SystemButtons = new View[]{
            // Row 1: Number row
            activity.findViewById(R.id.Key_Tilde),
            activity.findViewById(R.id.One_Sigh_Button),
            activity.findViewById(R.id.Two_At_Button),
            activity.findViewById(R.id.Three_Pound_Button),
            activity.findViewById(R.id.Four_Dollar_Button),
            activity.findViewById(R.id.Five_Percent_Button),
            activity.findViewById(R.id.Six_Caret_Button),
            activity.findViewById(R.id.Seven_Ampersand_Button),
            activity.findViewById(R.id.Eight_Asterisk_Button),
            activity.findViewById(R.id.Nine_Left_Parenthesis_Button),
            activity.findViewById(R.id.Zero_Right_Parenthesis_Button),
            activity.findViewById(R.id.Key_Minus),
            activity.findViewById(R.id.Key_Equals),
            activity.findViewById(R.id.Key_Backspace),

            // Row 2: Q row
            activity.findViewById(R.id.Key_Tab),
            activity.findViewById(R.id.Q_Button),
            activity.findViewById(R.id.W_Button),
            activity.findViewById(R.id.E_Button),
            activity.findViewById(R.id.R_Button),
            activity.findViewById(R.id.T_Button),
            activity.findViewById(R.id.Y_Button),
            activity.findViewById(R.id.U_Button),
            activity.findViewById(R.id.I_Button),
            activity.findViewById(R.id.O_Button),
            activity.findViewById(R.id.P_Button),
            activity.findViewById(R.id.Key_LeftBracket),
            activity.findViewById(R.id.Key_RightBracket),
            activity.findViewById(R.id.Key_Backslash),

            // Row 3: A row
            activity.findViewById(R.id.Key_Caps),
            activity.findViewById(R.id.A_Button),
            activity.findViewById(R.id.S_Button),
            activity.findViewById(R.id.D_Button),
            activity.findViewById(R.id.F_Button),
            activity.findViewById(R.id.G_Button),
            activity.findViewById(R.id.H_Button),
            activity.findViewById(R.id.J_Button),
            activity.findViewById(R.id.K_Button),
            activity.findViewById(R.id.L_Button),
            activity.findViewById(R.id.Key_Semicolon),
            activity.findViewById(R.id.Key_Apostrophe),
            activity.findViewById(R.id.Enter_Button),

            // Row 4: Z row
            activity.findViewById(R.id.Key_LeftShift),
            activity.findViewById(R.id.Z_Button),
            activity.findViewById(R.id.X_Button),
            activity.findViewById(R.id.C_Button),
            activity.findViewById(R.id.V_Button),
            activity.findViewById(R.id.B_Button),
            activity.findViewById(R.id.N_Button),
            activity.findViewById(R.id.M_Button),
            activity.findViewById(R.id.Key_Comma),
            activity.findViewById(R.id.Key_Period),
            activity.findViewById(R.id.Key_Slash),
            activity.findViewById(R.id.Key_RightShift),

            // Row 5: Bottom row
            activity.findViewById(R.id.Key_Ctrl),
            activity.findViewById(R.id.Key_Win),
            activity.findViewById(R.id.Key_Alt),
            activity.findViewById(R.id.Space_Button),
            activity.findViewById(R.id.Key_Up),
            activity.findViewById(R.id.Key_AltGr),
            activity.findViewById(R.id.Key_CtrlGr),
            activity.findViewById(R.id.Key_Left),
            activity.findViewById(R.id.Key_Down),
            activity.findViewById(R.id.Key_Right),
        };

        languageMappings.put("us", new KeyMapConfig_Us());
        languageMappings.put("de", new KeyMapConfig_De());
        currentMapping = languageMappings.get("us");
        SystemButtonListeners();
    }

    /**
     * Get the shared instance for external modifier buttons to call.
     */
    public static KeyBoardSystem getInstance() { return instance; }

    /**
     * Refresh all letter button labels based on current Shift state.
     * Called when Shift is pressed/released/locked.
     */
    public static void refreshLetterButtons() {
        if (instance == null) return;
        for (Button btn : letterButtons) {
            String[] mapping = currentMapping.getKeyMappings().get(btn.getId());
            if (mapping != null) {
                btn.setText(KeyBoard_ShIft_Press_state ? mapping[1] : mapping[0]);
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

    public static void setKeyboardLanguage(String language) {
        currentLanguage = language;
        currentMapping = languageMappings.get(language);
        if (currentMapping == null) {
            currentLanguage = "us";
            currentMapping = languageMappings.get("us");
        }
    }

    private void SystemButtonListeners() {
        for (View view : SystemButtons) {
            final boolean[] isKeyPressed = {false};

            // Check if this is a modifier key in the keyboard layout
            boolean isModifier = isModifierView(view);

            if (isModifier) {
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
                            }
                            return true;
                    }
                    return false;
                });
            }
        }
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
     * Set up a modifier key button with press/hold and long-press swipe-up to lock.
     * Press = activate, Release = deactivate, Long-press+swipe-up = toggle lock.
     */
    private void setupModifierButton(View v) {
        final boolean[] isLocked = {false};
        final float[] startY = {0};
        final Handler lockHandler = new Handler();
        final boolean[] longPressFired = {false};

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
            KeyBoardManager.sendModifierPress("Ctrl");
        } else if (id == R.id.Key_CtrlGr) {
            KeyBoard_Ctrl_Press(true);
            KeyBoardFunction.KeyBoard_Ctrl_Press(true);
            KeyBoardManager.sendModifierPress("CtrlR");
        } else if (id == R.id.Key_Alt) {
            KeyBoard_Alt_Press(true);
            KeyBoardFunction.KeyBoard_Alt_Press(true);
            KeyBoardManager.sendModifierPress("Alt");
        } else if (id == R.id.Key_AltGr) {
            KeyBoard_Alt_Press(true);
            KeyBoardFunction.KeyBoard_Alt_Press(true);
            KeyBoardManager.sendModifierPress("AltR");
        } else if (id == R.id.Key_Win) {
            KeyBoard_Win_Press(true);
            KeyBoardFunction.KeyBoard_Win_Press(true);
            KeyBoardManager.sendModifierPress("Win");
        } else if (id == R.id.Key_LeftShift) {
            KeyBoard_ShIft_Press(true);
            KeyBoardFunction.KeyBoard_ShIft_Press(true);
            KeyBoardManager.sendModifierPress("Shift");
        } else if (id == R.id.Key_RightShift) {
            KeyBoard_ShIft_Press(true);
            KeyBoardFunction.KeyBoard_ShIft_Press(true);
            KeyBoardManager.sendModifierPress("ShiftR");
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
            KeyBoardManager.sendModifierRelease();
        } else if (id == R.id.Key_Alt || id == R.id.Key_AltGr) {
            KeyBoard_Alt_Press(false);
            KeyBoardFunction.KeyBoard_Alt_Press(false);
            KeyBoardManager.sendModifierRelease();
        } else if (id == R.id.Key_Win) {
            KeyBoard_Win_Press(false);
            KeyBoardFunction.KeyBoard_Win_Press(false);
            KeyBoardManager.sendModifierRelease();
        } else if (id == R.id.Key_LeftShift || id == R.id.Key_RightShift) {
            KeyBoard_ShIft_Press(false);
            KeyBoardFunction.KeyBoard_ShIft_Press(false);
            KeyBoardManager.sendModifierRelease();
        }
    }

    private String getKey(int systemButtonId) {
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
        KeyBoardManager.sendKeyBoardFunction(SystemKeyCtrlPress, SystemKeyShiftPress, SystemKeyAltPress, SystemKeyWinPress, System_buttonId);
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
        KeyBoardManager.sendKeyBoardFunctionPress(SystemKeyCtrlPress, SystemKeyShiftPress, SystemKeyAltPress, SystemKeyWinPress, System_buttonId);
        Log.e("KeyBoardSystem", "🟢 Key press command sent - Shift State: " + KeyBoard_ShIft_Press_state);
    }

    /**
     * Handle key release event - send key release command
     */
    private void handleKeyRelease() {
        Log.e("KeyBoardSystem", "🔴 handleKeyRelease called");
        KeyBoardManager.sendKeyBoardRelease();
        Log.e("KeyBoardSystem", "🔴 Key release command sent");
    }

    public void setSystemButtonsClickColor(){
        KeyBoard_System.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);//close keyboard

                if (Fragment_KeyBoard_System.getVisibility() == View.VISIBLE){
                    Fragment_KeyBoard_System.setVisibility(View.GONE);
                    KeyBoard_System.setBackgroundResource(R.drawable.nopress_button_background);
                }else {
                    Fragment_KeyBoard_System.setVisibility(View.VISIBLE);
                    KeyBoard_System.setBackgroundResource(R.drawable.press_button_background);
                }

                if (Fragment_KeyBoard_ShortCut.getVisibility() == View.VISIBLE) {
                    Fragment_KeyBoard_ShortCut.setVisibility(View.GONE);
                    KeyBoard_ShortCut.setBackgroundResource(R.drawable.nopress_button_background);
                }

                if (Fragment_KeyBoard_Function.getVisibility() == View.VISIBLE) {
                    Fragment_KeyBoard_Function.setVisibility(View.GONE);
                    KeyBoard_Function.setBackgroundResource(R.drawable.nopress_button_background);
                }
            }
        });
    }
}
