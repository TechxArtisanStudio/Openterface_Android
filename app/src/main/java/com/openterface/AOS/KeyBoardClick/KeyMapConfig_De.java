package com.openterface.AOS.KeyBoardClick;

import com.openterface.AOS.R;
import com.openterface.AOS.target.KeyBoardMapping;

import java.util.HashMap;
import java.util.Map;

public class KeyMapConfig_De implements KeyBoardMapping {
    private final Map<Integer, String[]> keyMappings = new HashMap<>();

    @Override
    public Map<Integer, String[]> getKeyMappings() {
        return keyMappings;
    }

    public KeyMapConfig_De() {
        keyMappings.put(R.id.One_Sigh_Button, new String[]{"1", "!"});
        keyMappings.put(R.id.Two_At_Button, new String[]{"2", "\""});
        keyMappings.put(R.id.Three_Pound_Button, new String[]{"3", "§"});
        keyMappings.put(R.id.Four_Dollar_Button, new String[]{"4", "$"});
        keyMappings.put(R.id.Five_Percent_Button, new String[]{"5", "%"});
        keyMappings.put(R.id.Six_Caret_Button, new String[]{"6", "&"});
        keyMappings.put(R.id.Seven_Ampersand_Button, new String[]{"7", "/"});
        keyMappings.put(R.id.Eight_Asterisk_Button, new String[]{"8", "("});
        keyMappings.put(R.id.Nine_Left_Parenthesis_Button, new String[]{"9", ")"});
        keyMappings.put(R.id.Zero_Right_Parenthesis_Button, new String[]{"0", "="});

        keyMappings.put(R.id.Q_Button, new String[]{"q", "Q"});
        keyMappings.put(R.id.W_Button, new String[]{"w", "W"});
        keyMappings.put(R.id.E_Button, new String[]{"e", "E"});
        keyMappings.put(R.id.R_Button, new String[]{"r", "R"});
        keyMappings.put(R.id.T_Button, new String[]{"t", "T"});
        keyMappings.put(R.id.Y_Button, new String[]{"z", "Z"});
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

        keyMappings.put(R.id.Z_Button, new String[]{"y", "Y"});
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
        keyMappings.put(R.id.PrtSc, new String[]{"Druck", "Druck"});
        keyMappings.put(R.id.ScrLk, new String[]{"Rollen", "Rollen"});
        keyMappings.put(R.id.Pause, new String[]{"Pause", "Pause"});
        keyMappings.put(R.id.Ins, new String[]{"Einfg", "Einfg"});
        keyMappings.put(R.id.Home, new String[]{"Pos1", "Pos1"});
        keyMappings.put(R.id.PgUp, new String[]{"BildUp", "BildUp"});
        keyMappings.put(R.id.Delete, new String[]{"Entf", "Entf"});
        keyMappings.put(R.id.End, new String[]{"Ende", "Ende"});
        keyMappings.put(R.id.PgDn, new String[]{"BildDn", "BildDn"});
        keyMappings.put(R.id.Esc, new String[]{"Esc", "Esc"});

        keyMappings.put(R.id.TAB, new String[]{"TAB", "TAB"});
        keyMappings.put(R.id.dropLeft, new String[]{"DPAD_LEFT", "DPAD_LEFT"});
        keyMappings.put(R.id.dropRight, new String[]{"DPAD_RIGHT", "DPAD_RIGHT"});
        keyMappings.put(R.id.dropUp, new String[]{"DPAD_UP", "DPAD_UP"});
        keyMappings.put(R.id.dropDown, new String[]{"DPAD_DOWN", "DPAD_DOWN"});
        keyMappings.put(R.id.Minus_Sign_Button, new String[]{"ß", "?"});
        keyMappings.put(R.id.Plus_Sign_Button, new String[]{"´", "`"});
        keyMappings.put(R.id.Left_Bracket_Button, new String[]{"ü", "Ü"});

        keyMappings.put(R.id.Right_Bracket_Button, new String[]{"+", "*"});
        keyMappings.put(R.id.Colon_Button, new String[]{"ö", "Ö"});
        keyMappings.put(R.id.Quotation_Button, new String[]{"ä", "Ä"});
        keyMappings.put(R.id.Bitwise_OR_Button, new String[]{"#", "'"});
        keyMappings.put(R.id.Less_Sign_Button, new String[]{",", ";"});
        keyMappings.put(R.id.Greater_Sign_Button, new String[]{".", ":"});
        keyMappings.put(R.id.Question_Mark, new String[]{"-", "_"});
        keyMappings.put(R.id.Tilde, new String[]{"^", "°"});
        keyMappings.put(R.id.Left_Than_Button, new String[]{"<", ">"});
    }

