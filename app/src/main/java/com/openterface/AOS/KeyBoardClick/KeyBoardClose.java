package com.openterface.AOS.KeyBoardClick;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.openterface.AOS.R;
import com.openterface.AOS.activity.MainActivity;

public class KeyBoardClose {
    private final Context context;
    private final ImageButton KeyBoard_Close;
    private final LinearLayout keyBoardView;

    public KeyBoardClose(MainActivity activity) {
        KeyBoard_Close = activity.findViewById(R.id.KeyBoard_Close);
        keyBoardView = activity.findViewById(R.id.KeyBoard_View);
        this.context = activity;
    }

    public void setCloseButtonClickColor(){
        KeyBoard_Close.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);//open keyboard
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                keyBoardView.setVisibility(View.GONE);
            }
        });
    }
}
