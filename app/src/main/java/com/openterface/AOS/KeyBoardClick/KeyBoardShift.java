/**
* @Title: KeyBoardShift
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

import android.view.View;
import android.widget.Button;

import com.openterface.AOS.R;
import com.openterface.AOS.activity.MainActivity;

public class KeyBoardShift {
    private final Button KeyBoard_Shift;
    private boolean KeyBoard_ShIft_Press = false;

    public KeyBoardShift(MainActivity activity) {
        KeyBoard_Shift = activity.findViewById(R.id.KeyBoard_Shift);
    }

    public void setShiftButtonClickColor(){
        KeyBoard_Shift.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!KeyBoard_ShIft_Press) {
                    com.openterface.AOS.KeyBoardClick.KeyBoardFunction.KeyBoard_ShIft_Press(true);
                    com.openterface.AOS.KeyBoardClick.KeyBoardSystem.KeyBoard_ShIft_Press(true);
                    KeyBoard_Shift.setBackgroundResource(R.drawable.press_button_background);
                }else{
                    com.openterface.AOS.KeyBoardClick.KeyBoardFunction.KeyBoard_ShIft_Press(false);
                    com.openterface.AOS.KeyBoardClick.KeyBoardSystem.KeyBoard_ShIft_Press(false);
                    KeyBoard_Shift.setBackgroundResource(R.drawable.nopress_button_background);
                }
                KeyBoard_ShIft_Press = !KeyBoard_ShIft_Press;
            }
        });
    }
}
