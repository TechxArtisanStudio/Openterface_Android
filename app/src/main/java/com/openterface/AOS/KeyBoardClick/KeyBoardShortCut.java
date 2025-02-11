/**
* @Title: KeyBoardShortCut
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

public class KeyBoardShortCut {
    private final Button[] ShortCutButtons;
    private final Button KeyBoard_ShortCut;
    private final Button KeyBoard_Function;
    private final ImageButton KeyBoard_System;
    private final LinearLayout Fragment_KeyBoard_ShortCut;
    private final LinearLayout Fragment_KeyBoard_Function;
    private final LinearLayout Fragment_KeyBoard_System;
    private final Context context;

    public KeyBoardShortCut(MainActivity activity) {
        Fragment_KeyBoard_ShortCut = activity.findViewById(R.id.Fragment_KeyBoard_ShortCut);
        Fragment_KeyBoard_Function = activity.findViewById(R.id.Fragment_KeyBoard_Function);
        Fragment_KeyBoard_System = activity.findViewById(R.id.Fragment_KeyBoard_System);

        KeyBoard_ShortCut = activity.findViewById(R.id.KeyBoard_ShortCut);
        KeyBoard_Function = activity.findViewById(R.id.KeyBoard_Function);
        KeyBoard_System = activity.findViewById(R.id.KeyBoard_System);
        this.context = activity;
        ShortCutButtons = new Button[]{
                activity.findViewById(R.id.Ctrl_C),
                activity.findViewById(R.id.Ctrl_V),
                activity.findViewById(R.id.Ctrl_Z),
                activity.findViewById(R.id.Ctrl_X),
                activity.findViewById(R.id.Ctrl_A),
                activity.findViewById(R.id.Ctrl_S),
                activity.findViewById(R.id.Win_Tab),
                activity.findViewById(R.id.Win_S),
                activity.findViewById(R.id.Win_E),
                activity.findViewById(R.id.Win_R),
                activity.findViewById(R.id.Win_D),
                activity.findViewById(R.id.Win_L),
                activity.findViewById(R.id.Alt_F4),
                activity.findViewById(R.id.Alt_PrtScr),
                activity.findViewById(R.id.CtrlAltDel)
        };

        ShortCutButtonListeners();
    }

    private void ShortCutButtonListeners() {
        ShortCutButtons[ShortCutButtons.length - 1].setOnClickListener(v -> {
            KeyBoardManager.sendKeyBoardFunctionCtrlAltDel();
        });

        for (Button button : ShortCutButtons) {
            if (button.getId() != R.id.CtrlAltDel) {
                String modifier = getModifier(button.getId());
                String key = getKey(button.getId());
                button.setOnClickListener(v -> handleShortcut(modifier, key));
            }
        }
    }

    private String getModifier(int buttonId) {
        if (buttonId == R.id.Alt_F4 || buttonId == R.id.Alt_PrtScr) {
            return "Alt";
        } else if (buttonId == R.id.Win_Tab || buttonId == R.id.Win_S ||
                buttonId == R.id.Win_E || buttonId == R.id.Win_R ||
                buttonId == R.id.Win_D || buttonId == R.id.Win_L) {
            return "Win";
        } else {
            return "Ctrl";
        }
    }

    private String getKey(int buttonId) {
        if (buttonId == R.id.Ctrl_C) {
            return "C";
        } else if (buttonId == R.id.Ctrl_V) {
            return "V";
        } else if (buttonId == R.id.Ctrl_Z) {
            return "Z";
        } else if (buttonId == R.id.Ctrl_X) {
            return "X";
        } else if (buttonId == R.id.Ctrl_A) {
            return "A";
        } else if (buttonId == R.id.Ctrl_S) {
            return "S";
        } else if (buttonId == R.id.Win_Tab) {
            return "TAB";
        } else if (buttonId == R.id.Win_S) {
            return "S";
        } else if (buttonId == R.id.Win_E) {
            return "E";
        } else if (buttonId == R.id.Win_R) {
            return "R";
        } else if (buttonId == R.id.Win_D) {
            return "D";
        } else if (buttonId == R.id.Win_L) {
            return "L";
        } else if (buttonId == R.id.Alt_F4) {
            return "F4";
        } else if (buttonId == R.id.Alt_PrtScr) {
            return "PrtSc";
        }else {
            return "";
        }
    }

    private void handleShortcut(String modifier, String key) {
        KeyBoardManager.sendKeyBoardShortCut(modifier, key);
    }

    public void setShortCutButtonsClickColor(){
        KeyBoard_ShortCut.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);//close keyboard

                if (Fragment_KeyBoard_ShortCut.getVisibility() == View.VISIBLE){
                    KeyBoard_ShortCut.setBackgroundResource(R.drawable.nopress_button_background);
                    Fragment_KeyBoard_ShortCut.setVisibility(View.GONE);
                }else {
                    Fragment_KeyBoard_ShortCut.setVisibility(View.VISIBLE);
                    KeyBoard_ShortCut.setBackgroundResource(R.drawable.press_button_background);
                }

                if (Fragment_KeyBoard_Function.getVisibility() == View.VISIBLE) {
                    Fragment_KeyBoard_Function.setVisibility(View.GONE);
                    KeyBoard_Function.setBackgroundResource(R.drawable.nopress_button_background);
                }

                if (Fragment_KeyBoard_System.getVisibility() == View.VISIBLE) {
                    Fragment_KeyBoard_System.setVisibility(View.GONE);
                    KeyBoard_System.setBackgroundResource(R.drawable.nopress_button_background);
                }
            }
        });
    }
}
