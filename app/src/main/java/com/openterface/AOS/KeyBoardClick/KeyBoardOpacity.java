/**
* @Title: KeyBoardOpacity
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

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.openterface.AOS.R;
import com.openterface.AOS.activity.MainActivity;

public class KeyBoardOpacity {
    private static final String PREF_KEY = "keyboard_opacity";
    private static final int DEFAULT_OPACITY = 100;

    private final ImageButton KeyBoard_Opacity;
    private final LinearLayout keyBoardView;
    private final android.content.SharedPreferences prefs;

    public KeyBoardOpacity(MainActivity activity) {
        KeyBoard_Opacity = activity.findViewById(R.id.KeyBoard_Opacity);
        keyBoardView = activity.findViewById(R.id.KeyBoard_View);
        prefs = activity.getPreferences(Context.MODE_PRIVATE);
    }

    public void setOpacityButtonClick() {
        KeyBoard_Opacity.setOnClickListener(v -> showOpacityDialog());
    }

    private void showOpacityDialog() {
        int currentOpacity = prefs.getInt(PREF_KEY, DEFAULT_OPACITY);

        View dialogView = View.inflate(keyBoardView.getContext(), R.layout.dialog_opacity, null);
        SeekBar seekBar = dialogView.findViewById(R.id.opacity_seekbar);
        TextView opacityText = dialogView.findViewById(R.id.opacity_text);

        seekBar.setProgress(currentOpacity);
        opacityText.setText(currentOpacity + "%");

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                opacityText.setText(progress + "%");
                keyBoardView.setAlpha(progress / 100f);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                prefs.edit().putInt(PREF_KEY, seekBar.getProgress()).apply();
            }
        });

        new AlertDialog.Builder(keyBoardView.getContext())
                .setTitle("调整键盘透明度")
                .setView(dialogView)
                .setPositiveButton("确定", null)
                .show();
    }

    /**
     * Restore the saved keyboard opacity.
     */
    public void restoreOpacity() {
        int savedOpacity = prefs.getInt(PREF_KEY, DEFAULT_OPACITY);
        keyBoardView.setAlpha(savedOpacity / 100f);
    }
}