/**
* @Title: KeyBoardFunction
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
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.openterface.AOS.R;
import com.openterface.AOS.activity.MainActivity;
import com.openterface.AOS.manager.LocaleManager;
import com.openterface.AOS.target.HidManager;
// import com.openterface.AOS.target.KeyBoardManager;
import com.openterface.AOS.target.KeyBoardMapping;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class KeyBoardFunction {
    private static final String TAG = "OP-KB";
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

    public static void KeyBoard_Ctrl_Press(Boolean KeyBoard_Ctrl_Press){
        KeyBoard_Ctrl_Press_state = KeyBoard_Ctrl_Press;
    }

    public static void KeyBoard_ShIft_Press(Boolean KeyBoard_ShIft_Press){
        KeyBoard_ShIft_Press_state = KeyBoard_ShIft_Press;
    }

    public static void KeyBoard_Alt_Press(Boolean KeyBoard_Alt_Press){
        KeyBoard_Alt_Press_state = KeyBoard_Alt_Press;
    }

    public static void KeyBoard_Win_Press(Boolean KeyBoard_Win_Press){
        KeyBoard_Win_Press_state = KeyBoard_Win_Press;
    }

    private final View[] FunctionButtons;
    private static KeyBoardMapping currentMapping;
    private static final Map<String, KeyBoardMapping> languageMappings = new HashMap<>();
    private static String currentLanguage = "us";

    public KeyBoardFunction(View rootView) {
        Fragment_KeyBoard_ShortCut = rootView.findViewById(R.id.Fragment_KeyBoard_ShortCut);
        Fragment_KeyBoard_Function = rootView.findViewById(R.id.Fragment_KeyBoard_Function);
        Fragment_KeyBoard_System = rootView.findViewById(R.id.Fragment_KeyBoard_System);

        KeyBoard_ShortCut = rootView.findViewById(R.id.KeyBoard_ShortCut);
        KeyBoard_Function = rootView.findViewById(R.id.KeyBoard_Function);
        KeyBoard_System = rootView.findViewById(R.id.KeyBoard_System);

        this.context = rootView.getContext();
        FunctionButtons = new View[]{
                rootView.findViewById(R.id.Function1),
                rootView.findViewById(R.id.Function2),
                rootView.findViewById(R.id.Function3),
                rootView.findViewById(R.id.Function4),
                rootView.findViewById(R.id.Function5),
                rootView.findViewById(R.id.Function6),
                rootView.findViewById(R.id.Function7),
                rootView.findViewById(R.id.Function8),
                rootView.findViewById(R.id.Function9),
                rootView.findViewById(R.id.Function10),
                rootView.findViewById(R.id.Function11),
                rootView.findViewById(R.id.Function12),

                rootView.findViewById(R.id.PrtSc),
                rootView.findViewById(R.id.ScrLk),
                rootView.findViewById(R.id.Pause),
                rootView.findViewById(R.id.Ins),
                rootView.findViewById(R.id.Home),
                rootView.findViewById(R.id.PgUp),
                rootView.findViewById(R.id.Delete),
                rootView.findViewById(R.id.End),
                rootView.findViewById(R.id.PgDn),

                rootView.findViewById(R.id.Esc),
                rootView.findViewById(R.id.TAB),

                rootView.findViewById(R.id.dropLeft),
                rootView.findViewById(R.id.dropRight),
                rootView.findViewById(R.id.dropUp),
                rootView.findViewById(R.id.dropDown),

                rootView.findViewById(R.id.Minus_Sign_Button),
                rootView.findViewById(R.id.Plus_Sign_Button),
                rootView.findViewById(R.id.Left_Bracket_Button),
                rootView.findViewById(R.id.Right_Bracket_Button),
                rootView.findViewById(R.id.Colon_Button),
                rootView.findViewById(R.id.Quotation_Button),
                rootView.findViewById(R.id.Bitwise_OR_Button),
                rootView.findViewById(R.id.Less_Sign_Button),
                rootView.findViewById(R.id.Greater_Sign_Button),
                rootView.findViewById(R.id.Question_Mark),
                rootView.findViewById(R.id.Tilde),
        };

        languageMappings.put("us", new KeyMapConfig_Us());
        languageMappings.put("de", new KeyMapConfig_De());
        currentMapping = languageMappings.get("us");

        FunctionButtonListeners();

        Button Left_Than_Button = rootView.findViewById(R.id.Left_Than_Button);
        if (Left_Than_Button != null) {
            Left_Than_Button.setOnClickListener(v -> {
                // Use the resolved locale from LocaleManager (e.g., "system" -> "ja-rJP")
                String resolvedLocale = LocaleManager.getInstance().getResolvedLocaleCode();
                // Extract base language for keyboard layout logic
                String baseLang = resolvedLocale.contains("-r") ?
                    resolvedLocale.substring(0, resolvedLocale.indexOf("-r")) :
                    resolvedLocale;
                if (baseLang.equals("de")) {
                    String key = getKey(R.id.Left_Than_Button);
                    Log.d(TAG, "German Button Pressed: " + key);
                    handleShortcut(key);
                }
            });
        }
    }

    public static void setKeyboardLanguage(String language) {
        currentLanguage = language;
        currentMapping = languageMappings.get(language);
        if (currentMapping == null) {
            currentLanguage = "us";
            currentMapping = languageMappings.get("us");
        }
    }

    private void FunctionButtonListeners() {
        for (View view : FunctionButtons) {
            if (view == null) continue;
            final boolean[] isKeyPressed = {false}; // Track if key is already pressed

            view.setOnTouchListener((v, event) -> {
                String functionButtonId = getKey(view.getId());

                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        // Only send key press if not already pressed (prevent repeat ACTION_DOWN)
                        if (!isKeyPressed[0]) {
                            Log.d(TAG, "Function Button PRESSED: " + functionButtonId);
                            handleKeyPress(functionButtonId);
                            isKeyPressed[0] = true;
                        }
                        return true;

                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        // Only send release if key was pressed
                        if (isKeyPressed[0]) {
                            Log.d(TAG, "Function Button RELEASED: " + functionButtonId);
                            handleKeyRelease();
                            isKeyPressed[0] = false;
                        }
                        return true;

                    case android.view.MotionEvent.ACTION_MOVE:
                        // Key hold - do nothing
                        return true;
                }
                return false;
            });
        }
    }

    private String getKey(int Function_buttonId) {
        String[] mapping = currentMapping.getKeyMappings().get(Function_buttonId);
        if (mapping != null) {
            return KeyBoard_ShIft_Press_state ? mapping[1] : mapping[0];
        }
        return "";
    }

    private void handleShortcut(String Function_buttonId) {
//        String FunctionKeyCtrlPress;
//        if (KeyBoard_ShIft_Press_state){
//            FunctionKeyCtrlPress = "Shift";
//            HidManager.sendKeyBoardFunction(FunctionKeyCtrlPress, Function_buttonId);
//        }else{
//            FunctionKeyCtrlPress = "ShortCutKeyNull";
//            HidManager.sendKeyBoardFunction(FunctionKeyCtrlPress, Function_buttonId);
//        }
//        Log.d(TAG, "Shift State: " + KeyBoard_ShIft_Press_state);

        String FunctionKeyCtrlPress;
        String FunctionKeyShiftPress;
        String FunctionKeyAltPress;
        String FunctionKeyWinPress;
        if (KeyBoard_Ctrl_Press_state){
            FunctionKeyCtrlPress = "Ctrl";
        }else{
            FunctionKeyCtrlPress = "ShortCutKeyCtrlNull";
        }

        if (KeyBoard_ShIft_Press_state){
            FunctionKeyShiftPress = "Shift";
        }else{
            FunctionKeyShiftPress = "ShortCutKeyShiftNull";
        }

        if (KeyBoard_Alt_Press_state){
            FunctionKeyAltPress = "Alt";
        }else{
            FunctionKeyAltPress = "ShortCutKeyAltNull";
        }

        if (KeyBoard_Win_Press_state){
            FunctionKeyWinPress = "Win";
        }else{
            FunctionKeyWinPress = "ShortCutKeyWinNull";
        }
        HidManager.sendKeyBoardFunction(FunctionKeyCtrlPress, FunctionKeyShiftPress, FunctionKeyAltPress, FunctionKeyWinPress, Function_buttonId);
        Log.d(TAG, "Shift State: " + KeyBoard_ShIft_Press_state);

    }

    /**
     * Handle key press event - send key press without auto-release
     */
    private void handleKeyPress(String Function_buttonId) {
        String FunctionKeyCtrlPress;
        String FunctionKeyShiftPress;
        String FunctionKeyAltPress;
        String FunctionKeyWinPress;
        
        if (KeyBoard_Ctrl_Press_state){
            FunctionKeyCtrlPress = "Ctrl";
        }else{
            FunctionKeyCtrlPress = "ShortCutKeyCtrlNull";
        }

        if (KeyBoard_ShIft_Press_state){
            FunctionKeyShiftPress = "Shift";
        }else{
            FunctionKeyShiftPress = "ShortCutKeyShiftNull";
        }

        if (KeyBoard_Alt_Press_state){
            FunctionKeyAltPress = "Alt";
        }else{
            FunctionKeyAltPress = "ShortCutKeyAltNull";
        }

        if (KeyBoard_Win_Press_state){
            FunctionKeyWinPress = "Win";
        }else{
            FunctionKeyWinPress = "ShortCutKeyWinNull";
        }
        
        // Send key press without automatic release
        HidManager.sendKeyBoardFunctionPress(FunctionKeyCtrlPress, FunctionKeyShiftPress, FunctionKeyAltPress, FunctionKeyWinPress, Function_buttonId);
        Log.d(TAG, "Key press sent - Shift State: " + KeyBoard_ShIft_Press_state);
    }

    /**
     * Handle key release event - send key release command
     */
    private void handleKeyRelease() {
        HidManager.sendKeyBoardRelease();
        Log.d(TAG, "Key released");
    }

    public void setFunctionButtonsClickColor(){
        if (KeyBoard_Function == null) return;
        KeyBoard_Function.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);//close keyboard

                if (Fragment_KeyBoard_Function != null && Fragment_KeyBoard_Function.getVisibility() == View.VISIBLE){
                    Fragment_KeyBoard_Function.setVisibility(View.GONE);
                    KeyBoard_Function.setBackgroundResource(R.drawable.nopress_button_background);
                }else if (Fragment_KeyBoard_Function != null) {
                    Fragment_KeyBoard_Function.setVisibility(View.VISIBLE);
                    KeyBoard_Function.setBackgroundResource(R.drawable.press_button_background);
                }

                if (Fragment_KeyBoard_ShortCut != null && Fragment_KeyBoard_ShortCut.getVisibility() == View.VISIBLE) {
                    Fragment_KeyBoard_ShortCut.setVisibility(View.GONE);
                    if (KeyBoard_ShortCut != null) KeyBoard_ShortCut.setBackgroundResource(R.drawable.nopress_button_background);
                }

                if (Fragment_KeyBoard_System != null && Fragment_KeyBoard_System.getVisibility() == View.VISIBLE) {
                    Fragment_KeyBoard_System.setVisibility(View.GONE);
                    if (KeyBoard_System != null) KeyBoard_System.setBackgroundResource(R.drawable.nopress_button_background);
                }
            }
        });
    }
}
