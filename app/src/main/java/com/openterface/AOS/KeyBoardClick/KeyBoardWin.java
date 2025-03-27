package com.openterface.AOS.KeyBoardClick;

import android.view.View;
import android.widget.Button;

import com.openterface.AOS.R;
import com.openterface.AOS.activity.MainActivity;

public class KeyBoardWin {
    private final Button KeyBoard_Win;
    private boolean KeyBoard_Win_Press = false;

    public KeyBoardWin(MainActivity activity) {
        KeyBoard_Win = activity.findViewById(R.id.KeyBoard_Win);
    }

    public void setWinButtonClickColor(){
        KeyBoard_Win.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!KeyBoard_Win_Press) {
                    com.openterface.AOS.KeyBoardClick.KeyBoardFunction.KeyBoard_Win_Press(true);
                    com.openterface.AOS.KeyBoardClick.KeyBoardSystem.KeyBoard_Win_Press(true);
                    KeyBoard_Win.setBackgroundResource(R.drawable.press_button_background);
                }else{
                    com.openterface.AOS.KeyBoardClick.KeyBoardFunction.KeyBoard_Win_Press(false);
                    com.openterface.AOS.KeyBoardClick.KeyBoardSystem.KeyBoard_Win_Press(false);
                    KeyBoard_Win.setBackgroundResource(R.drawable.nopress_button_background);
                }
                KeyBoard_Win_Press = !KeyBoard_Win_Press;
            }
        });
    }
}
