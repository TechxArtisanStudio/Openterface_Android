package com.openterface.AOS.KeyBoardClick;

import android.view.View;
import android.widget.Button;

import com.openterface.AOS.R;
import com.openterface.AOS.activity.MainActivity;

public class KeyBoardCtrl
{
    private final Button KeyBoard_Ctrl;
    private boolean KeyBoard_Ctrl_Press = false;

    public KeyBoardCtrl(MainActivity activity) {
        KeyBoard_Ctrl = activity.findViewById(R.id.KeyBoard_Ctrl);
    }

    public void setCtrlButtonClickColor(){
        KeyBoard_Ctrl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!KeyBoard_Ctrl_Press) {
                    com.openterface.AOS.KeyBoardClick.KeyBoardFunction.KeyBoard_Ctrl_Press(true);
                    com.openterface.AOS.KeyBoardClick.KeyBoardSystem.KeyBoard_Ctrl_Press(true);
                    KeyBoard_Ctrl.setBackgroundResource(R.drawable.press_button_background);
                }else{
                    com.openterface.AOS.KeyBoardClick.KeyBoardFunction.KeyBoard_Ctrl_Press(false);
                    com.openterface.AOS.KeyBoardClick.KeyBoardSystem.KeyBoard_Ctrl_Press(false);
                    KeyBoard_Ctrl.setBackgroundResource(R.drawable.nopress_button_background);
                }
                KeyBoard_Ctrl_Press = !KeyBoard_Ctrl_Press;
            }
        });
    }
}
