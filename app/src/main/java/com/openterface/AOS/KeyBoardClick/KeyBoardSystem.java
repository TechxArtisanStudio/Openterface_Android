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

public class KeyBoardSystem {

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

    private final View[] SystemButtons;

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

        SystemButtonListeners();
    }

    private void SystemButtonListeners() {
        for (View view : SystemButtons) {
            view.setOnClickListener(v -> {
                String systemButtonId = getKey(view.getId());
                handleShortcut(systemButtonId);
            });
        }
    }

    private String getKey(int System_buttonId) {
        if (System_buttonId == R.id.One_Sigh_Button) {
            return KeyBoard_ShIft_Press_state ? "!" : "1";
        } else if (System_buttonId == R.id.Two_At_Button) {
            return KeyBoard_ShIft_Press_state ? "@" : "2";
        } else if (System_buttonId == R.id.Three_Pound_Button) {
            return KeyBoard_ShIft_Press_state ? "#" : "3";
        } else if (System_buttonId == R.id.Four_Dollar_Button) {
            return KeyBoard_ShIft_Press_state ? "$" : "4";
        } else if (System_buttonId == R.id.Five_Percent_Button) {
            return KeyBoard_ShIft_Press_state ? "%" : "5";
        } else if (System_buttonId == R.id.Six_Caret_Button) {
            return KeyBoard_ShIft_Press_state ? "^" : "6";
        } else if (System_buttonId == R.id.Seven_Ampersand_Button) {
            return KeyBoard_ShIft_Press_state ? "&" : "7";
        } else if (System_buttonId == R.id.Eight_Asterisk_Button) {
            return KeyBoard_ShIft_Press_state ? "*" : "8";
        } else if (System_buttonId == R.id.Nine_Left_Parenthesis_Button) {
            return KeyBoard_ShIft_Press_state ? "(" : "9";
        } else if (System_buttonId == R.id.Zero_Right_Parenthesis_Button) {
            return KeyBoard_ShIft_Press_state ? ")" : "0";
        } else if (System_buttonId == R.id.Q_Button) {
            return KeyBoard_ShIft_Press_state ? "Q" : "q";
        } else if (System_buttonId == R.id.W_Button) {
            return KeyBoard_ShIft_Press_state ? "W" : "w";
        } else if (System_buttonId == R.id.E_Button) {
            return KeyBoard_ShIft_Press_state ? "E" : "e";
        } else if (System_buttonId == R.id.R_Button) {
            return KeyBoard_ShIft_Press_state ? "R" : "r";
        } else if (System_buttonId == R.id.T_Button) {
            return KeyBoard_ShIft_Press_state ? "T" : "t";
        } else if (System_buttonId == R.id.Y_Button) {
            return KeyBoard_ShIft_Press_state ? "Y" : "y";
        } else if (System_buttonId == R.id.U_Button) {
            return KeyBoard_ShIft_Press_state ? "U" : "u";
        } else if (System_buttonId == R.id.I_Button) {
            return KeyBoard_ShIft_Press_state ? "I" : "i";
        } else if (System_buttonId == R.id.O_Button) {
            return KeyBoard_ShIft_Press_state ? "O" : "o";
        } else if (System_buttonId == R.id.P_Button) {
            return KeyBoard_ShIft_Press_state ? "P" : "p";
        } else if (System_buttonId == R.id.A_Button) {
            return KeyBoard_ShIft_Press_state ? "A" : "a";
        } else if (System_buttonId == R.id.S_Button) {
            return KeyBoard_ShIft_Press_state ? "S" : "s";
        } else if (System_buttonId == R.id.D_Button) {
            return KeyBoard_ShIft_Press_state ? "D" : "d";
        } else if (System_buttonId == R.id.F_Button) {
            return KeyBoard_ShIft_Press_state ? "F" : "f";
        } else if (System_buttonId == R.id.G_Button) {
            return KeyBoard_ShIft_Press_state ? "G" : "g";
        } else if (System_buttonId == R.id.H_Button) {
            return KeyBoard_ShIft_Press_state ? "H" : "h";
        } else if (System_buttonId == R.id.J_Button) {
            return KeyBoard_ShIft_Press_state ? "J" : "j";
        } else if (System_buttonId == R.id.K_Button) {
            return KeyBoard_ShIft_Press_state ? "K" : "k";
        } else if (System_buttonId == R.id.L_Button) {
            return KeyBoard_ShIft_Press_state ? "L" : "l";
        } else if (System_buttonId == R.id.Z_Button) {
            return KeyBoard_ShIft_Press_state ? "Z" : "z";
        } else if (System_buttonId == R.id.X_Button) {
            return KeyBoard_ShIft_Press_state ? "X" : "x";
        } else if (System_buttonId == R.id.C_Button) {
            return KeyBoard_ShIft_Press_state ? "C" : "c";
        } else if (System_buttonId == R.id.V_Button) {
            return KeyBoard_ShIft_Press_state ? "V" : "v";
        } else if (System_buttonId == R.id.B_Button) {
            return KeyBoard_ShIft_Press_state ? "B" : "b";
        } else if (System_buttonId == R.id.N_Button) {
            return KeyBoard_ShIft_Press_state ? "N" : "n";
        } else if (System_buttonId == R.id.M_Button) {
            return KeyBoard_ShIft_Press_state ? "M" : "m";
        }  else if (System_buttonId == R.id.DEL_Button) {
            return "DEL";
        }  else if (System_buttonId == R.id.Space_Button) {
            return "SPACE";
        }  else if (System_buttonId == R.id.Enter_Button) {
            return "ENTER";
        }  else {
            return "";
        }
    }

    private void handleShortcut(String System_buttonId) {
        String SystemKeyPress;
        if (KeyBoard_ShIft_Press_state){
            SystemKeyPress = "Shift";
            KeyBoardManager.sendKeyBoardFunction(SystemKeyPress, System_buttonId);
        }else{
            SystemKeyPress = "ShortCutKeyNull";
            KeyBoardManager.sendKeyBoardFunction(SystemKeyPress, System_buttonId);
        }
        System.out.println("Shift State: " + KeyBoard_ShIft_Press_state);

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
