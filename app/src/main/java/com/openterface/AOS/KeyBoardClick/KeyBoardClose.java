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
    private final FloatingActionButton keyBoard;
    private final FloatingActionButton set_up_button;

    public KeyBoardClose(MainActivity activity) {
        KeyBoard_Close = activity.findViewById(R.id.KeyBoard_Close);
        keyBoardView = activity.findViewById(R.id.KeyBoard_View);
        keyBoard = activity.findViewById(R.id.keyBoard);
        set_up_button = activity.findViewById(R.id.set_up_button);
        this.context = activity;
    }

    public void setCloseButtonClickColor(){
        KeyBoard_Close.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);//open keyboard
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                keyBoardView.setVisibility(View.GONE);

                keyBoard.setVisibility(View.VISIBLE);
                set_up_button.setVisibility(View.VISIBLE);
            }
        });
    }
}
