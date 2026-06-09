package com.openterface.AOS.view;

import android.content.Context;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.openterface.AOS.R;

/**
 * Key preview popup - shows a small bubble directly above the pressed key
 * Reference: KeyCMD BasicKeyPreview implementation
 */
public class KeyPreviewPopup {
    private final Context context;
    private PopupWindow popupWindow;
    private TextView textView;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable dismissRunnable;

    public KeyPreviewPopup(Context context) {
        this.context = context;
        initPopup();
    }

    private void initPopup() {
        View view = LayoutInflater.from(context).inflate(R.layout.key_preview_popup, null);
        textView = view.findViewById(R.id.key_preview_text);

        popupWindow = new PopupWindow(view,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow.setClippingEnabled(true);
        popupWindow.setTouchable(false);
        popupWindow.setFocusable(false);
    }

    /**
     * Show popup directly above the anchor view (centered horizontally)
     * Reference: KeyCMD BasicKeyPreview implementation for proper positioning
     * @param anchor The button being pressed
     * @param keyLabel Text to display (e.g., "A", "Ctrl", "Alt")
     */
    public void show(View anchor, String keyLabel) {
        if (popupWindow == null || anchor == null || keyLabel == null || keyLabel.isEmpty()) {
            return;
        }

        textView.setText(keyLabel);

        // Measure the popup to get its size
        textView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );
        int popupWidth = textView.getMeasuredWidth();
        int popupHeight = textView.getMeasuredHeight();

        // Get anchor position on screen
        int[] location = new int[2];
        anchor.getLocationOnScreen(location);
        int anchorLeft = location[0];
        int anchorTop = location[1];
        int anchorWidth = anchor.getWidth();
        int anchorHeight = anchor.getHeight();

        // Get visible display frame to prevent popup from going off-screen
        Rect visible = new Rect();
        anchor.getWindowVisibleDisplayFrame(visible);

        // Calculate gap and margin in pixels
        int gap = (int) (4 * context.getResources().getDisplayMetrics().density);
        int margin = (int) (8 * context.getResources().getDisplayMetrics().density);

        // Calculate horizontal position (centered above the key)
        int popupX = anchorLeft + (anchorWidth - popupWidth) / 2;
        // Constrain to visible area with margins
        popupX = Math.max(visible.left + margin, Math.min(popupX, visible.right - popupWidth - margin));

        // Calculate vertical position (above the key)
        int aboveY = anchorTop - popupHeight - gap;
        int belowY = anchorTop + anchorHeight + gap;

        int popupY;
        // Try to show above first
        if (aboveY >= visible.top + margin) {
            popupY = aboveY;
        } else if (belowY + popupHeight <= visible.bottom - margin) {
            // If not enough space above, show below
            popupY = belowY;
        } else {
            // If neither works well, constrain to visible area
            popupY = Math.max(visible.top + margin, Math.min(aboveY, visible.bottom - popupHeight - margin));
        }

        // Cancel previous dismiss task
        if (dismissRunnable != null) {
            handler.removeCallbacks(dismissRunnable);
        }

        if (popupWindow.isShowing()) {
            popupWindow.update(popupX, popupY, -1, -1);
        } else {
            try {
                popupWindow.showAtLocation(anchor, Gravity.NO_GRAVITY, popupX, popupY);
            } catch (Exception e) {
                // Ignore display exceptions (e.g., anchor detached from window)
            }
        }
    }

    /**
     * Dismiss the popup immediately (called on key release)
     */
    public void dismiss() {
        // Cancel any pending dismiss task
        if (dismissRunnable != null) {
            handler.removeCallbacks(dismissRunnable);
            dismissRunnable = null;
        }

        if (popupWindow != null && popupWindow.isShowing()) {
            try {
                popupWindow.dismiss();
            } catch (Exception e) {
                // Ignore
            }
        }
    }
}
