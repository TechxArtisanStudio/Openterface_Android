package com.openterface.AOS.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;

import com.openterface.AOS.R;

/**
 * Portrait keyboard view for Openterface Android.
 * Loads keyboard panels (system/function) with Material Design styling.
 * Key event handling is delegated to the existing KeyBoard* classes.
 */
public class PortraitKeyboardView extends LinearLayout {

    private static final String TAG = "OP-UI";

    // Keyboard panels
    private View systemPanel;    // QWERTY keyboard (5 rows)
    private View functionPanel;  // Function keys (F1-F12, shortcuts)

    public PortraitKeyboardView(Context context) {
        super(context);
        init(context);
    }

    public PortraitKeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PortraitKeyboardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setOrientation(VERTICAL);
        // Use ContextCompat for proper theme-aware color resolution
        setBackgroundColor(ContextCompat.getColor(context, R.color.background_light));

        // Load keyboard panels
        systemPanel = loadPanel(context, R.layout.system_button);
        functionPanel = loadPanel(context, R.layout.function_button);

        // Default: show system (QWERTY) panel
        showSystemPanel();
    }

    /**
     * Show the QWERTY keyboard panel.
     */
    public void showSystemPanel() {
        if (systemPanel != null) systemPanel.setVisibility(VISIBLE);
        if (functionPanel != null) functionPanel.setVisibility(GONE);
    }

    /**
     * Show the function keys panel (F1-F12, shortcuts).
     */
    public void showFunctionPanel() {
        if (systemPanel != null) systemPanel.setVisibility(GONE);
        if (functionPanel != null) functionPanel.setVisibility(VISIBLE);
    }

    private View loadPanel(Context context, int layoutResId) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View panel = inflater.inflate(layoutResId, this, false);
        addView(panel);
        return panel;
    }
}