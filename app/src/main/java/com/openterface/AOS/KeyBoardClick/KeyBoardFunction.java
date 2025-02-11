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
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.openterface.AOS.R;
import com.openterface.AOS.activity.MainActivity;
import com.openterface.AOS.target.KeyBoardManager;

public class KeyBoardFunction {
    private final Button KeyBoard_ShortCut;
    private final Button KeyBoard_Function;
    private final ImageButton KeyBoard_System;
    private final LinearLayout Fragment_KeyBoard_ShortCut;
    private final LinearLayout Fragment_KeyBoard_Function;
    private final LinearLayout Fragment_KeyBoard_System;
    private final Context context;

    private static boolean KeyBoard_ShIft_Press_state;

    public static void KeyBoard_ShIft_Press(Boolean KeyBoard_ShIft_Press){
        KeyBoard_ShIft_Press_state = KeyBoard_ShIft_Press;
    }

    private final View[] FunctionButtons;

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

        FunctionButtonListeners();
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
        if (Function_buttonId == R.id.Function1) {
            return "F1";
        } else if (Function_buttonId == R.id.Function2) {
            return "F2";
        } else if (Function_buttonId == R.id.Function3) {
            return "F3";
        } else if (Function_buttonId == R.id.Function4) {
            return "F4";
        } else if (Function_buttonId == R.id.Function5) {
            return "F5";
        } else if (Function_buttonId == R.id.Function6) {
            return "F6";
        } else if (Function_buttonId == R.id.Function7) {
            return "F7";
        } else if (Function_buttonId == R.id.Function8) {
            return "F8";
        } else if (Function_buttonId == R.id.Function9) {
            return "F9";
        } else if (Function_buttonId == R.id.Function10) {
            return "F10";
        } else if (Function_buttonId == R.id.Function11) {
            return "F11";
        } else if (Function_buttonId == R.id.Function12) {
            return "F12";
        } else if (Function_buttonId == R.id.PrtSc) {
            return "PrtSc";
        } else if (Function_buttonId == R.id.ScrLk) {
            return "ScrLk";
        } else if (Function_buttonId == R.id.Pause) {
            return "Pause";
        } else if (Function_buttonId == R.id.Ins) {
            return "Ins";
        } else if (Function_buttonId == R.id.Home) {
            return "Home";
        } else if (Function_buttonId == R.id.PgUp) {
            return "PgUp";
        } else if (Function_buttonId == R.id.Delete) {
            return "Delete";
        } else if (Function_buttonId == R.id.End) {
            return "End";
        } else if (Function_buttonId == R.id.PgDn) {
            return "PgDn";
        } else if (Function_buttonId == R.id.Esc) {
            return "Esc";
        } else if (Function_buttonId == R.id.TAB) {
            return "TAB";
        } else if (Function_buttonId == R.id.dropLeft) {
            return "DPAD_LEFT";
        } else if (Function_buttonId == R.id.dropRight) {
            return "DPAD_RIGHT";
        } else if (Function_buttonId == R.id.dropUp) {
            return "DPAD_UP";
        } else if (Function_buttonId == R.id.dropDown) {
            return "DPAD_DOWN";
        } else if (Function_buttonId == R.id.Minus_Sign_Button) {
            return KeyBoard_ShIft_Press_state ? "_" : "-";
        } else if (Function_buttonId == R.id.Plus_Sign_Button) {
            return KeyBoard_ShIft_Press_state ? "+" : "=";
        } else if (Function_buttonId == R.id.Left_Bracket_Button) {
            return KeyBoard_ShIft_Press_state ? "{" : "[";
        } else if (Function_buttonId == R.id.Right_Bracket_Button) {
            return KeyBoard_ShIft_Press_state ? "}" : "]";
        } else if (Function_buttonId == R.id.Colon_Button) {
            return KeyBoard_ShIft_Press_state ? ":" : ";";
        } else if (Function_buttonId == R.id.Quotation_Button) {
            return KeyBoard_ShIft_Press_state ? "\"" : "'";
        } else if (Function_buttonId == R.id.Bitwise_OR_Button) {
            return KeyBoard_ShIft_Press_state ? "|" : "\\";
        } else if (Function_buttonId == R.id.Less_Sign_Button) {
            return KeyBoard_ShIft_Press_state ? "<" : ",";
        } else if (Function_buttonId == R.id.Greater_Sign_Button) {
            return KeyBoard_ShIft_Press_state ? ">" : ".";
        } else if (Function_buttonId == R.id.Question_Mark) {
            return KeyBoard_ShIft_Press_state ? "?" : "/";
        } else if (Function_buttonId == R.id.Tilde) {
            return "~";
        } else {
            return "";
        }
    }

    private void handleShortcut(String Function_buttonId) {
        String FunctionKeyPress;
        if (KeyBoard_ShIft_Press_state){
            FunctionKeyPress = "Shift";
            KeyBoardManager.sendKeyBoardFunction(FunctionKeyPress, Function_buttonId);
        }else{
            FunctionKeyPress = "ShortCutKeyNull";
            KeyBoardManager.sendKeyBoardFunction(FunctionKeyPress, Function_buttonId);
        }
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
