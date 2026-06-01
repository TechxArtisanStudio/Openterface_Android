package com.openterface.AOS.KeyBoardClick;

import android.widget.Button;

import com.openterface.AOS.R;
import com.openterface.AOS.activity.MainActivity;

public class KeyBoardWin {
    private final Button KeyBoard_Win;
    private boolean KeyBoard_Win_Press = false;
    private boolean isLocked = false;

    public KeyBoardWin(MainActivity activity) {
        KeyBoard_Win = activity.findViewById(R.id.KeyBoard_Win);
        new ModifierKeyHelper(KeyBoard_Win, new ModifierKeyHelper.ModifierCallback() {
            @Override public void onPress() {
                KeyBoard_Win.setBackgroundResource(R.drawable.press_button_background);
                KeyBoard_Win_Press = true;
                KeyBoardFunction.KeyBoard_Win_Press(true);
                KeyBoardSystem.KeyBoard_Win_Press(true);
            }
            @Override public void onRelease() {
                if (!isLocked) KeyBoard_Win.setBackgroundResource(R.drawable.nopress_button_background);
                if (!isLocked) {
                    KeyBoard_Win_Press = false;
                    KeyBoardFunction.KeyBoard_Win_Press(false);
                    KeyBoardSystem.KeyBoard_Win_Press(false);
                }
            }
            @Override public void onLock() {
                isLocked = true;
                KeyBoard_Win_Press = true;
                KeyBoardFunction.KeyBoard_Win_Press(true);
                KeyBoardSystem.KeyBoard_Win_Press(true);
            }
            @Override public void onUnlock() {
                isLocked = false;
                KeyBoard_Win_Press = false;
                KeyBoard_Win.setBackgroundResource(R.drawable.nopress_button_background);
                KeyBoardFunction.KeyBoard_Win_Press(false);
                KeyBoardSystem.KeyBoard_Win_Press(false);
            }
            @Override public boolean isLocked() { return isLocked; }
            @Override public boolean isPressed() { return KeyBoard_Win_Press; }
        });
    }
}
