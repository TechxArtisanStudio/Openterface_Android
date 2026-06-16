package com.openterface.AOS.view;

import android.content.Context;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.openterface.AOS.R;

import java.util.List;

/**
 * Character alternatives popup - shows multiple character options on long press
 * Similar to Gboard/KeyCMD alternates popup
 */
public class CharacterAlternatesPopup {
    private final Context context;
    private PopupWindow popupWindow;
    private LinearLayout container;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private OnCharacterSelectedListener listener;
    private int selectedPosition = -1;
    private List<TextView> optionViews;

    public interface OnCharacterSelectedListener {
        void onCharacterSelected(String character);
        void onDismissed();
    }

    public CharacterAlternatesPopup(Context context) {
        this.context = context;
        initPopup();
    }

    private void initPopup() {
        View view = LayoutInflater.from(context).inflate(R.layout.popup_character_alternates, null);
        container = view.findViewById(R.id.alternates_container);

        popupWindow = new PopupWindow(view,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow.setClippingEnabled(true);
        // CRITICAL: popup must NOT consume touch events.
        // All gestures (move/up) flow through the anchor button, which calls
        // handleMove() / getSelectedCharacter() on this popup.
        // This matches KeyCMD BasicKeyPreview behavior.
        popupWindow.setTouchable(false);
        popupWindow.setFocusable(false);
        popupWindow.setOutsideTouchable(false);

        popupWindow.setOnDismissListener(() -> {
            if (listener != null) {
                listener.onDismissed();
            }
        });
    }

    /**
     * Show popup with character alternatives above the anchor view
     * @param anchor The button being long-pressed
     * @param alternatives List of character options (first is usually the uppercase version)
     * @param listener Callback for character selection (can be null)
     */
    public void show(View anchor, List<String> alternatives, OnCharacterSelectedListener listener) {
        this.listener = listener;
        show(anchor, alternatives);
    }

    /**
     * Show popup with character alternatives above the anchor view (no listener)
     * @param anchor The button being long-pressed
     * @param alternatives List of character options
     */
    public void show(View anchor, List<String> alternatives) {
        if (popupWindow == null || anchor == null || alternatives == null || alternatives.isEmpty()) {
            return;
        }

        container.removeAllViews();
        optionViews = new java.util.ArrayList<>();

        // Create a button for each alternative
        for (int i = 0; i < alternatives.size(); i++) {
            final String character = alternatives.get(i);
            final int position = i;

            TextView optionView = (TextView) LayoutInflater.from(context)
                .inflate(R.layout.item_character_alternate, container, false);
            optionView.setText(character);

            container.addView(optionView);
            optionViews.add(optionView);
        }

        // 默认选中第一个选项（通常是原始字符或大写）
        selectedPosition = 0;
        if (optionViews.size() > 0) {
            optionViews.get(0).setBackgroundResource(R.drawable.alternate_item_selected);
        }

        // Measure the popup
        container.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );
        int popupWidth = container.getMeasuredWidth();
        int popupHeight = container.getMeasuredHeight();

        // Get anchor position
        int[] location = new int[2];
        anchor.getLocationOnScreen(location);
        int anchorLeft = location[0];
        int anchorTop = location[1];
        int anchorWidth = anchor.getWidth();
        int anchorHeight = anchor.getHeight();

        // Get visible display frame
        Rect visible = new Rect();
        anchor.getWindowVisibleDisplayFrame(visible);

        int gap = (int) (4 * context.getResources().getDisplayMetrics().density);
        int margin = (int) (8 * context.getResources().getDisplayMetrics().density);

        // Center popup horizontally over the anchor
        int popupX = anchorLeft + (anchorWidth - popupWidth) / 2;
        popupX = Math.max(visible.left + margin, Math.min(popupX, visible.right - popupWidth - margin));

        // Show above the anchor
        int popupY = anchorTop - popupHeight - gap;
        if (popupY < visible.top + margin) {
            // Not enough space above, show below
            popupY = anchorTop + anchorHeight + gap;
        }

        try {
            popupWindow.showAtLocation(anchor, Gravity.NO_GRAVITY, popupX, popupY);
        } catch (Exception e) {
            // Ignore display exceptions
        }
    }

    private void updateSelection(int position) {
        if (selectedPosition == position) return;

        // Reset previous selection
        if (selectedPosition >= 0 && selectedPosition < optionViews.size()) {
            optionViews.get(selectedPosition).setBackgroundResource(R.drawable.alternate_item_normal);
        }

        // Set new selection
        selectedPosition = position;
        if (position >= 0 && position < optionViews.size()) {
            optionViews.get(position).setBackgroundResource(R.drawable.alternate_item_selected);
        }
    }

    public void dismiss() {
        if (popupWindow != null && popupWindow.isShowing()) {
            try {
                popupWindow.dismiss();
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    public boolean isShowing() {
        return popupWindow != null && popupWindow.isShowing();
    }

    /**
     * Handle move events for swipe-to-select
     * @param rawX raw X coordinate from MotionEvent
     * @param rawY raw Y coordinate from MotionEvent
     */
    public void handleMove(float rawX, float rawY) {
        if (optionViews == null || optionViews.isEmpty()) return;

        for (int i = 0; i < optionViews.size(); i++) {
            TextView optionView = optionViews.get(i);
            Rect rect = new Rect();
            optionView.getGlobalVisibleRect(rect);

            if (rect.contains((int) rawX, (int) rawY)) {
                updateSelection(i);
                return;
            }
        }
        // Finger is outside all options - keep default selection (first option)
        // Don't clear selection, just keep the default
        if (selectedPosition < 0 || selectedPosition >= optionViews.size()) {
            updateSelection(0);
        }
    }

    /**
     * Get the currently selected character based on swipe position
     * @return The selected character, or null if nothing is selected
     */
    public String getSelectedCharacter() {
        if (selectedPosition >= 0 && selectedPosition < optionViews.size()) {
            TextView selectedView = optionViews.get(selectedPosition);
            CharSequence text = selectedView.getText();
            return text != null ? text.toString() : null;
        }
        return null;
    }
}
