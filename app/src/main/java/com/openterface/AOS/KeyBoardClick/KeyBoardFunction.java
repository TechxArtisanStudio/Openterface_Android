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
import com.openterface.AOS.target.KeyBoardManager;
import com.openterface.AOS.target.KeyBoardMapping;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class KeyBoardFunction {
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

    public KeyBoardFunction(MainActivity activity) {
        Fragment_KeyBoard_ShortCut = activity.findViewById(R.id.Fragment_KeyBoard_ShortCut);
        Fragment_KeyBoard_Function = activity.findViewById(R.id.Fragment_KeyBoard_Function);
        Fragment_KeyBoard_System = activity.findViewById(R.id.Fragment_KeyBoard_System);

        KeyBoard_ShortCut = activity.findViewById(R.id.KeyBoard_ShortCut);
        KeyBoard_Function = activity.findViewById(R.id.KeyBoard_Function);
        KeyBoard_System = activity.findViewById(R.id.KeyBoard_System);

        this.context = activity;
        FunctionButtons = new View[]{
                activity.findViewById(R.id.Function1),
                activity.findViewById(R.id.Function2),
                activity.findViewById(R.id.Function3),
                activity.findViewById(R.id.Function4),
                activity.findViewById(R.id.Function5),
                activity.findViewById(R.id.Function6),
                activity.findViewById(R.id.Function7),
                activity.findViewById(R.id.Function8),
                activity.findViewById(R.id.Function9),
                activity.findViewById(R.id.Function10),
                activity.findViewById(R.id.Function11),
                activity.findViewById(R.id.Function12),

                activity.findViewById(R.id.PrtSc),
                activity.findViewById(R.id.ScrLk),
                activity.findViewById(R.id.Pause),
                activity.findViewById(R.id.Ins),
                activity.findViewById(R.id.Home),
                activity.findViewById(R.id.PgUp),
                activity.findViewById(R.id.Delete),
                activity.findViewById(R.id.End),
                activity.findViewById(R.id.PgDn),

                activity.findViewById(R.id.Esc),
                activity.findViewById(R.id.TAB),

                activity.findViewById(R.id.dropLeft),
                activity.findViewById(R.id.dropRight),
                activity.findViewById(R.id.dropUp),
                activity.findViewById(R.id.dropDown),

                activity.findViewById(R.id.Minus_Sign_Button),
                activity.findViewById(R.id.Plus_Sign_Button),
                activity.findViewById(R.id.Left_Bracket_Button),
                activity.findViewById(R.id.Right_Bracket_Button),
                activity.findViewById(R.id.Colon_Button),
                activity.findViewById(R.id.Quotation_Button),
                activity.findViewById(R.id.Bitwise_OR_Button),
                activity.findViewById(R.id.Less_Sign_Button),
                activity.findViewById(R.id.Greater_Sign_Button),
                activity.findViewById(R.id.Question_Mark),
                activity.findViewById(R.id.Tilde),
        };

        languageMappings.put("us", new KeyMapConfig_Us());
        languageMappings.put("de", new KeyMapConfig_De());
        currentMapping = languageMappings.get("us");

        FunctionButtonListeners();

        Button Left_Than_Button = activity.findViewById(R.id.Left_Than_Button);
        Left_Than_Button.setOnClickListener(v -> {
            String currentLang = Locale.getDefault().getLanguage();
            if (currentLang.equals("de")) {

                String key = getKey(R.id.Left_Than_Button);
                Log.d("KeyBoardSystem", "German Button Pressed: " + key);
                handleShortcut(key);
            } else if (currentLang.equals("us")) {

            }
        });
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
            view.setOnClickListener(v -> {
                String functionButtonId = getKey(view.getId());
                handleShortcut(functionButtonId);
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
//            KeyBoardManager.sendKeyBoardFunction(FunctionKeyCtrlPress, Function_buttonId);
//        }else{
//            FunctionKeyCtrlPress = "ShortCutKeyNull";
//            KeyBoardManager.sendKeyBoardFunction(FunctionKeyCtrlPress, Function_buttonId);
//        }
//        System.out.println("Shift State: " + KeyBoard_ShIft_Press_state);

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
        KeyBoardManager.sendKeyBoardFunction(FunctionKeyCtrlPress, FunctionKeyShiftPress, FunctionKeyAltPress, FunctionKeyWinPress, Function_buttonId);
        System.out.println("Shift State: " + KeyBoard_ShIft_Press_state);

    }

    public void setFunctionButtonsClickColor(){
        KeyBoard_Function.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);//close keyboard

                if (Fragment_KeyBoard_Function.getVisibility() == View.VISIBLE){
                    Fragment_KeyBoard_Function.setVisibility(View.GONE);
                    KeyBoard_Function.setBackgroundResource(R.drawable.nopress_button_background);
                }else {
                    Fragment_KeyBoard_Function.setVisibility(View.VISIBLE);
                    KeyBoard_Function.setBackgroundResource(R.drawable.press_button_background);
                }

                if (Fragment_KeyBoard_ShortCut.getVisibility() == View.VISIBLE) {
                    Fragment_KeyBoard_ShortCut.setVisibility(View.GONE);
                    KeyBoard_ShortCut.setBackgroundResource(R.drawable.nopress_button_background);
                }

                if (Fragment_KeyBoard_System.getVisibility() == View.VISIBLE) {
                    Fragment_KeyBoard_System.setVisibility(View.GONE);
                    KeyBoard_System.setBackgroundResource(R.drawable.nopress_button_background);
                }
            }
        });
    }
}
