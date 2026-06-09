package com.openterface.AOS.KeyBoardClick;

import android.view.View;
import android.widget.Button;

import com.openterface.AOS.R;
import com.openterface.AOS.view.KeyPreviewPopup;

public class KeyBoardWin {
    private final Button KeyBoard_Win;
    private boolean KeyBoard_Win_Press = false;
    private boolean isLocked = false;
    private final KeyPreviewPopup popup;

    public KeyBoardWin(View rootView, KeyPreviewPopup popup) {
        this.popup = popup;
        KeyBoard_Win = rootView.findViewById(R.id.KeyBoard_Win);
        if (KeyBoard_Win == null) return;
        new ModifierKeyHelper(KeyBoard_Win, new ModifierKeyHelper.ModifierCallback() {
            @Override public void onPress() {
                KeyBoard_Win.setBackgroundResource(R.drawable.press_button_background);
                KeyBoard_Win_Press = true;
                KeyBoardFunction.KeyBoard_Win_Press(true);
                KeyBoardSystem.KeyBoard_Win_Press(true);
                if (popup != null) popup.show(KeyBoard_Win, "Win");
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
