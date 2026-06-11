package com.openterface.AOS.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;

import com.openterface.AOS.R;
import com.openterface.AOS.manager.TargetOsManager;

/**
 * Dialog for selecting target operating system
 * Changes the Win/Cmd/Super key label on the keyboard
 */
public class TargetOsPickerDialog extends Dialog {

    private OnTargetOsSelectedListener listener;
    private TargetOsManager.TargetOS currentOs;

    public interface OnTargetOsSelectedListener {
        void onTargetOsSelected(TargetOsManager.TargetOS os);
    }

    public TargetOsPickerDialog(@NonNull Context context) {
        super(context);
        currentOs = TargetOsManager.getInstance().getCurrentOs();
    }

    public void setOnTargetOsSelectedListener(OnTargetOsSelectedListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_target_os_picker);

        RadioGroup radioGroup = findViewById(R.id.radioGroupTargetOs);
        RadioButton radioWindows = findViewById(R.id.radioWindows);
        RadioButton radioMacos = findViewById(R.id.radioMacos);
        RadioButton radioLinux = findViewById(R.id.radioLinux);

        // Set current selection
        switch (currentOs) {
            case WINDOWS:
                radioWindows.setChecked(true);
                break;
            case MACOS:
                radioMacos.setChecked(true);
                break;
            case LINUX:
                radioLinux.setChecked(true);
                break;
        }

        // Handle selection change
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            TargetOsManager.TargetOS selectedOs = null;

            if (checkedId == R.id.radioWindows) {
                selectedOs = TargetOsManager.TargetOS.WINDOWS;
            } else if (checkedId == R.id.radioMacos) {
                selectedOs = TargetOsManager.TargetOS.MACOS;
            } else if (checkedId == R.id.radioLinux) {
                selectedOs = TargetOsManager.TargetOS.LINUX;
            }

            if (selectedOs != null && listener != null) {
                listener.onTargetOsSelected(selectedOs);
            }
        });
    }
}
