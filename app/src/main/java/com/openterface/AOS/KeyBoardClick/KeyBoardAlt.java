package com.openterface.AOS.KeyBoardClick;

import android.view.View;
import android.widget.Button;

import com.openterface.AOS.R;
import com.openterface.AOS.view.KeyPreviewPopup;

public class KeyBoardAlt {
    private final Button KeyBoard_Alt;
    private boolean KeyBoard_Alt_Press = false;
    private boolean isLocked = false;
    private final KeyPreviewPopup popup;

    public KeyBoardAlt(View rootView, KeyPreviewPopup popup) {
        this.popup = popup;
        KeyBoard_Alt = rootView.findViewById(R.id.KeyBoard_Alt);
        if (KeyBoard_Alt == null) return;
        new ModifierKeyHelper(KeyBoard_Alt, new ModifierKeyHelper.ModifierCallback() {
            @Override public void onPress() {
                KeyBoard_Alt.setBackgroundResource(R.drawable.press_button_background);
                KeyBoard_Alt_Press = true;
                KeyBoardFunction.KeyBoard_Alt_Press(true);
                KeyBoardSystem.KeyBoard_Alt_Press(true);
                if (popup != null) popup.show(KeyBoard_Alt, "Alt");
            }
            @Override public void onRelease() {
                if (!isLocked) KeyBoard_Alt.setBackgroundResource(R.drawable.nopress_button_background);
                if (!isLocked) {
                    KeyBoard_Alt_Press = false;
                    KeyBoardFunction.KeyBoard_Alt_Press(false);
                    KeyBoardSystem.KeyBoard_Alt_Press(false);
                }
            }
            @Override public void onLock() {
                isLocked = true;
                KeyBoard_Alt_Press = true;
                KeyBoardFunction.KeyBoard_Alt_Press(true);
                KeyBoardSystem.KeyBoard_Alt_Press(true);
            }
            @Override public void onUnlock() {
                isLocked = false;
                KeyBoard_Alt_Press = false;
                KeyBoard_Alt.setBackgroundResource(R.drawable.nopress_button_background);
                KeyBoardFunction.KeyBoard_Alt_Press(false);
                KeyBoardSystem.KeyBoard_Alt_Press(false);
            }
            @Override public boolean isLocked() { return isLocked; }
            @Override public boolean isPressed() { return KeyBoard_Alt_Press; }
        });
    }
}
