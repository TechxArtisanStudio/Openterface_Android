package com.openterface.AOS.KeyBoardClick;

import android.widget.Button;

import com.openterface.AOS.R;
import com.openterface.AOS.activity.MainActivity;

public class KeyBoardCtrl {
    private final Button KeyBoard_Ctrl;
    private boolean KeyBoard_Ctrl_Press = false;
    private boolean isLocked = false;

    public KeyBoardCtrl(MainActivity activity) {
        KeyBoard_Ctrl = activity.findViewById(R.id.KeyBoard_Ctrl);
        new ModifierKeyHelper(KeyBoard_Ctrl, new ModifierKeyHelper.ModifierCallback() {
            @Override public void onPress() {
                KeyBoard_Ctrl.setBackgroundResource(R.drawable.press_button_background);
                KeyBoard_Ctrl_Press = true;
                KeyBoardFunction.KeyBoard_Ctrl_Press(true);
                KeyBoardSystem.KeyBoard_Ctrl_Press(true);
            }
            @Override public void onRelease() {
                if (!isLocked) {
                    KeyBoard_Ctrl.setBackgroundResource(R.drawable.nopress_button_background);
                }
                if (!isLocked) {
                    KeyBoard_Ctrl_Press = false;
                    KeyBoardFunction.KeyBoard_Ctrl_Press(false);
                    KeyBoardSystem.KeyBoard_Ctrl_Press(false);
                }
            }
            @Override public void onLock() {
                isLocked = true;
                KeyBoard_Ctrl_Press = true;
                KeyBoardFunction.KeyBoard_Ctrl_Press(true);
                KeyBoardSystem.KeyBoard_Ctrl_Press(true);
            }
            @Override public void onUnlock() {
                isLocked = false;
                KeyBoard_Ctrl_Press = false;
                KeyBoard_Ctrl.setBackgroundResource(R.drawable.nopress_button_background);
                KeyBoardFunction.KeyBoard_Ctrl_Press(false);
                KeyBoardSystem.KeyBoard_Ctrl_Press(false);
            }
            @Override public boolean isLocked() { return isLocked; }
            @Override public boolean isPressed() { return KeyBoard_Ctrl_Press; }
        });
    }
}
