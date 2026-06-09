/**
* @Title: KeyBoardShift
* @Package com.openterface.AOS.KeyBoardClick
* @Description: Shift key with press/hold behavior, long-press swipe-up to lock
 * ========================================================================== *
 *    This file is part of the Openterface Mini KVM App Android version       *
 *    Copyright (C) 2024   <info@openterface.com>                             *
 *    This program is free software: you can redistribute it and/or modify    *
 *    it under the terms of the GNU General Public License as published by    *
 *    the Free Software Foundation version 3.                                 *
 * ========================================================================== *
*/
package com.openterface.AOS.KeyBoardClick;

import android.view.View;
import android.widget.Button;

import com.openterface.AOS.R;

public class KeyBoardShift {
    private final Button KeyBoard_Shift;
    private boolean KeyBoard_ShIft_Press = false;
    private boolean isLocked = false;

    public KeyBoardShift(View rootView) {
        KeyBoard_Shift = rootView.findViewById(R.id.KeyBoard_Shift);
        if (KeyBoard_Shift == null) return;
        new ModifierKeyHelper(KeyBoard_Shift, new ModifierKeyHelper.ModifierCallback() {
            @Override public void onPress() {
                KeyBoard_Shift.setBackgroundResource(R.drawable.press_button_background);
                KeyBoard_ShIft_Press = true;
                KeyBoardFunction.KeyBoard_ShIft_Press(true);
                KeyBoardSystem.KeyBoard_ShIft_Press(true);
            }
            @Override public void onRelease() {
                if (!isLocked) {
                    KeyBoard_Shift.setBackgroundResource(R.drawable.nopress_button_background);
                }
                if (!isLocked) {
                    KeyBoard_ShIft_Press = false;
                    KeyBoardFunction.KeyBoard_ShIft_Press(false);
                    KeyBoardSystem.KeyBoard_ShIft_Press(false);
                }
            }
            @Override public void onLock() {
                isLocked = true;
                KeyBoard_ShIft_Press = true;
                KeyBoardFunction.KeyBoard_ShIft_Press(true);
                KeyBoardSystem.KeyBoard_ShIft_Press(true);
            }
            @Override public void onUnlock() {
                isLocked = false;
                KeyBoard_ShIft_Press = false;
                KeyBoard_Shift.setBackgroundResource(R.drawable.nopress_button_background);
                KeyBoardFunction.KeyBoard_ShIft_Press(false);
                KeyBoardSystem.KeyBoard_ShIft_Press(false);
            }
            @Override public boolean isLocked() { return isLocked; }
            @Override public boolean isPressed() { return KeyBoard_ShIft_Press; }
        });
    }
}
