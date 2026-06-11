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
        popupWindow.setTouchable(true);
        popupWindow.setFocusable(true);
        popupWindow.setOutsideTouchable(true);

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
     * @param listener Callback for character selection
     */
    public void show(View anchor, List<String> alternatives, OnCharacterSelectedListener listener) {
        if (popupWindow == null || anchor == null || alternatives == null || alternatives.isEmpty()) {
            return;
        }

        this.listener = listener;
        container.removeAllViews();
        optionViews = new java.util.ArrayList<>();

        // Create a button for each alternative
        for (int i = 0; i < alternatives.size(); i++) {
            final String character = alternatives.get(i);
            final int position = i;

            TextView optionView = (TextView) LayoutInflater.from(context)
                .inflate(R.layout.item_character_alternate, container, false);
            optionView.setText(character);

            // Handle touch events for selection
            optionView.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // Highlight selected option
                        updateSelection(position);
                        return true;

                    case MotionEvent.ACTION_UP:
                        // Confirm selection
                        if (listener != null && selectedPosition == position) {
                            listener.onCharacterSelected(character);
                        }
                        dismiss();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        // Update selection as finger moves
                        Rect rect = new Rect();
                        v.getGlobalVisibleRect(rect);
                        if (rect.contains((int) event.getRawX(), (int) event.getRawY())) {
                            updateSelection(position);
                        } else {
                            updateSelection(-1);
                        }
                        return true;
                }
                return false;
            });

            container.addView(optionView);
            optionViews.add(optionView);
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

        selectedPosition = -1;

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
}
