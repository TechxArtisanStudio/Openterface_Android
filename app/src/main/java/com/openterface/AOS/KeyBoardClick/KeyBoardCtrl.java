package com.openterface.AOS.KeyBoardClick;

import android.view.View;
import android.widget.Button;

import com.openterface.AOS.R;

public class KeyBoardCtrl {
    private final Button KeyBoard_Ctrl;
    private boolean KeyBoard_Ctrl_Press = false;
    private boolean isLocked = false;

    public KeyBoardCtrl(View rootView) {
        KeyBoard_Ctrl = rootView.findViewById(R.id.KeyBoard_Ctrl);
        if (KeyBoard_Ctrl == null) return;
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
