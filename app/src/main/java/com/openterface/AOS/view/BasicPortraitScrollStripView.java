package com.openterface.AOS.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableCompat;

import com.openterface.AOS.R;
import com.openterface.AOS.target.MouseManager;

/**
 * Vertical drag strip for continuous scroll wheel control.
 * Drag up/down to accumulate scroll events based on finger movement distance.
 */
public class BasicPortraitScrollStripView extends View {

    private static final String TAG = "OP-UI";

    // Pixels of finger travel per one wheel unit accumulated
    private static final float STRIP_PIXELS_PER_WHEEL_UNIT = 5f;

    // Chevron sizing constants
    private static final float CHEVRON_MAX_DP = 24f;
    private static final float CHEVRON_EDGE_PAD_DP = 6f;
    private static final float CHEVRON_MAX_WIDTH_FRACTION = 0.85f;
    private static final int CHEVRON_MIN_SHRUNK_PX = 8;

    // Chevron tint pulse constants
    private static final long CHEVRON_PULSE_MS = 100L;
    private static final float PULSE_BLEND_STRONG = 0.55f;
    private static final float FINGER_DOWN_CHEVRON_BLEND = 0.12f;

    // Chevron pulse direction: 0=idle, 1=wheel up, -1=wheel down
    private int chevronPulseDir;
    private final android.os.Handler pulseHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable clearChevronPulse = () -> {
        chevronPulseDir = 0;
        invalidate();
    };

    private boolean fingerDown;

    public interface OnStripScrollListener {
        void onStripScroll(int deltaX, int deltaY);
    }

    private OnStripScrollListener listener;

    private final Paint dividerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    @Nullable private Drawable chevronUp;
    @Nullable private Drawable chevronDown;

    private float lastY;
    private float accumY;

    public BasicPortraitScrollStripView(Context context) {
        super(context);
        init();
    }

    public BasicPortraitScrollStripView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BasicPortraitScrollStripView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setClickable(true);
        setFocusable(true);

        // Setup divider paint
        dividerPaint.setColor(getContext().getResources().getColor(R.color.divider));
        dividerPaint.setStrokeWidth(Math.max(1f, getResources().getDisplayMetrics().density));

        // Load chevron drawables
        Drawable up = AppCompatResources.getDrawable(getContext(), R.drawable.km_basic_scroll_strip_chevron_up);
        if (up != null) {
            chevronUp = DrawableCompat.wrap(up.mutate());
        }
        Drawable down = AppCompatResources.getDrawable(getContext(), R.drawable.km_basic_scroll_strip_chevron_down);
        if (down != null) {
            chevronDown = DrawableCompat.wrap(down.mutate());
        }
    }

    public void setOnStripScrollListener(OnStripScrollListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onDetachedFromWindow() {
        pulseHandler.removeCallbacks(clearChevronPulse);
        super.onDetachedFromWindow();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w != oldw || h != oldh) {
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw divider on the left edge
        float x = dividerPaint.getStrokeWidth() * 0.5f;
        canvas.drawLine(x, 0, x, getHeight(), dividerPaint);

        if (chevronUp == null || chevronDown == null) {
            return;
        }

        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }

        // Determine chevron tint based on pulse state
        int baseChevronTint = getContext().getResources().getColor(R.color.km_touchpad_scroll_strip_chevron);
        int accent = getContext().getResources().getColor(R.color.primary);
        int upTint = baseChevronTint;
        int dnTint = baseChevronTint;

        if (chevronPulseDir > 0) {
            upTint = blendColors(baseChevronTint, accent, PULSE_BLEND_STRONG);
        } else if (chevronPulseDir < 0) {
            dnTint = blendColors(baseChevronTint, accent, PULSE_BLEND_STRONG);
        } else if (fingerDown) {
            upTint = blendColors(upTint, accent, FINGER_DOWN_CHEVRON_BLEND);
            dnTint = blendColors(dnTint, accent, FINGER_DOWN_CHEVRON_BLEND);
        }

        DrawableCompat.setTint(chevronUp, upTint);
        DrawableCompat.setTint(chevronDown, dnTint);

        // Calculate chevron size
        float density = getResources().getDisplayMetrics().density;
        int pad = Math.round(CHEVRON_EDGE_PAD_DP * density);
        int maxSize = Math.round(CHEVRON_MAX_DP * density);
        int size = Math.min(maxSize, Math.round(w * CHEVRON_MAX_WIDTH_FRACTION));
        size = Math.max(1, size);

        int needed = 2 * size + 2 * pad;
        if (h < needed) {
            int shrunk = (h - 2 * pad) / 2;
            if (shrunk < CHEVRON_MIN_SHRUNK_PX) {
                return;
            }
            size = shrunk;
        }

        int left = (w - size) / 2;

        // Draw up chevron
        chevronUp.setBounds(left, pad, left + size, pad + size);
        chevronUp.draw(canvas);

        // Draw down chevron
        int bottomTop = h - pad - size;
        chevronDown.setBounds(left, bottomTop, left + size, h - pad);
        chevronDown.draw(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastY = event.getY();
                accumY = 0f;
                fingerDown = true;
                invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
                float y = event.getY();
                float dy = y - lastY;
                lastY = y;

                // Accumulate scroll based on finger movement
                accumY += (-dy / STRIP_PIXELS_PER_WHEEL_UNIT);
                int sy = (int) accumY;

                if (sy != 0) {
                    accumY -= sy;

                    // Dispatch scroll event
                    if (listener != null) {
                        listener.onStripScroll(0, sy);
                    } else {
                        // Default: send to MouseManager
                        MouseManager.handleTwoFingerPanSlideUpDown((float) sy);
                    }

                    // Trigger haptic feedback and chevron pulse
                    onWheelStepDispatched(sy);
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                lastY = 0f;
                fingerDown = false;
                pulseHandler.removeCallbacks(clearChevronPulse);
                chevronPulseDir = 0;
                invalidate();
                return true;

            default:
                return super.onTouchEvent(event);
        }
    }

    private void onWheelStepDispatched(int sy) {
        // Trigger haptic feedback
        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);

        // Set chevron pulse direction
        chevronPulseDir = sy > 0 ? 1 : -1;
        pulseHandler.removeCallbacks(clearChevronPulse);
        pulseHandler.postDelayed(clearChevronPulse, CHEVRON_PULSE_MS);
        invalidate();
    }

    /**
     * Blend two colors together with a given ratio.
     */
    private int blendColors(int color1, int color2, float ratio) {
        final float inverseRatio = 1f - ratio;
        float r = ((color1 >> 16) & 0xFF) * inverseRatio + ((color2 >> 16) & 0xFF) * ratio;
        float g = ((color1 >> 8) & 0xFF) * inverseRatio + ((color2 >> 8) & 0xFF) * ratio;
        float b = (color1 & 0xFF) * inverseRatio + (color2 & 0xFF) * ratio;
        return ((int) r << 16) | ((int) g << 8) | (int) b;
    }
}
