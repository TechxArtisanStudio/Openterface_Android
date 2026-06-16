/**
* @Title: KeyBoardClose
* @Package com.openterface.AOS.KeyBoardClick
* @Description:
 * ========================================================================== *
 *                                                                            *
 *    This file is part of the Openterface Mini KVM App Android version       *
 *                                                                            *
 *    Copyright (C) 2024   <info@openterface.com>                             *
 *                                                                            *
 *    This program is free software: you can redistribute it and/or modify    *
 *    it under the terms of the GNU General Public License as published by    *
 *    the Free Software Foundation version 3.                                 *
 *                                                                            *
 *    This program is distributed in the hope that it will be useful, but     *
 *    WITHOUT ANY WARRANTY; without even the implied warranty of              *
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU        *
 *    General Public License for more details.                                *
 *                                                                            *
 *    You should have received a copy of the GNU General Public License       *
 *    along with this program. If not, see <http://www.gnu.org/licenses/>.    *
 *                                                                            *
 * ========================================================================== *
*/
package com.openterface.AOS.KeyBoardClick;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.openterface.AOS.R;
import com.openterface.AOS.activity.MainActivity;

public class KeyBoardClose {
    private final Context context;
    private final ImageButton KeyBoard_Close;
    private final LinearLayout keyBoardView;
    private final FloatingActionButton FloatKeyBoard;
    private final FloatingActionButton FloatSetUpButton;

    public KeyBoardClose(View rootView) {
        KeyBoard_Close = rootView.findViewById(R.id.KeyBoard_Close);
        keyBoardView = rootView.findViewById(R.id.KeyBoard_View);
        // These views may not exist in portrait mode - add null checks
        FloatKeyBoard = rootView.findViewById(R.id.keyBoard);
        FloatSetUpButton = rootView.findViewById(R.id.set_up_button);
        this.context = rootView.getContext();
    }

    public void setCloseButtonClickColor(){
        if (KeyBoard_Close == null) return;

        KeyBoard_Close.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

                // Only execute if views exist (landscape mode)
                if (FloatKeyBoard != null && FloatSetUpButton != null && keyBoardView != null) {
                    FloatKeyBoard.setVisibility(View.VISIBLE);
                    FloatSetUpButton.setVisibility(View.VISIBLE);
                    keyBoardView.setVisibility(View.GONE);
                } else {
                    // Portrait mode: hide the keyboard module
                    // The parent view will handle this through OnCloseListener
                    View parent = (View) v.getParent();
                    while (parent != null && parent.getId() != R.id.keyboard_module_root) {
                        parent = (View) parent.getParent();
                    }
                    if (parent != null) {
                        parent.setVisibility(View.GONE);
                    }
                }
            }
        });
    }
}
