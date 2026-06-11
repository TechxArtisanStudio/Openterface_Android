package com.openterface.AOS.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import com.openterface.AOS.R;
import com.openterface.AOS.target.HidManager;

/**
 * Mouse button strip with three keycap-styled buttons: Left, Middle, Right.
 * Includes haptic feedback on button press.
 * Scroll functionality has been moved to BasicPortraitScrollStripView.
 */
public class MouseControlStripView extends LinearLayout {

    private static final String TAG = "MouseControlStripView";

    // Button mask constants (kept compatible with CH9329 protocol)
    public static final int BTN_LEFT = 0x01;
    public static final int BTN_RIGHT = 0x02;
    public static final int BTN_MIDDLE = 0x04;

    private KeycapButton btnLeft;
    private KeycapButton btnMiddle;
    private KeycapButton btnRight;

    // Track which buttons are currently pressed
    private boolean isLeftPressed = false;
    private boolean isRightPressed = false;
    private boolean isMiddlePressed = false;

    private int currentOpacity = 100;

    /**
     * Listener interface for mouse button events.
     * Scroll callbacks are kept for backward compatibility but are no longer
     * triggered from this view (scroll moved to BasicPortraitScrollStripView).
     */
    public interface OnMouseClickListener {
        void onMouseClick(int buttonMask);
        void onMouseRelease();
        /** @deprecated Scroll is now handled by BasicPortraitScrollStripView */
        void onScrollClick();
        /** @deprecated Scroll is now handled by BasicPortraitScrollStripView */
        void onScrollUp();
        /** @deprecated Scroll is now handled by BasicPortraitScrollStripView */
        void onScrollDown();
    }

    private OnMouseClickListener mouseClickListener;

    public MouseControlStripView(Context context) {
        super(context);
        init(context);
    }

    public MouseControlStripView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MouseControlStripView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setOrientation(HORIZONTAL);
        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        createButtonLayout(context);
    }

    private void createButtonLayout(Context context) {
        removeAllViews();

        int buttonHeight = dpToPx(context, 52);
        int gap = dpToPx(context, 6);

        // Left button (weight 2)
        btnLeft = new KeycapButton(context, "L", BTN_LEFT, buttonHeight);
        LayoutParams leftLp = new LayoutParams(0, buttonHeight, 2f);
        addView(btnLeft, leftLp);

        // Middle button (weight 1)
        btnMiddle = new KeycapButton(context, "M", BTN_MIDDLE, buttonHeight);
        LayoutParams middleLp = new LayoutParams(0, buttonHeight, 1f);
        middleLp.setMarginStart(gap);
        addView(btnMiddle, middleLp);

        // Right button (weight 2)
        btnRight = new KeycapButton(context, "R", BTN_RIGHT, buttonHeight);
        LayoutParams rightLp = new LayoutParams(0, buttonHeight, 2f);
        rightLp.setMarginStart(gap);
        addView(btnRight, rightLp);

        applyOpacity();
    }

    /**
     * Get the combined button mask for currently pressed buttons.
     */
    public int getPressedButtonMask() {
        int mask = 0;
        if (isLeftPressed) mask |= BTN_LEFT;
        if (isRightPressed) mask |= BTN_RIGHT;
        if (isMiddlePressed) mask |= BTN_MIDDLE;
        return mask;
    }

    private void sendMouseButtonPress(int buttonMask) {
        if (mouseClickListener != null) {
            mouseClickListener.onMouseClick(buttonMask);
        } else {
            int x = HidManager.getScreenWidth() / 2;
            int y = HidManager.getScreenHeight() / 2;
            String clickType = getClickTypeString(buttonMask);
            HidManager.sendHexAbsButtonClickData(clickType, x, y);
        }
    }

    private void sendMouseRelease() {
        if (mouseClickListener != null) {
            mouseClickListener.onMouseRelease();
        } else {
            HidManager.releaseMSRelData();
        }
    }

    private String getClickTypeString(int mask) {
        switch (mask) {
            case BTN_LEFT:    return "SecLeftData";
            case BTN_MIDDLE:  return "SecMiddleData";
            case BTN_RIGHT:   return "SecRightData";
            default:          return "SecNullData";
        }
    }

    public void setOnMouseClickListener(OnMouseClickListener listener) {
        this.mouseClickListener = listener;
    }

    public void setOpacity(int opacity) {
        currentOpacity = Math.max(0, Math.min(100, opacity));
        applyOpacity();
    }

    public int getOpacity() {
        return currentOpacity;
    }

    private void applyOpacity() {
        float alpha = currentOpacity / 100f;
        setAlpha(alpha);
    }

    private int dpToPx(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    public KeycapButton getBtnLeft() { return btnLeft; }
    public KeycapButton getBtnMiddle() { return btnMiddle; }
    public KeycapButton getBtnRight() { return btnRight; }

    /**
     * Keycap-styled mouse button with haptic feedback.
     */
    public class KeycapButton extends View {
        private final String label;
        private final int buttonMask;
        private final Paint bgPaint;
        private final Paint borderPaint;
        private final Paint textPaint;
        private final RectF bgRect = new RectF();
        private boolean isPressed = false;

        public KeycapButton(Context context, String label, int buttonMask, int height) {
            super(context);
            this.label = label;
            this.buttonMask = buttonMask;

            float density = context.getResources().getDisplayMetrics().density;

            bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            bgPaint.setColor(Color.WHITE);

            borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(density);
            borderPaint.setColor(Color.parseColor("#9A9AA0"));

            textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTextSize(18 * density);
            textPaint.setColor(Color.parseColor("#1F1F24"));
            textPaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);

            setClickable(true);
            setFocusable(true);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            int w = getWidth();
            int h = getHeight();
            float cornerRadius = dpToPx(getContext(), 8);

            bgRect.set(0, 0, w, h);

            if (isPressed) {
                bgPaint.setColor(Color.parseColor("#FFE0B2"));
            } else {
                bgPaint.setColor(Color.WHITE);
            }

            canvas.drawRoundRect(bgRect, cornerRadius, cornerRadius, bgPaint);
            canvas.drawRoundRect(bgRect, cornerRadius, cornerRadius, borderPaint);

            float textX = w / 2f;
            float textY = (h / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f);
            canvas.drawText(label, textX, textY, textPaint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    isPressed = true;
                    invalidate();
                    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                    setButtonPressed(buttonMask, true);
                    sendMouseButtonPress(buttonMask);
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    isPressed = false;
                    invalidate();
                    setButtonPressed(buttonMask, false);
                    sendMouseRelease();
                    return true;
            }
            return super.onTouchEvent(event);
        }
    }

    private void setButtonPressed(int mask, boolean pressed) {
        switch (mask) {
            case BTN_LEFT:    isLeftPressed = pressed; break;
            case BTN_MIDDLE:  isMiddlePressed = pressed; break;
            case BTN_RIGHT:   isRightPressed = pressed; break;
        }
    }
}
