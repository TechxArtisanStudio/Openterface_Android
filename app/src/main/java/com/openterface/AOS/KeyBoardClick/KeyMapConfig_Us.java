package com.openterface.AOS.KeyBoardClick;

import com.openterface.AOS.R;
import com.openterface.AOS.target.KeyBoardMapping;

import java.util.HashMap;
import java.util.Map;

public class KeyMapConfig_Us implements KeyBoardMapping {
    private final Map<Integer, String[]> keyMappings = new HashMap<>();

    public KeyMapConfig_Us() {
        keyMappings.put(R.id.One_Sigh_Button, new String[]{"1", "!"});
        keyMappings.put(R.id.Two_At_Button, new String[]{"2", "@"});
        keyMappings.put(R.id.Three_Pound_Button, new String[]{"3", "#"});
        keyMappings.put(R.id.Four_Dollar_Button, new String[]{"4", "$"});
        keyMappings.put(R.id.Five_Percent_Button, new String[]{"5", "%"});
        keyMappings.put(R.id.Six_Caret_Button, new String[]{"6", "^"});
        keyMappings.put(R.id.Seven_Ampersand_Button, new String[]{"7", "&"});
        keyMappings.put(R.id.Eight_Asterisk_Button, new String[]{"8", "*"});
        keyMappings.put(R.id.Nine_Left_Parenthesis_Button, new String[]{"9", "("});
        keyMappings.put(R.id.Zero_Right_Parenthesis_Button, new String[]{"0", ")"});

        keyMappings.put(R.id.Q_Button, new String[]{"q", "Q"});
        keyMappings.put(R.id.W_Button, new String[]{"w", "W"});
        keyMappings.put(R.id.E_Button, new String[]{"e", "E"});
        keyMappings.put(R.id.R_Button, new String[]{"r", "R"});
        keyMappings.put(R.id.T_Button, new String[]{"t", "T"});
        keyMappings.put(R.id.Y_Button, new String[]{"y", "Y"});
        keyMappings.put(R.id.U_Button, new String[]{"u", "U"});
        keyMappings.put(R.id.I_Button, new String[]{"i", "I"});
        keyMappings.put(R.id.O_Button, new String[]{"o", "O"});
        keyMappings.put(R.id.P_Button, new String[]{"p", "P"});

        keyMappings.put(R.id.A_Button, new String[]{"a", "A"});
        keyMappings.put(R.id.S_Button, new String[]{"s", "S"});
        keyMappings.put(R.id.D_Button, new String[]{"d", "D"});
        keyMappings.put(R.id.F_Button, new String[]{"f", "F"});
        keyMappings.put(R.id.G_Button, new String[]{"g", "G"});
        keyMappings.put(R.id.H_Button, new String[]{"h", "H"});
        keyMappings.put(R.id.J_Button, new String[]{"j", "J"});
        keyMappings.put(R.id.K_Button, new String[]{"k", "K"});
        keyMappings.put(R.id.L_Button, new String[]{"l", "L"});

        keyMappings.put(R.id.Z_Button, new String[]{"z", "Z"});
        keyMappings.put(R.id.X_Button, new String[]{"x", "X"});
        keyMappings.put(R.id.C_Button, new String[]{"c", "C"});
        keyMappings.put(R.id.V_Button, new String[]{"v", "V"});
        keyMappings.put(R.id.B_Button, new String[]{"b", "B"});
        keyMappings.put(R.id.N_Button, new String[]{"n", "N"});
        keyMappings.put(R.id.M_Button, new String[]{"m", "M"});

        keyMappings.put(R.id.DEL_Button, new String[]{"DEL", "DEL"});
        keyMappings.put(R.id.Space_Button, new String[]{"SPACE", "SPACE"});
        keyMappings.put(R.id.Enter_Button, new String[]{"ENTER", "ENTER"});

        keyMappings.put(R.id.Function1, new String[]{"F1", "F1"});
        keyMappings.put(R.id.Function2, new String[]{"F2", "F2"});
        keyMappings.put(R.id.Function3, new String[]{"F3", "F3"});
        keyMappings.put(R.id.Function4, new String[]{"F4", "F4"});
        keyMappings.put(R.id.Function5, new String[]{"F5", "F5"});
        keyMappings.put(R.id.Function6, new String[]{"F6", "F6"});
        keyMappings.put(R.id.Function7, new String[]{"F7", "F7"});
        keyMappings.put(R.id.Function8, new String[]{"F8", "F8"});
        keyMappings.put(R.id.Function9, new String[]{"F9", "F9"});
        keyMappings.put(R.id.Function10, new String[]{"F10", "F10"});
        keyMappings.put(R.id.Function11, new String[]{"F11", "F11"});
        keyMappings.put(R.id.Function12, new String[]{"F12", "F12"});
        keyMappings.put(R.id.PrtSc, new String[]{"PrtSc", "PrtSc"});
        keyMappings.put(R.id.ScrLk, new String[]{"ScrLk", "ScrLk"});
        keyMappings.put(R.id.Pause, new String[]{"Pause", "Pause"});
        keyMappings.put(R.id.Ins, new String[]{"Ins", "Ins"});
        keyMappings.put(R.id.Home, new String[]{"Home", "Home"});
        keyMappings.put(R.id.PgUp, new String[]{"PgUp", "PgUp"});
        keyMappings.put(R.id.Delete, new String[]{"Delete", "Delete"});
        keyMappings.put(R.id.End, new String[]{"End", "F1"});
        keyMappings.put(R.id.PgDn, new String[]{"PgDn", "PgDn"});
        keyMappings.put(R.id.Esc, new String[]{"Esc", "Esc"});

        keyMappings.put(R.id.TAB, new String[]{"TAB", "TAB"});
        keyMappings.put(R.id.dropLeft, new String[]{"DPAD_LEFT", "DPAD_LEFT"});
        keyMappings.put(R.id.dropRight, new String[]{"DPAD_RIGHT", "DPAD_RIGHT"});
        keyMappings.put(R.id.dropUp, new String[]{"DPAD_UP", "DPAD_UP"});
        keyMappings.put(R.id.dropDown, new String[]{"DPAD_DOWN", "DPAD_DOWN"});
        keyMappings.put(R.id.Minus_Sign_Button, new String[]{"-", "_"});
        keyMappings.put(R.id.Plus_Sign_Button, new String[]{"=", "+"});
        keyMappings.put(R.id.Left_Bracket_Button, new String[]{"[", "{"});

        keyMappings.put(R.id.Right_Bracket_Button, new String[]{"]", "}"});
        keyMappings.put(R.id.Colon_Button, new String[]{";", ":"});
        keyMappings.put(R.id.Quotation_Button, new String[]{"'", "\""});
        keyMappings.put(R.id.Bitwise_OR_Button, new String[]{"\\", "|"});
        keyMappings.put(R.id.Less_Sign_Button, new String[]{",", "<"});
        keyMappings.put(R.id.Greater_Sign_Button, new String[]{".", ">"});
        keyMappings.put(R.id.Question_Mark, new String[]{"/", "?"});
        keyMappings.put(R.id.Tilde, new String[]{"`", "~"});
    }

    @Override
    public Map<Integer, String[]> getKeyMappings() {
        return keyMappings;
    }
}
