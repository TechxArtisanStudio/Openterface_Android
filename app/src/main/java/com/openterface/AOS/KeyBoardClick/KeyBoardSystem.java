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

    private static final Map<String, KeyBoardMapping> languageMappings = new HashMap<>();
    private static KeyBoardMapping currentMapping;
    private static String currentLanguage = "us";

    public KeyBoardSystem(MainActivity activity) {
        Fragment_KeyBoard_ShortCut = activity.findViewById(R.id.Fragment_KeyBoard_ShortCut);
        Fragment_KeyBoard_Function = activity.findViewById(R.id.Fragment_KeyBoard_Function);
        Fragment_KeyBoard_System = activity.findViewById(R.id.Fragment_KeyBoard_System);

        KeyBoard_ShortCut = activity.findViewById(R.id.KeyBoard_ShortCut);
        KeyBoard_Function = activity.findViewById(R.id.KeyBoard_Function);
        KeyBoard_System = activity.findViewById(R.id.KeyBoard_System);
        this.context = activity;
        SystemButtons = new View[]{
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

            activity.findViewById(R.id.A_Button),
            activity.findViewById(R.id.S_Button),
            activity.findViewById(R.id.D_Button),
            activity.findViewById(R.id.F_Button),
            activity.findViewById(R.id.G_Button),
            activity.findViewById(R.id.H_Button),
            activity.findViewById(R.id.J_Button),
            activity.findViewById(R.id.K_Button),
            activity.findViewById(R.id.L_Button),

            activity.findViewById(R.id.Z_Button),
            activity.findViewById(R.id.X_Button),
            activity.findViewById(R.id.C_Button),
            activity.findViewById(R.id.V_Button),
            activity.findViewById(R.id.B_Button),
            activity.findViewById(R.id.N_Button),
            activity.findViewById(R.id.M_Button),

            activity.findViewById(R.id.DEL_Button),
            activity.findViewById(R.id.Space_Button),
            activity.findViewById(R.id.Enter_Button),
        };

        languageMappings.put("us", new KeyMapConfig_Us());
        languageMappings.put("de", new KeyMapConfig_De());
        currentMapping = languageMappings.get("us");
        SystemButtonListeners();
    }

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
            final boolean[] isKeyPressed = {false}; // Track if key is already pressed
            
            view.setOnTouchListener((v, event) -> {
                String systemButtonId = getKey(view.getId());
                
                Log.e("KeyBoardSystem", "📱 Touch Event: action=" + event.getAction() + 
                      ", key=" + systemButtonId + 
                      ", isPressed=" + isKeyPressed[0] + 
                      ", time=" + System.currentTimeMillis());
                
                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        // Only send key press if not already pressed (prevent repeat ACTION_DOWN)
                        if (!isKeyPressed[0]) {
                            Log.e("KeyBoardSystem", "✅ System Button PRESSED: " + systemButtonId + " (sending press command)");
                            handleKeyPress(systemButtonId);
                            isKeyPressed[0] = true;
                        } else {
                            Log.e("KeyBoardSystem", "⚠️ System Button ALREADY PRESSED: " + systemButtonId + " (ignored duplicate)");
                        }
                        return true;
                        
                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        // Only send release if key was pressed
                        if (isKeyPressed[0]) {
                            Log.e("KeyBoardSystem", "✅ System Button RELEASED: " + systemButtonId + " (sending release command)");
                            handleKeyRelease();
                            isKeyPressed[0] = false;
                        } else {
                            Log.e("KeyBoardSystem", "⚠️ System Button RELEASE WITHOUT PRESS: " + systemButtonId + " (ignored)");
                        }
                        return true;
                        
                    case android.view.MotionEvent.ACTION_MOVE:
                        // Key hold - do nothing
                        Log.d("KeyBoardSystem", "🔄 System Button MOVE (holding): " + systemButtonId);
                        return true;
                }
                return false;
            });
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
