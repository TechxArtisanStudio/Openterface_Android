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
                    KeyBoard_Shift.setBackgroundResource(R.drawable.press_button_background);
                }else{
                    com.openterface.AOS.KeyBoardClick.KeyBoardFunction.KeyBoard_ShIft_Press(false);
                    KeyBoard_Shift.setBackgroundResource(R.drawable.nopress_button_background);
                }
                KeyBoard_ShIft_Press = !KeyBoard_ShIft_Press;
            }
        });
    }
}
