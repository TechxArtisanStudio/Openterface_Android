package com.openterface.AOS.KeyBoardClick;

import android.view.View;
import android.widget.Button;

import com.openterface.AOS.R;
import com.openterface.AOS.activity.MainActivity;

public class KeyBoardAlt {
    private final Button KeyBoard_Alt;
    private boolean KeyBoard_Alt_Press = false;

    public KeyBoardAlt(MainActivity activity) {
        KeyBoard_Alt = activity.findViewById(R.id.KeyBoard_Alt);
    }

    public void setAltButtonClickColor(){
        KeyBoard_Alt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!KeyBoard_Alt_Press) {
                    com.openterface.AOS.KeyBoardClick.KeyBoardFunction.KeyBoard_Alt_Press(true);
                    com.openterface.AOS.KeyBoardClick.KeyBoardSystem.KeyBoard_Alt_Press(true);
                    KeyBoard_Alt.setBackgroundResource(R.drawable.press_button_background);
                }else{
                    com.openterface.AOS.KeyBoardClick.KeyBoardFunction.KeyBoard_Alt_Press(false);
                    com.openterface.AOS.KeyBoardClick.KeyBoardSystem.KeyBoard_Alt_Press(false);
                    KeyBoard_Alt.setBackgroundResource(R.drawable.nopress_button_background);
                }
                KeyBoard_Alt_Press = !KeyBoard_Alt_Press;
            }
        });
    }
}