    public static Map<Object, String> getKeyCodeMap() {
        Map<Object, String> keyCodeMap = new HashMap<>();
        keyCodeMap.put("prefix1", "57");
        keyCodeMap.put("prefix2", "AB");
        keyCodeMap.put("address", "00");

        //click key
        keyCodeMap.put("A", "04");
        keyCodeMap.put("B", "05");
        keyCodeMap.put("C", "06");
        keyCodeMap.put("D", "07");
        keyCodeMap.put("E", "08");
        keyCodeMap.put("F", "09");
        keyCodeMap.put("G", "0A");
        keyCodeMap.put("H", "0B");
        keyCodeMap.put("I", "0C");
        keyCodeMap.put("J", "0D");
        keyCodeMap.put("K", "0E");
        keyCodeMap.put("L", "0F");
        keyCodeMap.put("M", "10");
        keyCodeMap.put("N", "11");
        keyCodeMap.put("O", "12");
        keyCodeMap.put("P", "13");
        keyCodeMap.put("Q", "14");
        keyCodeMap.put("R", "15");
        keyCodeMap.put("S", "16");
        keyCodeMap.put("T", "17");
        keyCodeMap.put("U", "18");
        keyCodeMap.put("V", "19");
        keyCodeMap.put("W", "1A");
        keyCodeMap.put("X", "1B");
        keyCodeMap.put("Y", "1D");
        keyCodeMap.put("Z", "1C");
        keyCodeMap.put("a", "04");
        keyCodeMap.put("b", "05");
        keyCodeMap.put("c", "06");
        keyCodeMap.put("d", "07");
        keyCodeMap.put("e", "08");
        keyCodeMap.put("f", "09");
        keyCodeMap.put("g", "0A");
        keyCodeMap.put("h", "0B");
        keyCodeMap.put("i", "0C");
        keyCodeMap.put("j", "0D");
        keyCodeMap.put("k", "0E");
        keyCodeMap.put("l", "0F");
        keyCodeMap.put("m", "10");
        keyCodeMap.put("n", "11");
        keyCodeMap.put("o", "12");
        keyCodeMap.put("p", "13");
        keyCodeMap.put("q", "14");
        keyCodeMap.put("r", "15");
        keyCodeMap.put("s", "16");
        keyCodeMap.put("t", "17");
        keyCodeMap.put("u", "18");
        keyCodeMap.put("v", "19");
        keyCodeMap.put("w", "1A");
        keyCodeMap.put("x", "1B");
        keyCodeMap.put("y", "1D");
        keyCodeMap.put("z", "1C");

        keyCodeMap.put("1", "1E");keyCodeMap.put("!", "1E");
        keyCodeMap.put("2", "1F");keyCodeMap.put("\"", "1F");
        keyCodeMap.put("3", "20");keyCodeMap.put("§", "20");
        keyCodeMap.put("4", "21");keyCodeMap.put("$", "21");
        keyCodeMap.put("5", "22");keyCodeMap.put("%", "22");
        keyCodeMap.put("6", "23");keyCodeMap.put("&", "23");
        keyCodeMap.put("7", "24");keyCodeMap.put("/", "24");
        keyCodeMap.put("8", "25");keyCodeMap.put("(", "25");
        keyCodeMap.put("9", "26");keyCodeMap.put(")", "26");
        keyCodeMap.put("0", "27");keyCodeMap.put("=", "27");
        keyCodeMap.put("^", "35");keyCodeMap.put("°", "35");
        keyCodeMap.put("ß", "2D");keyCodeMap.put("?", "2D");
        keyCodeMap.put("´", "2E");keyCodeMap.put("`", "2E");
        keyCodeMap.put("ü", "2F");keyCodeMap.put("Ü", "2F");
        keyCodeMap.put("+", "30");keyCodeMap.put("*", "30");
        keyCodeMap.put("ö", "33");keyCodeMap.put("Ö", "33");
        keyCodeMap.put("ä", "34");keyCodeMap.put("Ä", "34");
        keyCodeMap.put("#", "31");keyCodeMap.put("'", "31");
        keyCodeMap.put(",", "36");keyCodeMap.put(";", "36");
        keyCodeMap.put(".", "37");keyCodeMap.put(":", "37");
        keyCodeMap.put("-", "38");keyCodeMap.put("_", "38");
        keyCodeMap.put("<", "64");keyCodeMap.put(">", "64");

        //function key
        keyCodeMap.put("GRAVE", "35");
        keyCodeMap.put("MINUS", "2D");
        keyCodeMap.put("EQUALS", "2E");
        keyCodeMap.put("LEFT_BRACKET", "2F");
        keyCodeMap.put("RIGHT_BRACKET", "30");
        keyCodeMap.put("BACKSLASH", "31");
        keyCodeMap.put("SEMICOLON", "33");
        keyCodeMap.put("APOSTROPHE", "34");
        keyCodeMap.put("COMMA", "36");
        keyCodeMap.put("PERIOD", "37");
        keyCodeMap.put("SLASH", "38");
        keyCodeMap.put("CapsLk", "39");
        keyCodeMap.put("CAPS_LOCK", "39");

        keyCodeMap.put("F1", "3A");
        keyCodeMap.put("F2", "3B");
        keyCodeMap.put("F3", "3C");
        keyCodeMap.put("F4", "3D");
        keyCodeMap.put("F5", "3E");
        keyCodeMap.put("F6", "3F");
        keyCodeMap.put("F7", "40");
        keyCodeMap.put("F8", "41");
        keyCodeMap.put("F9", "42");
        keyCodeMap.put("F10", "43");
        keyCodeMap.put("F11", "44");
        keyCodeMap.put("F12", "45");
        keyCodeMap.put("SYSRQ", "46");
        keyCodeMap.put("SCROLL_LOCK", "47");
        keyCodeMap.put("BREAK", "48");
        keyCodeMap.put("INSERT", "49");
        keyCodeMap.put("FORWARD_DEL", "4C");
        keyCodeMap.put("MOVE_HOME", "4A");
        keyCodeMap.put("PAGE_UP", "4B");

        keyCodeMap.put("MOVE_END", "4D");
        keyCodeMap.put("PAGE_DOWN", "4E");

        keyCodeMap.put("CTRL_LEFT", "E0");
        keyCodeMap.put("SHIFT_LEFT", "E1");
        keyCodeMap.put("ALT_LEFT", "E2");
        keyCodeMap.put("Win", "E3");
        keyCodeMap.put("CTRL_RIGHT", "E4");
        keyCodeMap.put("SHIFT_RIGHT", "E5");
        keyCodeMap.put("ALT_RIGHT", "E6");
        keyCodeMap.put("TAB", "2B");
        keyCodeMap.put("SPACE", "2C");
        keyCodeMap.put("BACK", "29");
        keyCodeMap.put("ENTER", "28");
        keyCodeMap.put("DEL", "2A");
        keyCodeMap.put("Esc", "29");

        keyCodeMap.put("Druck", "46");
        keyCodeMap.put("Rollen", "47");
        keyCodeMap.put("Pause", "48");
        keyCodeMap.put("Einfg", "49");
        keyCodeMap.put("Pos1", "4A");
        keyCodeMap.put("BildUp", "4B");
        keyCodeMap.put("Entf", "4C");
        keyCodeMap.put("Ende", "4D");
        keyCodeMap.put("BildDn", "4E");

        keyCodeMap.put("NumLk", "53");

        keyCodeMap.put("DPAD_LEFT", "50");
        keyCodeMap.put("DPAD_RIGHT", "4F");
        keyCodeMap.put("DPAD_DOWN", "51");
        keyCodeMap.put("DPAD_UP", "52");

        keyCodeMap.put("NUM_LOCK", "53");
        keyCodeMap.put("NUMPAD_DIVIDE", "54");
        keyCodeMap.put("NUMPAD_MULTIPLY", "55");
        keyCodeMap.put("NUMPAD_SUBTRACT", "56");
        keyCodeMap.put("NUMPAD_ADD", "57");

        //release keyboard data
        keyCodeMap.put("release", "57AB00020800000000000000000C");
        keyCodeMap.put("releaseRel", "57AB0005050100000000");

        return keyCodeMap;
    }
}
