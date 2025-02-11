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

    public KeyBoardClose(MainActivity activity) {
        KeyBoard_Close = activity.findViewById(R.id.KeyBoard_Close);
        keyBoardView = activity.findViewById(R.id.KeyBoard_View);
        FloatKeyBoard = activity.findViewById(R.id.keyBoard);
        FloatSetUpButton = activity.findViewById(R.id.set_up_button);
        this.context = activity;
    }

    public void setCloseButtonClickColor(){
        KeyBoard_Close.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);//open keyboard
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                keyBoardView.setVisibility(View.GONE);

                FloatKeyBoard.setVisibility(View.VISIBLE);
                FloatSetUpButton.setVisibility(View.VISIBLE);
            }
        });
    }
}
