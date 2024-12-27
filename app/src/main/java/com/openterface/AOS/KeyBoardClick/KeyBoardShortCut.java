package com.openterface.AOS.KeyBoardClick;

import android.widget.Button;

import com.openterface.AOS.R;
import com.openterface.AOS.activity.MainActivity;
import com.openterface.AOS.target.KeyBoardManager;

public class KeyBoardShortCut {
    private final Button[] ShortCutButtons;

    public KeyBoardShortCut(MainActivity activity) {
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
        switch (buttonId) {
            case R.id.Ctrl_C: return "C";
            case R.id.Ctrl_V: return "V";
            case R.id.Ctrl_Z: return "Z";
            case R.id.Ctrl_X: return "X";
            case R.id.Ctrl_A: return "A";
            case R.id.Ctrl_S: return "S";
            case R.id.Win_Tab: return "TAB";
            case R.id.Win_S: return "S";
            case R.id.Win_E: return "E";
            case R.id.Win_R: return "R";
            case R.id.Win_D: return "D";
            case R.id.Win_L: return "L";
            case R.id.Alt_F4: return "F4";
            case R.id.Alt_PrtScr: return "PrtSc";
            default: return "";
        }
    }

    private void handleShortcut(String modifier, String key) {
        KeyBoardManager.sendKeyBoardShortCut(modifier, key);
    }
}
